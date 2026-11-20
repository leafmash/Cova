package com.kotha.app.ui.screens.chat

import com.kotha.app.core.AppConfig
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kotha.app.R
import com.kotha.app.data.model.Message
import com.kotha.app.ui.components.avatarColor
import com.kotha.app.ui.screens.chat.media.FileBody
import com.kotha.app.ui.screens.chat.media.MediaCallbacks
import com.kotha.app.ui.screens.chat.media.StackBody
import com.kotha.app.ui.screens.chat.media.VisualBody
import com.kotha.app.ui.screens.chat.media.VoiceBody
import com.kotha.app.ui.theme.CovaMotion
import com.kotha.app.ui.theme.CovaTheme
import com.kotha.app.ui.theme.bubbleBrush
import com.kotha.app.util.Format
import com.kotha.app.util.MessageText
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MessageBubble(
    item: ChatItem.Bubble,
    group: Boolean,
    senderName: String,
    highlighted: Boolean,
    selecting: Boolean,
    picker: Boolean,
    myReaction: String?,
    onReact: (String) -> Unit,
    media: MediaCallbacks,
    onClick: () -> Unit,
    onLongPress: () -> Unit,
    onReply: () -> Unit,
    onQuoteClick: (String) -> Unit,
    onSenderClick: () -> Unit
) {
    val message = item.message
    val mine = item.mine
    val colors = CovaTheme.colors
    val container = if (mine) Color.Transparent else colors.bubbleTheirs
    val bubbleBackground = bubbleBrush()
    val actionsLabel = stringResource(R.string.a11y_message_actions)
    val content = if (mine) colors.onBubbleMine else colors.onBubbleTheirs
    val density = LocalDensity.current
    val threshold = with(density) { 56.dp.toPx() }
    val maxDrag = with(density) { 84.dp.toPx() }
    val offset = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current
    var armed by remember { mutableStateOf(false) }
    val direction = if (mine) -1f else 1f
    val tap: () -> Unit = { if (selecting) onClick() else media.onOpen(message.id) }
    val longPress: () -> Unit = {
        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        onLongPress()
    }
    val shape = RoundedCornerShape(
        topStart = 22.dp,
        topEnd = 22.dp,
        bottomStart = if (mine) 22.dp else 6.dp,
        bottomEnd = if (mine) 6.dp else 22.dp
    )
    val accent = if (mine) Color.White.copy(alpha = 0.85f) else MaterialTheme.colorScheme.primary
    val visual = !message.deleted && (message.type == "image" || message.type == "video")
    val stacked = item.stack.size > 1
    val inset = if (visual) 12.dp else 0.dp
    val rimBrush = remember(colors.brandStart, colors.aurora, mine) {
        val alpha = if (mine) 0.95f else 0.6f
        Brush.linearGradient(listOf(colors.brandStart.copy(alpha = alpha), colors.aurora.copy(alpha = alpha)))
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = if (item.first) 8.dp else 2.dp)
            .pointerInput(message.id, message.deleted, selecting) {
                if (message.deleted || selecting) return@pointerInput
                detectHorizontalDragGestures(
                    onDragEnd = {
                        if (offset.value >= threshold) onReply()
                        armed = false
                        scope.launch { offset.animateTo(0f, spring()) }
                    },
                    onDragCancel = {
                        armed = false
                        scope.launch { offset.animateTo(0f, spring()) }
                    },
                    onHorizontalDrag = { change, amount ->
                        change.consume()
                        val next = (offset.value + amount * direction).coerceIn(0f, maxDrag)
                        scope.launch { offset.snapTo(next) }
                        val nowArmed = next >= threshold
                        if (nowArmed != armed) {
                            armed = nowArmed
                            if (nowArmed) haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                    }
                )
            }
    ) {
        Box(
            modifier = Modifier
                .align(if (mine) Alignment.CenterEnd else Alignment.CenterStart)
                .padding(horizontal = 10.dp)
                .alpha((offset.value / threshold).coerceIn(0f, 1f))
                .size(34.dp)
                .background(CovaTheme.colors.accentSoft, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Reply,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset((offset.value * direction).roundToInt(), 0) },
            contentAlignment = if (mine) Alignment.CenterEnd else Alignment.CenterStart
        ) {
            val hasReactions = message.reactions.isNotEmpty() && !message.deleted
            Box(modifier = Modifier.padding(bottom = if (hasReactions) 18.dp else 0.dp)) {
                if (stacked) {
                    StackBubble(
                        item = item,
                        group = group,
                        senderName = senderName,
                        highlighted = highlighted,
                        selecting = selecting,
                        media = media,
                        onClick = onClick,
                        onLongPress = longPress,
                        onQuoteClick = onQuoteClick,
                        onSenderClick = onSenderClick
                    )
                } else {
                    Surface(
                        shape = shape,
                        color = container,
                        contentColor = content,
                        border = when {
                            highlighted -> BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                            visual -> BorderStroke(1.5.dp, rimBrush)
                            !mine -> BorderStroke(1.dp, colors.glassBorder)
                            else -> BorderStroke(1.dp, Color.White.copy(alpha = 0.14f))
                        },
                        modifier = Modifier
                            .widthIn(max = 312.dp)
                            .then(if (mine) Modifier.background(bubbleBackground, shape) else Modifier)
                            .semantics(mergeDescendants = true) {}
                            .combinedClickable(
                                onClick = onClick,
                                onLongClickLabel = actionsLabel,
                                onLongClick = {
                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onLongPress()
                                }
                            )
                    ) {
                        Column(
                            modifier = if (visual) {
                                Modifier
                            } else {
                                Modifier.padding(start = 15.dp, end = 15.dp, top = 10.dp, bottom = 8.dp)
                            }
                        ) {
                            if (group && !mine) {
                                SenderLabel(
                                    name = senderName,
                                    from = message.from,
                                    selecting = selecting,
                                    onClick = onClick,
                                    onSenderClick = onSenderClick,
                                    onLongPress = longPress,
                                    modifier = Modifier.padding(
                                        start = inset,
                                        end = inset,
                                        top = if (visual) 8.dp else 0.dp,
                                        bottom = 2.dp
                                    )
                                )
                            }
                            if (message.forwarded && !message.deleted) {
                                Text(
                                    text = "↪ " + stringResource(R.string.msg_forwarded),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontStyle = FontStyle.Italic,
                                    color = content.copy(alpha = 0.7f),
                                    modifier = Modifier.padding(
                                        start = inset,
                                        end = inset,
                                        top = if (visual) 8.dp else 0.dp
                                    )
                                )
                            }
                            if (message.replyText != null && !message.deleted && !item.quoteHidden) {
                                val noteReply = message.replyId?.startsWith(AppConfig.NOTE_REPLY_PREFIX) == true
                                Box(
                                    modifier = Modifier.padding(
                                        start = inset,
                                        end = inset,
                                        top = if (visual) 6.dp else 0.dp
                                    )
                                ) {
                                    QuoteBlock(
                                        text = message.replyText,
                                        label = if (noteReply) stringResource(if (mine) R.string.note_reply_mine else R.string.note_reply_theirs) else null,
                                        content = content,
                                        accent = accent,
                                        onClick = { if (!noteReply) message.replyId?.let(onQuoteClick) }
                                    )
                                }
                            }
                            BubbleBody(
                                message = message,
                                content = content,
                                mine = mine,
                                selecting = selecting,
                                media = media,
                                onTap = tap,
                                onLongPress = longPress,
                                meta = { MetaRow(message = message, mine = mine, content = Color.White) }
                            )
                            if (!visual) {
                                MetaRow(
                                    message = message,
                                    mine = mine,
                                    content = content,
                                    modifier = Modifier.align(Alignment.End).padding(top = 3.dp)
                                )
                            }
                        }
                    }
                }
                if (hasReactions) {
                    ReactionBadge(
                        message = message,
                        modifier = Modifier
                            .align(if (mine) Alignment.BottomStart else Alignment.BottomEnd)
                            .offset(x = if (mine) 10.dp else (-10).dp)
                    )
                }
                if (picker) {
                    ReactionPicker(current = myReaction, mine = mine, onReact = onReact)
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SenderLabel(
    name: String,
    from: String,
    selecting: Boolean,
    onClick: () -> Unit,
    onSenderClick: () -> Unit,
    onLongPress: () -> Unit,
    modifier: Modifier = Modifier
) {
    Text(
        text = name.ifEmpty { stringResource(R.string.common_user) },
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Bold,
        color = avatarColor(from),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier.combinedClickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClick = { if (selecting) onClick() else onSenderClick() },
            onLongClick = onLongPress
        )
    )
}

private fun summarize(stack: List<Message>): Message {
    val rank = stack.minOf {
        when (it.status) {
            "seen" -> 2
            "delivered" -> 1
            else -> 0
        }
    }
    return stack.last().copy(
        pending = stack.any { it.pending },
        status = when (rank) {
            2 -> "seen"
            1 -> "delivered"
            else -> "sent"
        }
    )
}

@Composable
private fun StackBubble(
    item: ChatItem.Bubble,
    group: Boolean,
    senderName: String,
    highlighted: Boolean,
    selecting: Boolean,
    media: MediaCallbacks,
    onClick: () -> Unit,
    onLongPress: () -> Unit,
    onQuoteClick: (String) -> Unit,
    onSenderClick: () -> Unit
) {
    val message = item.message
    val mine = item.mine
    val colors = CovaTheme.colors
    val content = if (mine) colors.onBubbleMine else colors.onBubbleTheirs
    val accent = if (mine) Color.White.copy(alpha = 0.85f) else MaterialTheme.colorScheme.primary
    val summary = remember(item.stack) { summarize(item.stack) }
    Column(
        modifier = Modifier.widthIn(max = 312.dp),
        horizontalAlignment = if (mine) Alignment.End else Alignment.Start
    ) {
        if (group && !mine) {
            SenderLabel(
                name = senderName,
                from = message.from,
                selecting = selecting,
                onClick = onClick,
                onSenderClick = onSenderClick,
                onLongPress = onLongPress,
                modifier = Modifier.padding(start = 8.dp, bottom = 2.dp)
            )
        }
        if (message.replyText != null && !item.quoteHidden) {
            val noteReply = message.replyId?.startsWith(AppConfig.NOTE_REPLY_PREFIX) == true
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = if (mine) colors.bubbleMine else colors.bubbleTheirs,
                contentColor = content,
                modifier = Modifier.padding(horizontal = 6.dp)
            ) {
                Box(modifier = Modifier.padding(horizontal = 8.dp)) {
                    QuoteBlock(
                        text = message.replyText,
                        label = if (noteReply) stringResource(if (mine) R.string.note_reply_mine else R.string.note_reply_theirs) else null,
                        content = content,
                        accent = accent,
                        onClick = { if (!noteReply) message.replyId?.let(onQuoteClick) }
                    )
                }
            }
        }
        StackBody(
            messages = item.stack,
            mine = mine,
            selected = highlighted,
            onTapCard = { card -> if (selecting) onClick() else media.onOpen(card.id) },
            onLongPress = onLongPress,
            callbacks = media,
            meta = { MetaRow(message = summary, mine = mine, content = Color.White) }
        )
    }
}

@Composable
private fun QuoteBlock(text: String, label: String?, content: Color, accent: Color, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = content.copy(alpha = 0.12f),
        modifier = Modifier
            .padding(top = 2.dp, bottom = 6.dp)
            .clickable(onClick = onClick)
    ) {
        Row(modifier = Modifier.height(IntrinsicSize.Min)) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(accent)
            )
            Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)) {
                if (label != null) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = accent
                    )
                }
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodySmall,
                    color = content.copy(alpha = 0.9f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun BubbleBody(
    message: Message,
    content: Color,
    mine: Boolean,
    selecting: Boolean,
    media: MediaCallbacks,
    onTap: () -> Unit,
    onLongPress: () -> Unit,
    meta: @Composable () -> Unit
) {
    when {
        message.deleted -> Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Filled.Block,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = content.copy(alpha = 0.7f)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = stringResource(R.string.chat_deleted),
                style = MaterialTheme.typography.bodyMedium,
                fontStyle = FontStyle.Italic,
                color = content.copy(alpha = 0.7f)
            )
        }
        message.type == "image" || message.type == "video" ->
            VisualBody(message = message, onTap = onTap, onLongPress = onLongPress, callbacks = media, meta = meta)
        message.type == "audio" -> VoiceBody(
            message = message,
            content = content,
            selecting = selecting,
            onTap = onTap,
            onLongPress = onLongPress,
            callbacks = media
        )
        message.type == "file" ->
            FileBody(message = message, content = content, onTap = onTap, onLongPress = onLongPress, callbacks = media)
        else -> Text(
            text = MessageText.linkify(
                message.text,
                if (mine) content else MaterialTheme.colorScheme.primary
            ),
            style = MaterialTheme.typography.bodyLarge,
            color = content
        )
    }
}

