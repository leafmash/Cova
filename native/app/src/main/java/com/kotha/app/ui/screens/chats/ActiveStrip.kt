package com.kotha.app.ui.screens.chats

import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.remember
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.Dialog
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kotha.app.R
import com.kotha.app.core.AppConfig
import com.kotha.app.ui.components.Avatar
import com.kotha.app.ui.components.softFieldColors
import com.kotha.app.ui.theme.CovaTheme
import com.kotha.app.ui.theme.brandBrush
import com.kotha.app.util.Format

@Composable
fun ActiveStrip(
    state: StripState,
    onOpenPeer: (String) -> Unit,
    onOpenNote: (StripItem) -> Unit,
    onEditOwn: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item(key = "own") {
            StripCell(
                name = stringResource(R.string.note_your),
                photo = state.ownPhoto,
                avatarName = state.ownName,
                note = state.ownNote,
                online = false,
                own = true,
                onClick = onEditOwn,
                onNoteClick = onEditOwn
            )
        }
        items(state.items, key = { it.uid }) { item ->
            StripCell(
                name = item.name.substringBefore(' '),
                photo = item.photo,
                avatarName = item.name,
                note = item.note,
                online = item.online,
                own = false,
                onClick = { onOpenPeer(item.uid) },
                onNoteClick = { onOpenNote(item) }
            )
        }
    }
}

@Composable
private fun StripCell(
    name: String,
    photo: String,
    avatarName: String,
    note: String,
    online: Boolean,
    own: Boolean,
    onClick: () -> Unit,
    onNoteClick: () -> Unit
) {
    val cellShape = RoundedCornerShape(20.dp)
    Column(
        modifier = Modifier
            .width(80.dp)
            .clip(cellShape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(vertical = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(modifier = Modifier.fillMaxWidth().height(110.dp)) {
            Box(modifier = Modifier.align(Alignment.BottomCenter)) {
                val ring = if (note.isNotEmpty() || online) {
                    Modifier.border(2.dp, brandBrush(), CircleShape).padding(3.dp)
                } else {
                    Modifier.padding(5.dp)
                }
                Box(modifier = ring) {
                    Avatar(name = avatarName, photo = photo, size = 54.dp, online = false)
                }
                if (online && !own) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(19.dp)
                            .background(MaterialTheme.colorScheme.background, CircleShape)
                            .padding(3.dp)
                            .background(CovaTheme.colors.success, CircleShape)
                    )
                }
                if (own) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(22.dp)
                            .background(MaterialTheme.colorScheme.background, CircleShape)
                            .padding(2.dp)
                            .background(brandBrush(), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = Color.White
                        )
                    }
                }
            }
            if (note.isNotEmpty()) {
                NoteBubble(text = note, ghost = false, onClick = onNoteClick, modifier = Modifier.align(Alignment.TopCenter))
            } else if (own) {
                NoteBubble(text = stringResource(R.string.note_add), ghost = true, onClick = onNoteClick, modifier = Modifier.align(Alignment.TopCenter))
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = name.ifEmpty { stringResource(R.string.common_user) },
            style = MaterialTheme.typography.labelMedium,
            color = if (own) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun NoteBubble(text: String, ghost: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = CovaTheme.colors
    val shape = RoundedCornerShape(16.dp)
    val fill = if (ghost) colors.glassFill else MaterialTheme.colorScheme.surfaceContainerHighest
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .widthIn(max = 80.dp)
                .height(50.dp)
                .clip(shape)
                .background(fill, shape)
                .border(1.dp, colors.glassBorder, shape)
                .clickable(role = Role.Button, onClick = onClick)
                .padding(horizontal = 8.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, lineHeight = 13.sp),
                color = if (ghost) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }
        Box(
            modifier = Modifier
                .padding(top = 2.dp)
                .size(6.dp)
                .background(fill, CircleShape)
                .border(1.dp, colors.glassBorder, CircleShape)
        )
    }
}

@Composable
fun noteLeftLabel(leftMs: Long): String {
    val minutes = (leftMs / 60_000L).coerceAtLeast(1L)
    return if (minutes >= 60L) {
        stringResource(R.string.note_left_hours, Format.number(((minutes + 59L) / 60L).toInt()))
    } else {
        stringResource(R.string.note_left_minutes, Format.number(minutes.toInt()))
    }
}

@Composable
private fun NotePillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    filled: Boolean = true,
    enabled: Boolean = true,
    tint: Color = Color.White
) {
    val shape = CircleShape
    val base = modifier
        .height(42.dp)
        .clip(shape)
    val styled = if (filled) {
        base.background(brandBrush(), shape, alpha = if (enabled) 1f else 0.4f)
    } else {
        base.border(1.dp, Color.White.copy(alpha = 0.22f), shape)
    }
    Box(
        modifier = styled
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 26.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge.copy(fontSize = 14.sp, letterSpacing = 0.6.sp),
            fontWeight = FontWeight.SemiBold,
            color = if (filled) Color.White else tint,
            maxLines = 1
        )
    }
}

