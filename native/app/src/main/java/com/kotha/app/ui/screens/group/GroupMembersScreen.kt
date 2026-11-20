package com.kotha.app.ui.screens.group

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kotha.app.R
import com.kotha.app.ui.components.ConfirmDialog
import com.kotha.app.ui.components.RoleChip
import com.kotha.app.ui.components.UserRow
import com.kotha.app.util.Format

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupMembersScreen(
    onBack: () -> Unit,
    onOpenChat: (String) -> Unit,
    onOpenUser: (String, String) -> Unit,
    viewModel: GroupInfoViewModel = hiltViewModel()
) {
    val chat by viewModel.chat.collectAsStateWithLifecycle()
    val users by viewModel.users.collectAsStateWithLifecycle()
    val busy by viewModel.busy.collectAsStateWithLifecycle()
    val blocked by viewModel.blocked.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current
    var showAdd by rememberSaveable { mutableStateOf(false) }
    var memberId by rememberSaveable { mutableStateOf<String?>(null) }
    var confirmRemove by rememberSaveable { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        viewModel.events.collect { snackbar.showSnackbar(it.resolve(context)) }
    }

    val group = chat
    if (group == null || !group.members.contains(viewModel.uid)) {
        LaunchedEffect(group == null) { if (group != null) onBack() }
        Scaffold(
            topBar = { MembersTopBar(title = stringResource(R.string.ginfo_title), onBack = onBack) },
            containerColor = MaterialTheme.colorScheme.background
        ) { padding ->
            Box(Modifier.fillMaxSize().padding(padding))
        }
        return
    }

    val admin = group.isAdmin(viewModel.uid)
    val adminIds = group.adminIds()
    val members = group.members.sortedByDescending { adminIds.contains(it) }

    Scaffold(
        topBar = {
            MembersTopBar(
                title = stringResource(R.string.ginfo_members_label, Format.number(members.size)),
                onBack = onBack
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
            LazyColumn(modifier = Modifier.weight(1f).navigationBarsPadding()) {
                if (admin) {
                    item {
                        ActionRow(
                            icon = {
                                Icon(
                                    imageVector = Icons.Filled.PersonAdd,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            },
                            label = stringResource(R.string.ginfo_add_members),
                            color = MaterialTheme.colorScheme.primary
                        ) { showAdd = true }
                    }
                }
                items(members, key = { it }) { id ->
                    val user = users[id]
                    UserRow(
                        name = user?.name.orEmpty(),
                        photo = user?.photo.orEmpty(),
                        modifier = Modifier.padding(horizontal = 8.dp),
                        subtitle = if (id == viewModel.uid) stringResource(R.string.common_you) else null,
                        enabled = id != viewModel.uid,
                        onClick = { memberId = id },
                        trailing = {
                            if (adminIds.contains(id)) RoleChip(stringResource(R.string.group_admin))
                        }
                    )
                }
            }
        }
    }

    confirmRemove?.let { id ->
        val name = users[id]?.name.orEmpty().ifEmpty { stringResource(R.string.common_user) }
        ConfirmDialog(
            title = stringResource(R.string.member_remove_title, name),
            text = stringResource(R.string.member_remove_text),
            confirmLabel = stringResource(R.string.member_remove),
            destructive = true,
            onConfirm = {
                confirmRemove = null
                viewModel.remove(id)
            },
            onDismiss = { confirmRemove = null }
        )
    }

    if (showAdd) {
        GroupPickerSheet(
            mode = PickerMode.Add,
            chatId = viewModel.chatId,
            onDismiss = { showAdd = false },
            onCreated = {},
            onAdded = { showAdd = false }
        )
    }

    memberId?.let { id ->
        val user = users[id]
        val name = user?.name.orEmpty().ifEmpty { stringResource(R.string.common_user) }
        val isAdminTarget = adminIds.contains(id)
        val isOwner = group.admin == id
        val canMessage = user != null && !blocked.contains(id)
        ModalBottomSheet(onDismissRequest = { memberId = null }) {
            Column(Modifier.navigationBarsPadding().padding(bottom = 16.dp)) {
                Text(
                    text = name,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.titleLarge
                )
                ActionRow(
                    icon = {},
                    label = stringResource(R.string.member_view_profile),
                    color = MaterialTheme.colorScheme.onSurface
                ) {
                    memberId = null
                    onOpenUser(id, viewModel.chatId)
                }
                if (canMessage) {
                    ActionRow(
                        icon = {},
                        label = stringResource(R.string.member_message, name),
                        color = MaterialTheme.colorScheme.onSurface
                    ) {
                        memberId = null
                        viewModel.openDirect(id)?.let(onOpenChat)
                    }
                }
                if (admin && !isOwner) {
                    ActionRow(
                        icon = {},
                        label = stringResource(if (isAdminTarget) R.string.member_remove_admin else R.string.member_make_admin),
                        color = MaterialTheme.colorScheme.onSurface
                    ) {
                        memberId = null
                        viewModel.setAdmin(id, !isAdminTarget)
                    }
                    ActionRow(
                        icon = {},
                        label = stringResource(R.string.member_remove),
                        color = MaterialTheme.colorScheme.error
                    ) {
                        memberId = null
                        confirmRemove = id
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MembersTopBar(title: String, onBack: () -> Unit) {
    TopAppBar(
        title = { Text(title) },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back))
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
    )
}
