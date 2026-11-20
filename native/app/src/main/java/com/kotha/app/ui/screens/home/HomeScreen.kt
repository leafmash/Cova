package com.kotha.app.ui.screens.home

import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.runtime.derivedStateOf
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.filled.Search
import androidx.compose.animation.scaleOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material3.Switch
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import com.kotha.app.ui.theme.LocalThemeController
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import com.kotha.app.ui.theme.BrandFont
import com.kotha.app.ui.theme.brandBrush
import com.kotha.app.ui.theme.coveBackdrop
import com.kotha.app.ui.theme.glass
import com.kotha.app.ui.theme.glowShadow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.outlined.GroupAdd
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kotha.app.R
import com.kotha.app.ui.components.ConfirmDialog
import com.kotha.app.ui.components.OfflineBar
import com.kotha.app.ui.screens.calls.CallHistoryViewModel
import androidx.compose.material.icons.outlined.DeleteSweep
import com.kotha.app.ui.components.TermsGateDialog
import com.kotha.app.ui.components.VerifyEmailBar
import com.kotha.app.ui.screens.calls.CallHistoryContent
import com.kotha.app.ui.screens.chats.ChatListContent
import com.kotha.app.ui.screens.chats.ChatListViewModel
import com.kotha.app.ui.screens.chats.FindContactSheet
import com.kotha.app.ui.screens.group.GroupPickerSheet
import com.kotha.app.ui.screens.group.PickerMode
import com.kotha.app.ui.theme.CovaTheme
import com.kotha.app.util.Format

