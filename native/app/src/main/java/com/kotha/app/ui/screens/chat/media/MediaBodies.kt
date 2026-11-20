package com.kotha.app.ui.screens.chat.media

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.remember
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import coil.request.ImageRequest
import com.kotha.app.data.chat.MediaBatch
import com.kotha.app.ui.theme.CovaTheme
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.kotha.app.R
import com.kotha.app.data.model.Message
import com.kotha.app.data.model.UploadStage
import com.kotha.app.util.Format
import java.io.File

internal val MediaWidth = 240.dp

private val VisualWidth = 256.dp
private val StackWidth = 252.dp
private val StackCardWidth = 196.dp
private const val StackLimit = 4
private val StackOffsetX = floatArrayOf(0f, 12f, -12f, 5f)
private val StackOffsetY = floatArrayOf(4f, -6f, -4f, -10f)
private val StackSide = floatArrayOf(-1f, 1f, -1f, 1f)

private fun ratioOf(message: Message): Float =
    if (message.width > 0 && message.height > 0) {
        (message.width.toFloat() / message.height).coerceIn(0.66f, 1.6f)
    } else {
        4f / 3f
    }

private fun showsOverlay(message: Message): Boolean {
    val upload = message.upload ?: return false
    return message.url.isEmpty() || upload.stage == UploadStage.Failed
}

private fun sourceOf(message: Message): Any? {
    val upload = message.upload
    return when {
        message.type == "video" -> upload?.thumbPath?.takeIf { it.isNotEmpty() }?.let { File(it) }
            ?: message.thumb.takeIf { it.isNotEmpty() }
        upload != null && upload.localPath.isNotEmpty() -> File(upload.localPath)
        else -> message.url.takeIf { it.isNotEmpty() }
    }
}

private fun tilt(seed: Int, depth: Int): Float {
    val spread = ((seed ushr (depth * 4)) and 15) / 15f
    val magnitude = if (depth == 0) 1.5f + spread * 2f else 4f + spread * 4.5f
    return magnitude * StackSide[depth]
}

@Composable
fun GlassPill(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(11.dp))
            .background(Color.Black.copy(alpha = 0.42f))
            .padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        content()
    }
}

@Composable
private fun MediaImage(source: Any?, video: Boolean, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val request = remember(source) {
        source?.let { ImageRequest.Builder(context).data(it).crossfade(280).build() }
    }
    if (request != null) {
        AsyncImage(
            model = request,
            contentDescription = stringResource(if (video) R.string.common_video else R.string.common_photo),
            contentScale = ContentScale.Crop,
            modifier = modifier
        )
    } else if (video) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Icon(
                imageVector = Icons.Filled.Videocam,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun PlayBadge(size: Dp, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.45f))
            .border(1.5.dp, Color.White.copy(alpha = 0.7f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Filled.PlayArrow,
            contentDescription = stringResource(R.string.viewer_play),
            modifier = Modifier.size(size * 0.62f),
            tint = Color.White
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun VisualBody(
    message: Message,
    onTap: () -> Unit,
    onLongPress: () -> Unit,
    callbacks: MediaCallbacks,
    meta: @Composable () -> Unit
) {
    val upload = message.upload
    val video = message.type == "video"
    Box(
        modifier = Modifier
            .width(VisualWidth)
            .aspectRatio(ratioOf(message))
            .combinedClickable(onClick = onTap, onLongClick = onLongPress)
    ) {
        MediaImage(source = sourceOf(message), video = video, modifier = Modifier.fillMaxSize())
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(64.dp)
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.4f))))
        )
        if (video && !showsOverlay(message)) {
            PlayBadge(size = 52.dp, modifier = Modifier.align(Alignment.Center))
        }
        if (video && message.duration > 0) {
            GlassPill(modifier = Modifier.align(Alignment.BottomStart).padding(8.dp)) {
                Text(
                    text = Format.duration(message.duration),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White
                )
            }
        }
        GlassPill(modifier = Modifier.align(Alignment.BottomEnd).padding(8.dp)) { meta() }
        if (upload != null && showsOverlay(message)) {
            UploadOverlay(
                upload = upload,
                onCancel = { callbacks.onCancel(message.id) },
                onRetry = { callbacks.onRetry(message.id) }
            )
        }
    }
}