@Composable
private fun ReactionBadge(message: Message, modifier: Modifier = Modifier) {
    val picks = message.reactions.values
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        shadowElevation = 2.dp,
        border = BorderStroke(2.dp, CovaTheme.colors.chatBackground),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = picks.toSet().take(3).joinToString(""),
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp, lineHeight = 17.sp)
            )
            if (picks.size > 1) {
                Text(
                    text = Format.number(picks.size),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun MetaRow(message: Message, mine: Boolean, content: Color, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        if (message.edited && !message.deleted) {
            Text(
                text = stringResource(R.string.msg_edited),
                style = MaterialTheme.typography.labelSmall,
                color = content.copy(alpha = 0.72f)
            )
        }
        Text(
            text = Format.clock(message.atMs),
            style = MaterialTheme.typography.labelSmall,
            color = content.copy(alpha = 0.72f)
        )
        if (mine && !message.deleted) {
            val state = when {
                message.pending -> DeliveryState.Sending
                message.status == "seen" -> DeliveryState.Seen
                message.status == "delivered" -> DeliveryState.Delivered
                else -> DeliveryState.Sent
            }
            Crossfade(targetState = state, animationSpec = tween(CovaMotion.Standard), label = "deliveryState") { value ->
                Icon(
                    imageVector = when (value) {
                        DeliveryState.Sending -> Icons.Filled.Schedule
                        DeliveryState.Sent -> Icons.Filled.Done
                        DeliveryState.Delivered -> Icons.Filled.DoneAll
                        DeliveryState.Seen -> Icons.Filled.Visibility
                    },
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = if (value == DeliveryState.Seen) CovaTheme.colors.tick else content.copy(alpha = 0.72f)
                )
            }
        }
    }
}

private enum class DeliveryState { Sending, Sent, Delivered, Seen }