@Composable
private fun NoteStage(
    photo: String,
    name: String,
    onDismiss: () -> Unit,
    bubble: @Composable () -> Unit,
    footer: @Composable ColumnScope.() -> Unit
) {
    val bubbleColor = MaterialTheme.colorScheme.surfaceContainerHighest
    val bubbleShape = RoundedCornerShape(26.dp)
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.62f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .padding(horizontal = 32.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {}
                    ),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .widthIn(min = 180.dp, max = 260.dp)
                        .clip(bubbleShape)
                        .background(bubbleColor)
                        .border(1.dp, CovaTheme.colors.glassBorder, bubbleShape)
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    bubble()
                }
                Box(
                    modifier = Modifier
                        .offset(x = (-22).dp, y = (-4).dp)
                        .size(18.dp)
                        .background(bubbleColor, CircleShape)
                )
                Box(
                    modifier = Modifier
                        .offset(x = (-17).dp, y = 2.dp)
                        .size(10.dp)
                        .background(bubbleColor, CircleShape)
                )
                Spacer(Modifier.height(6.dp))
                Avatar(name = name, photo = photo, size = 104.dp)
                Spacer(Modifier.height(12.dp))
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleSmall.copy(fontSize = 15.sp),
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
                footer()
            }
        }
    }
}

@Composable
fun NoteViewDialog(
    name: String,
    photo: String,
    text: String,
    leftMs: Long,
    onMessage: () -> Unit,
    onReply: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var reply by rememberSaveable { mutableStateOf("") }
    NoteStage(
        photo = photo,
        name = name,
        onDismiss = onDismiss,
        bubble = {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp, lineHeight = 22.sp),
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
        },
        footer = {
            if (leftMs > 0L) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = noteLeftLabel(leftMs),
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = 0.68f)
                )
            }
            Spacer(Modifier.height(16.dp))
            NoteReplyField(
                value = reply,
                onValueChange = { if (it.length <= AppConfig.EDIT_MAX) reply = it },
                onSend = { onReply(reply.trim()) }
            )
            Spacer(Modifier.height(12.dp))
            NotePillButton(text = stringResource(R.string.note_message), onClick = onMessage, filled = false, tint = Color.White)
        }
    )
}

@Composable
private fun NoteReplyField(value: String, onValueChange: (String) -> Unit, onSend: () -> Unit) {
    val shape = RoundedCornerShape(26.dp)
    val ready = value.isNotBlank()
    Row(
        modifier = Modifier
            .widthIn(min = 240.dp, max = 300.dp)
            .fillMaxWidth()
            .clip(shape)
            .background(Color.White.copy(alpha = 0.10f), shape)
            .border(1.dp, Color.White.copy(alpha = 0.22f), shape)
            .padding(start = 18.dp, end = 5.dp, top = 5.dp, bottom = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.weight(1f),
            textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp, color = Color.White),
            cursorBrush = SolidColor(Color.White),
            maxLines = 3,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            decorationBox = { inner ->
                Box {
                    if (value.isEmpty()) {
                        Text(
                            text = stringResource(R.string.note_reply_hint),
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                            color = Color.White.copy(alpha = 0.55f)
                        )
                    }
                    inner()
                }
            }
        )
        Spacer(Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(brandBrush(), CircleShape, alpha = if (ready) 1f else 0.35f)
                .clickable(enabled = ready, role = Role.Button, onClick = onSend),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Send,
                contentDescription = stringResource(R.string.chat_send),
                modifier = Modifier.size(18.dp),
                tint = Color.White
            )
        }
    }
}

@Composable
fun NoteEditorDialog(
    current: String,
    leftMs: Long,
    photo: String,
    name: String,
    onSave: (String) -> Unit,
    onClear: () -> Unit,
    onDismiss: () -> Unit
) {
    var text by rememberSaveable { mutableStateOf(current) }
    val focusRequester = remember { FocusRequester() }
    val trimmed = text.trim()
    LaunchedEffect(Unit) { runCatching { focusRequester.requestFocus() } }
    NoteStage(
        photo = photo,
        name = name.ifEmpty { stringResource(R.string.note_your) },
        onDismiss = onDismiss,
        bubble = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                BasicTextField(
                    value = text,
                    onValueChange = { if (it.length <= AppConfig.NOTE_MAX) text = it },
                    modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = 16.sp,
                        lineHeight = 22.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    maxLines = 3,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    decorationBox = { inner ->
                        Box(contentAlignment = Alignment.Center) {
                            if (text.isEmpty()) {
                                Text(
                                    text = stringResource(R.string.note_placeholder),
                                    style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            }
                            inner()
                        }
                    }
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = Format.number(text.length) + "/" + Format.number(AppConfig.NOTE_MAX),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        footer = {
            Spacer(Modifier.height(2.dp))
            Text(
                text = if (current.isNotEmpty() && leftMs > 0L) noteLeftLabel(leftMs) else stringResource(R.string.note_hint),
                style = MaterialTheme.typography.labelMedium,
                color = Color.White.copy(alpha = 0.68f),
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(18.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                if (current.isNotEmpty()) {
                    NotePillButton(
                        text = stringResource(R.string.note_delete),
                        onClick = onClear,
                        filled = false,
                        tint = Color(0xFFFF8A98)
                    )
                }
                NotePillButton(
                    text = stringResource(R.string.note_share),
                    onClick = { onSave(trimmed) },
                    enabled = trimmed.isNotEmpty()
                )
            }
        }
    )
}