@Composable
fun StackBody(
    messages: List<Message>,
    mine: Boolean,
    selected: Boolean,
    onTapCard: (Message) -> Unit,
    onLongPress: () -> Unit,
    callbacks: MediaCallbacks,
    meta: @Composable () -> Unit
) {
    val colors = CovaTheme.colors
    val shown = messages.take(StackLimit)
    val cover = shown.first()
    val seedSource = MediaBatch.keyOf(cover.id) ?: cover.id
    val seed = remember(seedSource) { seedSource.hashCode() }
    val mirror = if ((seed and 1) == 0) 1f else -1f
    val ratio = ratioOf(cover).coerceIn(0.8f, 1.35f)
    val cardHeight = StackCardWidth / ratio
    val rim = if (selected) MaterialTheme.colorScheme.primary else if (mine) colors.bubbleMine else colors.bubbleTheirs
    val extra = messages.size - shown.size
    Box(modifier = Modifier.width(StackWidth).height(cardHeight + 52.dp)) {
        for (depth in shown.indices.reversed()) {
            StackCard(
                message = shown[depth],
                front = depth == 0,
                offsetX = StackOffsetX[depth] * mirror,
                offsetY = StackOffsetY[depth],
                angle = tilt(seed, depth) * mirror,
                width = StackCardWidth,
                height = cardHeight,
                rim = rim,
                extra = if (depth == 0) extra else 0,
                onTap = onTapCard,
                onLongPress = onLongPress,
                callbacks = callbacks,
                meta = meta
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun BoxScope.StackCard(
    message: Message,
    front: Boolean,
    offsetX: Float,
    offsetY: Float,
    angle: Float,
    width: Dp,
    height: Dp,
    rim: Color,
    extra: Int,
    onTap: (Message) -> Unit,
    onLongPress: () -> Unit,
    callbacks: MediaCallbacks,
    meta: @Composable () -> Unit
) {
    val outer = RoundedCornerShape(14.dp)
    val inner = RoundedCornerShape(11.5.dp)
    val video = message.type == "video"
    val upload = message.upload
    Box(
        modifier = Modifier
            .align(Alignment.Center)
            .offset(x = offsetX.dp, y = offsetY.dp)
            .size(width, height)
            .graphicsLayer { rotationZ = angle }
            .shadow(if (front) 10.dp else 4.dp, outer)
            .clip(outer)
            .background(rim)
            .padding(2.5.dp)
            .clip(inner)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .combinedClickable(onClick = { onTap(message) }, onLongClick = onLongPress)
    ) {
        MediaImage(source = sourceOf(message), video = video, modifier = Modifier.fillMaxSize())
        if (video && !showsOverlay(message)) {
            PlayBadge(size = if (front) 46.dp else 38.dp, modifier = Modifier.align(Alignment.Center))
        }
        if (front) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(52.dp)
                    .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.4f))))
            )
            GlassPill(modifier = Modifier.align(Alignment.BottomEnd).padding(6.dp)) { meta() }
            if (extra > 0) {
                GlassPill(modifier = Modifier.align(Alignment.BottomStart).padding(6.dp)) {
                    Text(
                        text = "+" + Format.number(extra),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
        if (upload != null && showsOverlay(message)) {
            UploadOverlay(
                upload = upload,
                onCancel = { callbacks.onCancel(message.id) },
                onRetry = { callbacks.onRetry(message.id) }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FileBody(
    message: Message,
    content: Color,
    onTap: () -> Unit,
    onLongPress: () -> Unit,
    callbacks: MediaCallbacks
) {
    val upload = message.upload
    Row(
        modifier = Modifier
            .width(MediaWidth)
            .clip(RoundedCornerShape(10.dp))
            .combinedClickable(onClick = onTap, onLongClick = onLongPress)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (upload != null && showsOverlay(message)) {
            UploadButton(
                upload = upload,
                onCancel = { callbacks.onCancel(message.id) },
                onRetry = { callbacks.onRetry(message.id) },
                tint = content,
                background = content.copy(alpha = 0.12f),
                size = 44.dp
            )
        } else {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(content.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.InsertDriveFile,
                    contentDescription = null,
                    tint = content
                )
            }
        }
        Spacer(Modifier.width(10.dp))
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = message.name.ifEmpty { stringResource(R.string.common_file) },
                style = MaterialTheme.typography.bodyMedium,
                color = content,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            if (message.size > 0) {
                Text(
                    text = Format.fileSize(message.size),
                    style = MaterialTheme.typography.labelSmall,
                    color = content.copy(alpha = 0.7f)
                )
            }
        }
    }
}
