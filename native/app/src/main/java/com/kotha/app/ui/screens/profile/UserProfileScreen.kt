package com.kotha.app.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kotha.app.R
import com.kotha.app.ui.components.Avatar
import com.kotha.app.ui.components.ConfirmDialog
import com.kotha.app.ui.components.ReportSheet
import com.kotha.app.ui.components.SettingsCard
import com.kotha.app.ui.components.SettingsIconTile
import com.kotha.app.ui.components.SettingsSectionLabel
import com.kotha.app.ui.screens.call.rememberCallStarter
import com.kotha.app.ui.screens.chat.presenceText
import com.kotha.app.ui.theme.CovaTheme
import com.kotha.app.ui.theme.brandBrush
import com.kotha.app.ui.theme.pressScale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserProfileScreen(
    onBack: () -> Unit,
    onOpenChat: (String) -> Unit,
    viewModel: UserProfileViewModel = hiltViewModel()
) {
    val profile by viewModel.profile.collectAsStateWithLifecycle()
    val presence by viewModel.presence.collectAsStateWithLifecycle()
    val blocked by viewModel.blocked.collectAsStateWithLifecycle()
    val startCall = rememberCallStarter()
    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current
    var confirmBlock by rememberSaveable { mutableStateOf(false) }
    var showReport by rememberSaveable { mutableStateOf(false) }
    val name = profile?.name.orEmpty().ifEmpty { stringResource(R.string.common_user) }
    val online = presence?.info?.online == true

    LaunchedEffect(Unit) {
        viewModel.events.collect { snackbar.showSnackbar(it.resolve(context)) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.profile_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .size(164.dp)
                    .background(
                        Brush.radialGradient(listOf(CovaTheme.colors.glow.copy(alpha = 0.24f), Color.Transparent)),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Avatar(
                    name = profile?.name.orEmpty(),
                    photo = profile?.photo.orEmpty(),
                    size = 128.dp,
                    online = online
                )
            }
            Spacer(Modifier.height(12.dp))
            Text(
                text = name,
                modifier = Modifier.padding(horizontal = 24.dp),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            presence?.let {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = presenceText(it.info, it.now),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (online) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (!blocked) {
                Spacer(Modifier.height(22.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ProfileAction(
                        icon = Icons.AutoMirrored.Filled.Message,
                        label = stringResource(R.string.user_message),
                        filled = true,
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.openDirect()?.let(onOpenChat) }
                    )
                    ProfileAction(
                        icon = Icons.Filled.Call,
                        label = stringResource(R.string.call_call),
                        filled = false,
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.openDirect()?.let { startCall(it, viewModel.uid, false) } }
                    )
                    ProfileAction(
                        icon = Icons.Filled.Videocam,
                        label = stringResource(R.string.call_video),
                        filled = false,
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.openDirect()?.let { startCall(it, viewModel.uid, true) } }
                    )
                }
            }
            val bio = profile?.bio.orEmpty().trim()
            SettingsSectionLabel(stringResource(R.string.profile_about))
            SettingsCard {
                Text(
                    text = bio.ifEmpty { stringResource(R.string.profile_about_empty) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 18.dp),
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (bio.isEmpty()) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(Modifier.height(16.dp))
            SettingsCard {
                ProfileRow(
                    icon = if (blocked) Icons.Filled.LockOpen else Icons.Filled.Block,
                    title = stringResource(if (blocked) R.string.user_unblock else R.string.user_block),
                    danger = !blocked,
                    onClick = { if (blocked) viewModel.unblock() else confirmBlock = true }
                )
                ProfileRow(
                    icon = Icons.Filled.Flag,
                    title = stringResource(R.string.report_user),
                    danger = true,
                    onClick = { showReport = true }
                )
            }
            Spacer(Modifier.height(32.dp))
        }
    }

    if (confirmBlock) {
        ConfirmDialog(
            title = stringResource(R.string.block_confirm_title, name),
            text = stringResource(R.string.block_confirm_text),
            confirmLabel = stringResource(R.string.block_action),
            destructive = true,
            onConfirm = {
                confirmBlock = false
                viewModel.block()
            },
            onDismiss = { confirmBlock = false }
        )
    }

    if (showReport) {
        ReportSheet(
            title = stringResource(R.string.report_title_user, name),
            showBlock = !blocked,
            onSubmit = { reason, note, alsoBlock ->
                showReport = false
                viewModel.report(reason, note, alsoBlock)
            },
            onDismiss = { showReport = false }
        )
    }
}

@Composable
private fun ProfileAction(
    icon: ImageVector,
    label: String,
    filled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(22.dp)
    val source = remember { MutableInteractionSource() }
    val tint = if (filled) Color.White else MaterialTheme.colorScheme.primary
    Column(
        modifier = modifier
            .pressScale(source)
            .clip(shape)
            .then(
                if (filled) {
                    Modifier.background(brandBrush(), shape)
                } else {
                    Modifier
                        .background(MaterialTheme.colorScheme.surfaceContainer, shape)
                        .border(1.dp, CovaTheme.colors.hairline, shape)
                }
            )
            .clickable(
                interactionSource = source,
                indication = LocalIndication.current,
                role = Role.Button,
                onClick = onClick
            )
            .padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(26.dp), tint = tint)
        Spacer(Modifier.height(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = tint
        )
    }
}

@Composable
private fun ProfileRow(icon: ImageVector, title: String, danger: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SettingsIconTile(icon = icon, danger = danger)
        Spacer(Modifier.width(16.dp))
        Text(
            text = title,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyLarge,
            color = if (danger) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
        )
    }
}
