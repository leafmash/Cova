package com.kotha.app.data.media

import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import android.media.MediaRecorder
import android.media.audiofx.AcousticEchoCanceler
import android.media.audiofx.AudioEffect
import android.media.audiofx.AutomaticGainControl
import android.media.audiofx.NoiseSuppressor
import android.os.SystemClock
import com.kotha.app.core.AppConfig
import com.kotha.app.core.ApplicationScope
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.nio.ByteOrder
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sqrt
import kotlin.math.tanh
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class RecordingState(val elapsedMs: Long, val levels: List<Float>)

class RecordedVoice(val file: File, val durationMs: Long, val wave: List<Int>)

@Singleton
class VoiceRecorder @Inject constructor(
    @ApplicationContext private val context: Context,
    @ApplicationScope private val scope: CoroutineScope
) {

    private var session: Session? = null
    private val samples = ArrayList<Int>()
    private val mutableState = MutableStateFlow<RecordingState?>(null)

    val state: StateFlow<RecordingState?> = mutableState.asStateFlow()

    fun start(): Boolean {
        if (session != null) return false
        val target = File(context.cacheDir, "voice-${System.currentTimeMillis()}.m4a")
        val created = Session(target)
        if (!created.begin()) {
            target.delete()
            return false
        }
        synchronized(samples) { samples.clear() }
        mutableState.value = RecordingState(0L, emptyList())
        session = created
        return true
    }

    fun stop(): RecordedVoice? {
        val active = session ?: return null
        session = null
        val duration = SystemClock.elapsedRealtime() - active.startedAt
        val ok = active.finish(cancel = false)
        mutableState.value = null
        if (!ok || duration < AppConfig.VOICE_MIN_MS || !active.target.exists() || active.target.length() == 0L) {
            active.target.delete()
            return null
        }
        return RecordedVoice(active.target, duration, buildWave())
    }

    fun cancel() {
        val active = session ?: return
        session = null
        active.finish(cancel = true)
        mutableState.value = null
        active.target.delete()
    }

    private fun buildWave(): List<Int> {
        val copy = synchronized(samples) { samples.toList() }
        if (copy.isEmpty()) return List(AppConfig.WAVE_POINTS) { 0 }
        val size = copy.size.toDouble() / AppConfig.WAVE_POINTS
        val peaks = List(AppConfig.WAVE_POINTS) { index ->
            val from = (index * size).toInt()
            val to = max(from + 1, ((index + 1) * size).toInt())
            var peak = 0
            var cursor = from
            while (cursor < to && cursor < copy.size) {
                peak = max(peak, copy[cursor])
                cursor++
            }
            peak.toDouble()
        }
        val top = max(peaks.maxOrNull() ?: 0.0, 1.0)
        return peaks.map { ((it / top).coerceIn(0.0, 1.0).pow(0.7) * 100).roundToInt() }
    }

    private inner class Session(val target: File) {

        @Volatile
        private var stopping = false

        @Volatile
        private var cancelled = false

        @Volatile
        private var failed = false

        var startedAt = 0L
            private set

        private var record: AudioRecord? = null
        private var codec: MediaCodec? = null
        private var muxer: MediaMuxer? = null
        private var worker: Thread? = null
        private var track = -1
        private var muxing = false
        private val effects = ArrayList<AudioEffect>()
        private val info = MediaCodec.BufferInfo()

        fun begin(): Boolean {
            val minBuffer = AudioRecord.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
            if (minBuffer <= 0) return false
            val bufferSize = max(minBuffer * 2, FRAME_SAMPLES * 2 * 6)
            val input = openRecord(bufferSize) ?: return false
            record = input
            attachEffects(input.audioSessionId)
            return try {
                val format = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC, SAMPLE_RATE, 1)
                format.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
                format.setInteger(MediaFormat.KEY_BIT_RATE, BIT_RATE)
                format.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, FRAME_SAMPLES * 2 * 4)
                val encoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC)
                encoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
                encoder.start()
                codec = encoder
                muxer = MediaMuxer(target.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
                input.startRecording()
                if (input.recordingState != AudioRecord.RECORDSTATE_RECORDING) {
                    release()
                    return false
                }
                startedAt = SystemClock.elapsedRealtime()
                val thread = Thread({ capture() }, "voice-recorder")
                worker = thread
                thread.start()
                true
            } catch (e: Exception) {
                release()
                false
            }
        }

        fun finish(cancel: Boolean): Boolean {
            cancelled = cancel
            stopping = true
            val thread = worker
            if (thread != null) {
                try {
                    thread.join(4_000L)
                } catch (e: InterruptedException) {
                    Thread.currentThread().interrupt()
                }
            }
            return !failed && !cancelled
        }

        private fun openRecord(bufferSize: Int): AudioRecord? {
            val sources = intArrayOf(MediaRecorder.AudioSource.VOICE_COMMUNICATION, MediaRecorder.AudioSource.MIC)
            for (source in sources) {
                val candidate = try {
                    AudioRecord(source, SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, bufferSize)
                } catch (e: Exception) {
                    null
                }
                if (candidate != null && candidate.state == AudioRecord.STATE_INITIALIZED) return candidate
                candidate?.release()
            }
            return null
        }

        private fun attachEffects(sessionId: Int) {
            runCatching {
                if (NoiseSuppressor.isAvailable()) {
                    NoiseSuppressor.create(sessionId)?.let {
                        it.setEnabled(true)
                        effects.add(it)
                    }
                }
            }
            runCatching {
                if (AcousticEchoCanceler.isAvailable()) {
                    AcousticEchoCanceler.create(sessionId)?.let {
                        it.setEnabled(true)
                        effects.add(it)
                    }
                }
            }
            runCatching {
                if (AutomaticGainControl.isAvailable()) {
                    AutomaticGainControl.create(sessionId)?.let {
                        it.setEnabled(true)
                        effects.add(it)
                    }
                }
            }
        }

        private fun capture() {
            val input = record ?: return
            val encoder = codec ?: return
            val pcm = ShortArray(FRAME_SAMPLES)
            val dsp = VoiceDsp()
            val window = ArrayList<Float>()
            var total = 0L
            var peak = 0
            var lastTick = SystemClock.elapsedRealtime()
            try {
                while (true) {
                    val last = stopping
                    if (!last) {
                        val read = input.read(pcm, 0, pcm.size)
                        if (read > 0) {
                            dsp.process(pcm, read)
                            for (i in 0 until read) peak = max(peak, abs(pcm[i].toInt()))
                            if (!feed(encoder, pcm, read, total)) {
                                failed = true
                                break
                            }
                            total += read
                        } else if (read < 0) {
                            failed = true
                            break
                        }
                    }
                    drain(encoder, false)
                    val now = SystemClock.elapsedRealtime()
                    if (now - lastTick >= TICK_MS) {
                        lastTick = now
                        synchronized(samples) { samples.add(peak) }
                        val level = (peak / MAX_AMPLITUDE).toDouble().pow(0.5).toFloat().coerceIn(0f, 1f)
                        peak = 0
                        window.add(level)
                        if (window.size > WINDOW) window.removeAt(0)
                        if (!stopping) mutableState.value = RecordingState(now - startedAt, window.toList())
                    }
                    if (last) break
                }
                if (!failed) {
                    endStream(encoder, total)
                    drain(encoder, true)
                }
            } catch (e: Exception) {
                failed = true
            } finally {
                release()
                if (failed || cancelled) target.delete()
            }
        }

        private fun feed(encoder: MediaCodec, pcm: ShortArray, count: Int, total: Long): Boolean {
            var attempts = 0
            while (attempts < 60) {
                val index = encoder.dequeueInputBuffer(10_000L)
                if (index >= 0) {
                    val buffer = encoder.getInputBuffer(index) ?: return false
                    buffer.clear()
                    buffer.order(ByteOrder.LITTLE_ENDIAN)
                    buffer.asShortBuffer().put(pcm, 0, count)
                    val time = total * 1_000_000L / SAMPLE_RATE
                    encoder.queueInputBuffer(index, 0, count * 2, time, 0)
                    return true
                }
                drain(encoder, false)
                attempts++
            }
            return false
        }

        private fun endStream(encoder: MediaCodec, total: Long) {
            var attempts = 0
            while (attempts < 60) {
                val index = encoder.dequeueInputBuffer(10_000L)
                if (index >= 0) {
                    encoder.queueInputBuffer(index, 0, 0, total * 1_000_000L / SAMPLE_RATE, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                    return
                }
                drain(encoder, false)
                attempts++
            }
        }

        private fun drain(encoder: MediaCodec, finalPass: Boolean) {
            var waits = 0
            while (true) {
                val index = encoder.dequeueOutputBuffer(info, if (finalPass) 10_000L else 0L)
                if (index == MediaCodec.INFO_TRY_AGAIN_LATER) {
                    if (!finalPass) return
                    waits++
                    if (waits > 200) return
                    continue
                }
                if (index == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    val mux = muxer ?: return
                    if (!muxing) {
                        track = mux.addTrack(encoder.outputFormat)
                        mux.start()
                        muxing = true
                    }
                    continue
                }
                if (index < 0) continue
                val data = encoder.getOutputBuffer(index)
                if (info.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0) info.size = 0
                val mux = muxer
                if (info.size > 0 && muxing && data != null && mux != null) {
                    data.position(info.offset)
                    data.limit(info.offset + info.size)
                    mux.writeSampleData(track, data, info)
                }
                encoder.releaseOutputBuffer(index, false)
                if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) return
            }
        }

        private fun release() {
            effects.forEach { runCatching { it.release() } }
            effects.clear()
            record?.let {
                runCatching { it.stop() }
                runCatching { it.release() }
            }
            record = null
            codec?.let {
                runCatching { it.stop() }
                runCatching { it.release() }
            }
            codec = null
            muxer?.let {
                if (muxing) runCatching { it.stop() }
                runCatching { it.release() }
            }
            muxer = null
            muxing = false
        }
    }

    private class VoiceDsp {
        private var previousIn = 0f
        private var previousOut = 0f
        private var floor = 0.01f
        private var gate = 0.3f
        private var agc = 1.4f
        private var lastGain = 0.3f
        private val scratch = FloatArray(SLICE)

        fun process(buffer: ShortArray, count: Int) {
            var start = 0
            while (start < count) {
                val end = min(count, start + SLICE)
                slice(buffer, start, end)
                start = end
            }
        }

        private fun slice(buffer: ShortArray, from: Int, to: Int) {
            val length = to - from
            var energy = 0.0
            for (i in 0 until length) {
                val x = buffer[from + i] / 32768f
                val y = HIGH_PASS * (previousOut + x - previousIn)
                previousIn = x
                previousOut = y
                scratch[i] = y
                energy += (y * y).toDouble()
            }
            val rms = sqrt(energy / length).toFloat()
            floor = if (rms < floor) {
                floor * 0.85f + rms * 0.15f
            } else {
                floor + (rms - floor) * 0.0012f
            }
            floor = floor.coerceIn(0.0006f, 0.045f)
            val ratio = rms / (floor * 2.6f)
            val targetGate = if (ratio >= 1f) 1f else max(0.1f, ratio * ratio)
            gate += if (targetGate > gate) (targetGate - gate) * 0.55f else (targetGate - gate) * 0.05f
            if (rms * gate > 0.015f) {
                val wanted = (0.16f / rms).coerceIn(0.9f, 3.2f)
                agc += (wanted - agc) * 0.012f
            }
            val gain = gate * agc
            for (i in 0 until length) {
                val applied = lastGain + (gain - lastGain) * (i + 1) / length
                val shaped = tanh(scratch[i] * applied)
                buffer[from + i] = (shaped * 32767f).toInt().coerceIn(-32768, 32767).toShort()
            }
            lastGain = gain
        }

        private companion object {
            const val SLICE = 256
            const val HIGH_PASS = 0.986f
        }
    }

    private companion object {
        const val SAMPLE_RATE = 44_100
        const val BIT_RATE = 96_000
        const val FRAME_SAMPLES = 1_024
        const val TICK_MS = 60L
        const val WINDOW = 30
        const val MAX_AMPLITUDE = 32_767f
    }
}