private enum class HomeTab(
    @StringRes val label: Int,
    @StringRes val empty: Int,
    val icon: ImageVector,
    val selectedIcon: ImageVector
) {
    Chats(R.string.common_chats, R.string.list_no_chats, Icons.AutoMirrored.Outlined.Chat, Icons.AutoMirrored.Filled.Chat),
    Groups(R.string.common_groups, R.string.list_no_chats, Icons.Outlined.Groups, Icons.Filled.Groups),
    Calls(R.string.common_calls, R.string.calls_no_calls, Icons.Outlined.Call, Icons.Filled.Call)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onOpenChat: (String) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenProfile: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
    listViewModel: ChatListViewModel = hiltViewModel(),
    callViewModel: CallHistoryViewModel = hiltViewModel()
) {
    var selected by rememberSaveable { mutableIntStateOf(0) }
    var menuOpen by remember { mutableStateOf(false) }
    var showNewGroup by rememberSaveable { mutableStateOf(false) }
    var showFind by remember { mutableStateOf(false) }
    var confirmClearCalls by remember { mutableStateOf(false) }
    val chatsListState = rememberLazyListState()
    val groupsListState = rememberLazyListState()
    val searchFocus = remember { FocusRequester() }
    val collapsePx = with(LocalDensity.current) { 36.dp.roundToPx() }
    val searchCollapsed by remember {
        derivedStateOf {
            val state = if (selected == 1) groupsListState else chatsListState
            selected != 2 && (state.firstVisibleItemIndex > 0 || state.firstVisibleItemScrollOffset > collapsePx)
        }
    }
    val callRows by callViewModel.rows.collectAsStateWithLifecycle()
    var menuCenter by remember { mutableStateOf(Offset.Zero) }
    val themeController = LocalThemeController.current
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    val tabs = HomeTab.entries
    val needsTerms by viewModel.needsTerms.collectAsStateWithLifecycle()
    val unverifiedEmail by viewModel.unverifiedEmail.collectAsStateWithLifecycle()
    val resendLocked by viewModel.resendLocked.collectAsStateWithLifecycle()
    val listBase by listViewModel.base.collectAsStateWithLifecycle()
    val online by listViewModel.online.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.messages.collect { snackbar.showSnackbar(context.getString(it)) }
    }
    LaunchedEffect(Unit) {
        listViewModel.events.collect { snackbar.showSnackbar(it.resolve(context)) }
    }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.checkVerified(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .coveBackdrop()
    ) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontFamily = BrandFont,
                            fontWeight = FontWeight.Bold,
                            brush = brandBrush()
                        )
                    )
                },
                actions = {
                    if (tabs[selected] == HomeTab.Calls) {
                        HeaderAction(onClick = { showFind = true }) {
                            Icon(
                                Icons.Filled.PersonAdd,
                                contentDescription = stringResource(R.string.common_add_contact),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    AnimatedVisibility(
                        visible = searchCollapsed,
                        enter = fadeIn(tween(200)) + scaleIn(initialScale = 0.6f, animationSpec = tween(240)) + expandHorizontally(tween(240)),
                        exit = fadeOut(tween(120)) + scaleOut(targetScale = 0.6f, animationSpec = tween(160)) + shrinkHorizontally(tween(200))
                    ) {
                        HeaderAction(
                            onClick = {
                                scope.launch {
                                    val state = if (selected == 1) groupsListState else chatsListState
                                    state.animateScrollToItem(0)
                                    delay(80)
                                    runCatching { searchFocus.requestFocus() }
                                }
                            }
                        ) {
                            Icon(
                                Icons.Filled.Search,
                                contentDescription = stringResource(R.string.list_search),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    HeaderAction(
                        onClick = { menuOpen = true },
                        modifier = Modifier.onGloballyPositioned { menuCenter = it.boundsInWindow().center }
                    ) {
                        Icon(
                            Icons.Filled.MoreVert,
                            contentDescription = stringResource(R.string.common_more),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    DropdownMenu(
                        expanded = menuOpen,
                        onDismissRequest = { menuOpen = false },
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(if (themeController.dark) R.string.menu_light_mode else R.string.menu_dark_mode)) },
                            leadingIcon = {
                                Icon(
                                    imageVector = if (themeController.dark) Icons.Outlined.LightMode else Icons.Outlined.DarkMode,
                                    contentDescription = null
                                )
                            },
                            onClick = {
                                menuOpen = false
                                scope.launch {
                                    delay(220)
                                    themeController.toggle(menuCenter)
                                }
                            }
                        )
                        when (tabs[selected]) {
                            HomeTab.Chats -> {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.common_add_contact)) },
                                    leadingIcon = { Icon(Icons.Filled.PersonAdd, contentDescription = null) },
                                    onClick = {
                                        menuOpen = false
                                        showFind = true
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.menu_new_group)) },
                                    leadingIcon = { Icon(Icons.Outlined.GroupAdd, contentDescription = null) },
                                    onClick = {
                                        menuOpen = false
                                        showNewGroup = true
                                    }
                                )
                            }
                            HomeTab.Groups -> DropdownMenuItem(
                                text = { Text(stringResource(R.string.menu_new_group)) },
                                leadingIcon = { Icon(Icons.Outlined.GroupAdd, contentDescription = null) },
                                onClick = {
                                    menuOpen = false
                                    showNewGroup = true
                                }
                            )
                            HomeTab.Calls -> if (callRows.isNotEmpty()) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.calls_clear_log)) },
                                    leadingIcon = { Icon(Icons.Outlined.DeleteSweep, contentDescription = null) },
                                    onClick = {
                                        menuOpen = false
                                        confirmClearCalls = true
                                    }
                                )
                            }
                        }
                        HorizontalDivider(color = CovaTheme.colors.hairline)
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.menu_profile)) },
                            leadingIcon = { Icon(Icons.Outlined.Person, contentDescription = null) },
                            onClick = {
                                menuOpen = false
                                onOpenProfile()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.menu_settings)) },
                            leadingIcon = { Icon(Icons.Outlined.Settings, contentDescription = null) },
                            onClick = {
                                menuOpen = false
                                onOpenSettings()
                            }
                        )
                    }
                    Spacer(Modifier.size(8.dp))
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = Color.Transparent
                )
            )
        },
        bottomBar = {
            GlassTabBar(
                tabs = tabs,
                selected = selected,
                unread = listBase.totalUnread,
                onSelect = { index ->
                    if (selected != index) {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        selected = index
                    }
                }
            )
        },
        floatingActionButton = {
            val tab = tabs[selected]
            if (tab != HomeTab.Calls) {
                val fabShape = RoundedCornerShape(22.dp)
                FloatingActionButton(
                    onClick = { if (tab == HomeTab.Chats) showFind = true else showNewGroup = true },
                    shape = fabShape,
                    modifier = Modifier
                        .glowShadow(fabShape)
                        .background(brandBrush(), fabShape),
                    containerColor = Color.Transparent,
                    contentColor = Color.White,
                    elevation = FloatingActionButtonDefaults.elevation(0.dp, 0.dp, 0.dp, 0.dp)
                ) {
                    Icon(
                        imageVector = if (tab == HomeTab.Chats) Icons.Filled.PersonAdd else Icons.Outlined.GroupAdd,
                        contentDescription = stringResource(
                            if (tab == HomeTab.Chats) R.string.common_add_contact else R.string.menu_new_group
                        )
                    )
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbar) },
        containerColor = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onBackground
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            unverifiedEmail?.let { email ->
                VerifyEmailBar(
                    email = email,
                    resendLocked = resendLocked,
                    onResend = viewModel::resendVerification,
                    onVerified = { viewModel.checkVerified(true) }
                )
            }
            OfflineBar(offline = !online)
            when (tabs[selected]) {
                HomeTab.Chats -> ChatListContent(
                    groupsOnly = false,
                    viewModel = listViewModel,
                    onOpenChat = onOpenChat,
                    listState = chatsListState,
                    searchFocus = searchFocus,
                    modifier = Modifier.weight(1f)
                )
                HomeTab.Groups -> ChatListContent(
                    groupsOnly = true,
                    viewModel = listViewModel,
                    onOpenChat = onOpenChat,
                    listState = groupsListState,
                    searchFocus = searchFocus,
                    modifier = Modifier.weight(1f)
                )
                HomeTab.Calls -> CallHistoryContent(modifier = Modifier.weight(1f))
            }
        }
    }

    }

    if (confirmClearCalls) {
        ConfirmDialog(
            title = stringResource(R.string.calls_clear_title),
            text = stringResource(R.string.calls_clear_text),
            confirmLabel = stringResource(R.string.calls_clear_all),
            destructive = true,
            onConfirm = {
                callViewModel.clearAll()
                confirmClearCalls = false
            },
            onDismiss = { confirmClearCalls = false }
        )
    }

    if (showFind) {
        FindContactSheet(
            onDismiss = { showFind = false },
            onOpenChat = {
                showFind = false
                onOpenChat(it)
            }
        )
    }

    if (showNewGroup) {
        GroupPickerSheet(
            mode = PickerMode.Create,
            chatId = "",
            onDismiss = { showNewGroup = false },
            onCreated = {
                showNewGroup = false
                onOpenChat(it)
            },
            onAdded = {}
        )
    }

    if (needsTerms) {
        TermsGateDialog(onAgree = viewModel::acceptTerms, onDecline = viewModel::declineTerms)
    }
}

