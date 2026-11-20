package com.kotha.app.ui.screens.chats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kotha.app.R
import com.kotha.app.core.AppConfig
import com.kotha.app.data.app.AppForeground
import com.kotha.app.data.auth.AuthRepository
import com.kotha.app.data.chat.ChatActionsRepository
import com.kotha.app.data.chat.ChatListRepository
import com.kotha.app.data.chat.ChatRepository
import com.kotha.app.data.chat.ContactRepository
import com.kotha.app.data.chat.ReplyRef
import com.kotha.app.data.chat.DraftStore
import com.kotha.app.data.chat.isCleared
import com.kotha.app.data.chat.isMuted
import com.kotha.app.data.chat.unreadOf
import com.kotha.app.data.model.Chat
import com.kotha.app.data.model.ChatPrefs
import com.kotha.app.data.model.UserProfile
import com.kotha.app.data.net.NetworkMonitor
import com.kotha.app.data.note.NoteRepository
import com.kotha.app.data.note.activeNote
import com.kotha.app.data.presence.PresenceRepository
import com.kotha.app.data.presence.RtPresence
import com.kotha.app.data.user.UserRepository
import com.kotha.app.ui.UiMessage
import com.kotha.app.util.ticker
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ChatRowUi(
    val id: String,
    val peerUid: String,
    val name: String,
    val photo: String,
    val group: Boolean,
    val lastMessage: String,
    val lastFromMe: Boolean,
    val senderName: String,
    val hiddenLast: Boolean,
    val draft: String,
    val timeMs: Long,
    val unread: Int,
    val muted: Boolean,
    val pinned: Boolean,
    val archived: Boolean,
    val online: Boolean,
    val peerGone: Boolean = false
)

data class ListBase(
    val rows: List<ChatRowUi> = emptyList(),
    val totalUnread: Int = 0,
    val archivedUnread: Boolean = false,
    val ready: Boolean = false
)

data class StripItem(
    val uid: String,
    val name: String,
    val photo: String,
    val note: String,
    val noteAtMs: Long,
    val online: Boolean
)

data class StripState(
    val ownName: String = "",
    val ownPhoto: String = "",
    val ownNote: String = "",
    val ownNoteAtMs: Long = 0L,
    val items: List<StripItem> = emptyList()
)

private data class ListData(
    val chats: List<Chat>,
    val prefs: ChatPrefs,
    val blocked: Set<String>,
    val cleared: Map<String, Long>,
    val users: Map<String, UserProfile>,
    val deleted: Set<String>
)

private data class ListExtras(
    val active: String?,
    val foreground: Boolean,
    val drafts: Map<String, String>,
    val rt: Map<String, RtPresence>,
    val now: Long
)

