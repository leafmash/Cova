package com.kotha.app.ui.screens.chat

import com.kotha.app.core.AppConfig
import com.kotha.app.data.chat.MediaBatch
import com.kotha.app.data.model.Message
import com.kotha.app.util.Format

sealed interface ChatItem {
    val key: String

    data class Day(val ms: Long) : ChatItem {
        override val key: String = "day-${Format.dayStart(ms)}"
    }

    data class Bubble(
        val message: Message,
        val mine: Boolean,
        val first: Boolean,
        val quoteHidden: Boolean,
        val batch: String? = null,
        val stack: List<Message> = emptyList()
    ) : ChatItem {
        override val key: String = if (batch != null) "stack-$batch" else message.id

        val members: List<Message> get() = stack.ifEmpty { listOf(message) }
    }

    data class System(val message: Message) : ChatItem {
        override val key: String = message.id
    }

    data class Call(val message: Message, val mine: Boolean) : ChatItem {
        override val key: String = message.id
    }

    data class HiddenNote(val anchorId: String) : ChatItem {
        override val key: String = "hidden-$anchorId"
    }
}

fun buildChatItems(
    messages: List<Message>,
    uid: String,
    group: Boolean,
    blocked: Set<String>,
    clearedAt: Long,
    goneQuotes: Map<String, Boolean> = emptyMap()
): List<ChatItem> {
    val byId = messages.associateBy { it.id }
    val visible = messages
        .filter { !it.hiddenFor.contains(uid) && !(clearedAt > 0 && it.atMs > 0 && it.atMs <= clearedAt) }
        .asReversed()
    val out = ArrayList<ChatItem>(visible.size + 8)
    var lastDay = -1L
    var previousFrom: String? = null
    var hiddenRun = false
    for (message in visible) {
        val mine = message.from == uid
        if (message.atMs > 0) {
            val day = Format.dayStart(message.atMs)
            if (day != lastDay) {
                out.add(ChatItem.Day(message.atMs))
                lastDay = day
                previousFrom = null
            }
        }
        when {
            message.type == "system" && message.sys != null && !message.deleted -> {
                out.add(ChatItem.System(message))
                previousFrom = null
            }
            group && !mine && blocked.contains(message.from) -> {
                if (!hiddenRun) out.add(ChatItem.HiddenNote(message.id))
                hiddenRun = true
                previousFrom = null
            }
            message.callLog != null && !message.deleted -> {
                hiddenRun = false
                out.add(ChatItem.Call(message, mine))
                previousFrom = null
            }
            else -> {
                hiddenRun = false
                val quoteHidden = message.replyId?.takeIf { !it.startsWith(AppConfig.NOTE_REPLY_PREFIX) }?.let { id ->
                    byId[id]?.deleted ?: (goneQuotes[id] == true)
                } == true
                val batch = if (!message.deleted && (message.type == "image" || message.type == "video")) {
                    MediaBatch.keyOf(message.id)
                } else {
                    null
                }
                val last = out.lastOrNull() as? ChatItem.Bubble
                if (batch != null && last != null && last.batch == batch && last.message.from == message.from) {
                    val merged = (last.members + message).sortedBy { MediaBatch.indexOf(it.id) }
                    out[out.lastIndex] = last.copy(message = merged.first(), stack = merged)
                } else {
                    out.add(ChatItem.Bubble(message, mine, previousFrom != message.from, quoteHidden, batch))
                }
                previousFrom = message.from
            }
        }
    }
    return out.reversed()
}

val ChatItem.bubbleMessages: List<Message>
    get() = when (this) {
        is ChatItem.Bubble -> members
        else -> emptyList()
    }