@Composable
private fun HeaderAction(onClick: () -> Unit, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(
        modifier = modifier
            .padding(start = 8.dp)
            .size(40.dp)
            .clip(CircleShape)
            .background(CovaTheme.colors.glassFill)
            .border(1.dp, CovaTheme.colors.glassBorder, CircleShape)
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

@Composable
private fun GlassTabBar(
    tabs: List<HomeTab>,
    selected: Int,
    unread: Int,
    onSelect: (Int) -> Unit
) {
    val barShape = RoundedCornerShape(32.dp)
    val itemShape = RoundedCornerShape(26.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(start = 20.dp, end = 20.dp, top = 6.dp, bottom = 12.dp)
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .glowShadow(barShape, 10.dp)
                .clip(barShape)
                .background(CovaTheme.colors.glassBar, barShape)
                .border(1.dp, CovaTheme.colors.glassBorder, barShape)
                .padding(6.dp)
        ) {
            val itemWidth = maxWidth / tabs.size
            val slide by animateDpAsState(
                targetValue = itemWidth * selected,
                animationSpec = spring(dampingRatio = 0.78f, stiffness = Spring.StiffnessMediumLow),
                label = "tabSlide"
            )
            Box(
                modifier = Modifier
                    .offset(x = slide)
                    .width(itemWidth)
                    .height(48.dp)
                    .background(CovaTheme.colors.accentSoft, itemShape)
                    .border(1.dp, CovaTheme.colors.glassBorder, itemShape)
            )
            Row {
                tabs.forEachIndexed { index, tab ->
                    val active = selected == index
                    val tint by animateColorAsState(
                        targetValue = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        animationSpec = tween(260),
                        label = "tabTint"
                    )
                    Box(
                        modifier = Modifier
                            .width(itemWidth)
                            .height(48.dp)
                            .clip(itemShape)
                            .clickable(role = Role.Tab) { onSelect(index) },
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val count = if (tab == HomeTab.Chats) unread else 0
                            BadgedBox(
                                badge = {
                                    if (count > 0) {
                                        Badge(
                                            containerColor = MaterialTheme.colorScheme.primary,
                                            contentColor = MaterialTheme.colorScheme.onPrimary
                                        ) { Text(if (count > 99) "99+" else Format.number(count)) }
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = if (active) tab.selectedIcon else tab.icon,
                                    contentDescription = if (active) null else stringResource(tab.label),
                                    modifier = Modifier.size(24.dp),
                                    tint = tint
                                )
                            }
                            AnimatedVisibility(
                                visible = active,
                                enter = fadeIn(tween(240, delayMillis = 80)) + expandHorizontally(tween(320)),
                                exit = fadeOut(tween(120)) + shrinkHorizontally(tween(260))
                            ) {
                                Text(
                                    text = stringResource(tab.label),
                                    modifier = Modifier.padding(start = 8.dp),
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = tint,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