@HiltViewModel
class ChatListViewModel @Inject constructor(
    private val chatListRepository: ChatListRepository,
    private val contactRepository: ContactRepository,
    private val userRepository: UserRepository,
    private val presenceRepository: PresenceRepository,
    private val authRepository: AuthRepository,
    private val actions: ChatActionsRepository,
    private val noteRepository: NoteRepository,
    private val chatRepository: ChatRepository,
    appForeground: AppForeground,
    draftStore: DraftStore,
    networkMonitor: NetworkMonitor
) : ViewModel() {

    val online: StateFlow<Boolean> = networkMonitor.online

    private val people = combine(userRepository.users, userRepository.deleted) { users, deleted -> users to deleted }

    private val data = combine(
        chatListRepository.chats,
        chatListRepository.prefs,
        chatListRepository.blocked,
        chatListRepository.cleared,
        people
    ) { chats, prefs, blocked, cleared, known -> ListData(chats, prefs, blocked, cleared, known.first, known.second) }

    private val extras = combine(
        chatListRepository.activeChatId,
        appForeground.foreground,
        draftStore.drafts,
        presenceRepository.peers,
        ticker(AppConfig.PRESENCE_TICK_MS)
    ) { active, foreground, drafts, rt, now -> ListExtras(active, foreground, drafts, rt, now) }

    val base: StateFlow<ListBase> = combine(data, extras, chatListRepository.ready) { d, e, ready ->
        compute(d, e, ready)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ListBase())

    val strip: StateFlow<StripState> = combine(data, extras) { d, e -> computeStrip(d, e) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), StripState())

    private val mutableEvents = MutableSharedFlow<UiMessage>(extraBufferCapacity = 8)
    val events: SharedFlow<UiMessage> = mutableEvents.asSharedFlow()

    fun openDirect(peerUid: String): String? = contactRepository.openDirect(peerUid)

    fun saveNote(text: String) {
        viewModelScope.launch {
            runCatching { noteRepository.set(text) }
                .onSuccess { mutableEvents.emit(UiMessage(R.string.note_saved)) }
                .onFailure { mutableEvents.emit(UiMessage(R.string.note_fail)) }
        }
    }

    fun replyToNote(peerUid: String, note: String, text: String) {
        val value = text.trim()
        if (value.isEmpty()) return
        val uid = authRepository.user?.uid ?: return
        val chatId = contactRepository.openDirect(peerUid) ?: return
        viewModelScope.launch {
            runCatching {
                chatRepository.send(
                    chatId,
                    listOf(uid, peerUid),
                    mapOf("type" to "text", "text" to value),
                    ReplyRef(AppConfig.NOTE_REPLY_PREFIX + peerUid, note)
                )
            }
                .onSuccess { mutableEvents.emit(UiMessage(R.string.note_reply_sent)) }
                .onFailure { mutableEvents.emit(UiMessage(R.string.chat_send_fail)) }
        }
    }

    fun clearNote() {
        viewModelScope.launch {
            runCatching { noteRepository.clear() }
                .onSuccess { mutableEvents.emit(UiMessage(R.string.note_removed)) }
                .onFailure { mutableEvents.emit(UiMessage(R.string.note_fail)) }
        }
    }

    fun togglePin(chatId: String) {
        viewModelScope.launch { mutableEvents.emit(UiMessage(actions.togglePin(chatId))) }
    }

    fun toggleArchive(chatId: String) {
        viewModelScope.launch { mutableEvents.emit(UiMessage(actions.toggleArchive(chatId))) }
    }

    fun mute(chatId: String, until: Long) {
        viewModelScope.launch { mutableEvents.emit(UiMessage(actions.mute(chatId, until))) }
    }

    fun deleteConversation(chatId: String) {
        viewModelScope.launch { mutableEvents.emit(UiMessage(actions.clearChats(listOf(chatId)))) }
    }

    private fun computeStrip(d: ListData, e: ListExtras): StripState {
        val uid = authRepository.user?.uid.orEmpty()
        if (uid.isEmpty()) return StripState()
        userRepository.watch(uid)
        val own = d.users[uid]
        val items = d.chats
            .filter { !it.group }
            .map { it.peerId(uid) }
            .filter { it.isNotEmpty() && !d.blocked.contains(it) }
            .distinct()
            .mapNotNull { peerId ->
                val peer = d.users[peerId] ?: return@mapNotNull null
                val note = peer.activeNote(e.now)
                val online = presenceRepository.presenceOf(peer, e.rt[peerId]).online
                if (note == null && !online) {
                    null
                } else {
                    StripItem(peerId, peer.name, peer.photo, note.orEmpty(), if (note == null) 0L else peer.noteAtMs, online)
                }
            }
            .sortedWith(compareByDescending<StripItem> { it.note.isNotEmpty() }.thenByDescending { it.noteAtMs }.thenBy { it.name })
        return StripState(
            ownName = own?.name.orEmpty(),
            ownPhoto = own?.photo.orEmpty(),
            ownNote = own?.activeNote(e.now).orEmpty(),
            ownNoteAtMs = own?.noteAtMs ?: 0L,
            items = items
        )
    }

    private fun compute(d: ListData, e: ListExtras, ready: Boolean): ListBase {
        val uid = authRepository.user?.uid.orEmpty()
        var total = 0
        var archivedUnread = false
        val rows = d.chats
            .filter { it.lastMessage.isNotEmpty() && !isCleared(it, d.cleared) }
            .sortedByDescending { it.lastAtMs }
            .mapNotNull { chat ->
                val unread = unreadOf(chat, uid, e.active, e.foreground, d.blocked)
                val muted = isMuted(d.prefs, chat.id, e.now)
                val archived = d.prefs.archived.containsKey(chat.id)
                if (!muted) total += unread
                if (archived && !muted && unread > 0) archivedUnread = true
                val draft = if (e.active == chat.id) "" else e.drafts[chat.id].orEmpty().trim()
                if (chat.group) {
                    val hiddenLast = d.blocked.contains(chat.lastFrom)
                    val sender = if (chat.lastFrom != uid && chat.lastFrom.isNotEmpty() && !hiddenLast) {
                        d.users[chat.lastFrom]?.name.orEmpty()
                    } else {
                        ""
                    }
                    ChatRowUi(
                        id = chat.id,
                        peerUid = "",
                        name = chat.name,
                        photo = chat.photo,
                        group = true,
                        lastMessage = chat.lastMessage,
                        lastFromMe = chat.lastFrom == uid,
                        senderName = sender,
                        hiddenLast = hiddenLast,
                        draft = draft,
                        timeMs = chat.lastAtMs,
                        unread = unread,
                        muted = muted,
                        pinned = d.prefs.pinned.containsKey(chat.id),
                        archived = archived,
                        online = false
                    )
                } else {
                    val peerId = chat.peerId(uid)
                    val peer = d.users[peerId]
                    val gone = peer == null && d.deleted.contains(peerId)
                    if ((peer == null && !gone) || d.blocked.contains(peerId)) {
                        null
                    } else {
                        ChatRowUi(
                            id = chat.id,
                            peerUid = peerId,
                            name = peer?.name.orEmpty(),
                            photo = peer?.photo.orEmpty(),
                            group = false,
                            lastMessage = chat.lastMessage,
                            lastFromMe = chat.lastFrom == uid,
                            senderName = "",
                            hiddenLast = false,
                            draft = draft,
                            timeMs = chat.lastAtMs,
                            unread = unread,
                            muted = muted,
                            pinned = d.prefs.pinned.containsKey(chat.id),
                            archived = archived,
                            online = peer != null && presenceRepository.presenceOf(peer, e.rt[peerId]).online,
                            peerGone = gone
                        )
                    }
                }
            }
        return ListBase(rows, total, archivedUnread, ready)
    }
}
