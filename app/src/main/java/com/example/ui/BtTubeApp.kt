package com.example.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Subscriptions
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.AddCircle
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.Subscriptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.R
import com.example.audio.BackgroundAudioController
import com.example.audio.BackgroundAudioState
import com.example.audio.BackgroundAudioTrack
import com.example.data.MediaPermissionHelper
import com.example.model.ShortItem
import com.example.model.VideoItem
import com.example.ui.components.BtTubeLogo
import com.example.ui.components.NotificationsSheet
import com.example.ui.screens.CreateScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.SearchScreen
import com.example.ui.screens.ShortsScreen
import com.example.ui.screens.SubscriptionsScreen
import com.example.ui.screens.VideoPlayerScreen
import com.example.ui.theme.BtCrimson
import com.example.ui.theme.BtWhite
import com.example.ui.viewmodel.BtTubeViewModel
import com.example.util.NetworkConnectivityHelper
import kotlinx.coroutines.launch

object BtRoutes {
    const val HOME = "home"
    const val SHORTS = "shorts"
    const val CREATE = "create"
    const val SUBSCRIPTIONS = "subscriptions"
    const val PROFILE = "profile"
    const val SEARCH = "search"
    const val PLAYER = "player/{videoId}?audioMode={audioMode}"

    fun playerRoute(videoId: String, audioMode: Boolean = false): String =
        "player/$videoId?audioMode=$audioMode"
}

private data class BottomNavTab(
    val route: String,
    val labelResId: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val isCreate: Boolean = false
)

private val bottomTabs = listOf(
    BottomNavTab(
        route = BtRoutes.HOME,
        labelResId = R.string.nav_home,
        selectedIcon = Icons.Rounded.Home,
        unselectedIcon = Icons.Outlined.Home
    ),
    BottomNavTab(
        route = BtRoutes.SHORTS,
        labelResId = R.string.nav_shorts,
        selectedIcon = Icons.Rounded.Bolt,
        unselectedIcon = Icons.Outlined.Bolt
    ),
    BottomNavTab(
        route = BtRoutes.CREATE,
        labelResId = R.string.nav_create,
        selectedIcon = Icons.Rounded.AddCircle,
        unselectedIcon = Icons.Outlined.AddCircleOutline,
        isCreate = true
    ),
    BottomNavTab(
        route = BtRoutes.SUBSCRIPTIONS,
        labelResId = R.string.nav_subscriptions,
        selectedIcon = Icons.Rounded.Subscriptions,
        unselectedIcon = Icons.Outlined.Subscriptions
    ),
    BottomNavTab(
        route = BtRoutes.PROFILE,
        labelResId = R.string.nav_profile,
        selectedIcon = Icons.Rounded.AccountCircle,
        unselectedIcon = Icons.Outlined.AccountCircle
    )
)

@Composable
fun BtTubeApp(
    viewModel: BtTubeViewModel,
    onGoogleSignIn: () -> Unit = {},
    onSignOut: () -> Unit = { viewModel.signOut() },
    modifier: Modifier = Modifier
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    val homeState by viewModel.homeState.collectAsStateWithLifecycle()
    val playerState by viewModel.playerState.collectAsStateWithLifecycle()
    val searchState by viewModel.searchState.collectAsStateWithLifecycle()
    val subscriptionsState by viewModel.subscriptionsState.collectAsStateWithLifecycle()
    val profileState by viewModel.profileState.collectAsStateWithLifecycle()
    val shorts by viewModel.shorts.collectAsStateWithLifecycle()
    val likedShortIds by viewModel.likedShortIds.collectAsStateWithLifecycle()
    val subscribedIds by viewModel.subscribedChannelIds.collectAsStateWithLifecycle()
    val channels by viewModel.channels.collectAsStateWithLifecycle()
    val notifications by viewModel.notifications.collectAsStateWithLifecycle()
    val isImportingVideos by viewModel.isImportingVideos.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val backgroundAudioState by BackgroundAudioController.state.collectAsStateWithLifecycle()

    val channelsById = remember(channels) { channels.associateBy { it.id } }

    var showNotificationsSheet by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val networkState by produceState(
        initialValue = NetworkConnectivityHelper.currentNetworkState(context),
        context
    ) {
        NetworkConnectivityHelper.observeNetworkState(context).collect { value = it }
    }

    val showMessage: (String) -> Unit = { msg ->
        coroutineScope.launch {
            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarHostState.showSnackbar(msg)
        }
    }

    val multiMediaPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNotEmpty()) {
            viewModel.importPhoneVideos(context, uris) { count ->
                if (count > 0) showMessage("Added $count videos & audio files from your phone")
                else showMessage("Selected media is already in BT Tube")
            }
        }
    }

    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { treeUri ->
        if (treeUri != null) {
            viewModel.importPhoneFolder(context, treeUri) { count ->
                if (count > 0) showMessage("Imported $count videos & audio files from folder")
                else showMessage("No new videos or audio found in selected folder")
            }
        }
    }

    val mediaPermissionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        if (grants.values.any { it } || MediaPermissionHelper.hasAnyMediaPermission(context)) {
            viewModel.scanAllMediaStoreVideos(context) { count ->
                if (count > 0) showMessage("Loaded $count videos & audio tracks from your phone")
                else showMessage("All device videos & audio are already loaded")
            }
        } else {
            showMessage("Permission denied. You can still use Pick Files or Add Folder.")
        }
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ -> }

    // Automatically load MediaStore videos & audio if permission is already granted
    LaunchedEffect(Unit) {
        if (MediaPermissionHelper.hasAnyMediaPermission(context)) {
            viewModel.scanAllMediaStoreVideos(context) { }
        }
    }

    val launchMediaStoreScan: () -> Unit = {
        if (MediaPermissionHelper.hasAllMediaPermissions(context)) {
            viewModel.scanAllMediaStoreVideos(context) { count ->
                if (count > 0) showMessage("Loaded $count videos & audio tracks from your phone")
                else showMessage("All device videos & audio are already loaded")
            }
        } else {
            mediaPermissionsLauncher.launch(MediaPermissionHelper.requiredMediaPermissions())
        }
    }

    val launchMultiVideoImport: () -> Unit = {
        multiMediaPickerLauncher.launch(arrayOf("video/*", "audio/*"))
    }

    val launchFolderImport: () -> Unit = {
        folderPickerLauncher.launch(null)
    }

    val startBackgroundAudioForVideo: (VideoItem, Float, Float) -> Unit = { video, startSec, speed ->
        val uri = video.videoUri
        if (uri != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
            ) {
                runCatching {
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
            val queue = viewModel.buildAudioQueue(video)
            val track = queue.firstOrNull { it.id == video.id } ?: BackgroundAudioTrack(
                id = video.id,
                title = video.title,
                subtitle = channelsById[video.channelId]?.name ?: "Device Media",
                uriString = uri,
                isVideoSource = !video.isAudioOnly,
                durationSeconds = video.durationSeconds.coerceAtLeast(1)
            )
            BackgroundAudioController.playTrackAsAudio(
                context = context,
                track = track,
                queue = queue,
                startPositionSeconds = startSec,
                playbackSpeed = speed
            )
        }
    }

    val topLevelRoutes = remember { bottomTabs.map { it.route }.toSet() }
    val showTopLevelNav = currentRoute in topLevelRoutes

    val navigateToPlayer: (VideoItem) -> Unit = { video ->
        viewModel.openVideo(video.id)
        navController.navigate(BtRoutes.playerRoute(video.id, audioMode = video.isAudioOnly))
    }

    val navigateToAudioPlayer: (VideoItem) -> Unit = { video ->
        startBackgroundAudioForVideo(video, 0f, 1.0f)
        viewModel.openVideo(video.id)
        navController.navigate(BtRoutes.playerRoute(video.id, audioMode = true))
    }

    val navigateToTab: (String) -> Unit = { route ->
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val useNavRail = maxWidth >= 840.dp

        Row(modifier = Modifier.fillMaxSize()) {
            if (showTopLevelNav && useNavRail) {
                NavigationRail(
                    containerColor = MaterialTheme.colorScheme.surface,
                    header = {
                        Spacer(modifier = Modifier.height(12.dp))
                        BtTubeLogo(compact = true)
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                ) {
                    bottomTabs.forEach { tab ->
                        val selected = currentRoute == tab.route
                        val label = stringResource(tab.labelResId)
                        NavigationRailItem(
                            selected = selected,
                            onClick = { navigateToTab(tab.route) },
                            icon = {
                                Icon(
                                    imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                    contentDescription = label
                                )
                            },
                            label = {
                                Text(
                                    text = label,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            colors = NavigationRailItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer
                            ),
                            modifier = Modifier.testTag("nav_${tab.route}")
                        )
                    }
                }
            }

            Scaffold(
                modifier = Modifier.weight(1f),
                contentWindowInsets = WindowInsets(0, 0, 0, 0),
                snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                bottomBar = {
                    val showMiniAudio = backgroundAudioState.currentTrack != null &&
                        currentRoute != BtRoutes.PLAYER
                    val showNavBar = showTopLevelNav && !useNavRail
                    if (showMiniAudio || showNavBar) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .then(
                                    if (!showNavBar) {
                                        Modifier.windowInsetsPadding(WindowInsets.navigationBars)
                                    } else {
                                        Modifier
                                    }
                                )
                        ) {
                            if (showMiniAudio) {
                                MiniAudioPlayerBar(
                                    state = backgroundAudioState,
                                    onOpenPlayer = {
                                        val trackId = backgroundAudioState.currentTrack?.id
                                        if (trackId != null) {
                                            viewModel.openVideo(trackId)
                                            navController.navigate(
                                                BtRoutes.playerRoute(trackId, audioMode = true)
                                            )
                                        }
                                    },
                                    onTogglePlayPause = {
                                        BackgroundAudioController.togglePlayPause(context)
                                    },
                                    onNext = { BackgroundAudioController.playNext(context) },
                                    onClose = { BackgroundAudioController.stop(context) }
                                )
                            }
                            if (showNavBar) {
                                NavigationBar(
                                    containerColor = MaterialTheme.colorScheme.surface,
                                    tonalElevation = 3.dp,
                                    modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
                                ) {
                                    bottomTabs.forEach { tab ->
                                        val selected = currentRoute == tab.route
                                        val label = stringResource(tab.labelResId)
                                        NavigationBarItem(
                                            selected = selected,
                                            onClick = { navigateToTab(tab.route) },
                                            icon = {
                                                if (tab.isCreate) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(34.dp)
                                                            .clip(CircleShape)
                                                            .background(
                                                                if (selected) BtCrimson
                                                                else MaterialTheme.colorScheme.primaryContainer
                                                            ),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Icon(
                                                            imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                                            contentDescription = label,
                                                            tint = if (selected) BtWhite else BtCrimson,
                                                            modifier = Modifier.size(22.dp)
                                                        )
                                                    }
                                                } else {
                                                    Icon(
                                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                                        contentDescription = label
                                                    )
                                                }
                                            },
                                            label = {
                                                Text(
                                                    text = label,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            },
                                            colors = NavigationBarItemDefaults.colors(
                                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                                indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                            ),
                                            modifier = Modifier.testTag("nav_${tab.route}")
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            ) { innerPadding ->
                NavHost(
                    navController = navController,
                    startDestination = BtRoutes.HOME,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    enterTransition = {
                        fadeIn(animationSpec = tween(200))
                    },
                    exitTransition = {
                        fadeOut(animationSpec = tween(160))
                    }
                ) {
                    composable(BtRoutes.HOME) {
                        HomeScreen(
                            homeState = homeState,
                            isImportingVideos = isImportingVideos,
                            networkState = networkState,
                            streamOnMobileData = settings.streamOnMobileData,
                            onSelectCategory = viewModel::selectCategory,
                            onRefresh = viewModel::refreshHomeFeed,
                            onRestoreHidden = viewModel::restoreHiddenVideos,
                            onSearchClick = { navController.navigate(BtRoutes.SEARCH) },
                            onNotificationsClick = { showNotificationsSheet = true },
                            onScanAllMediaStoreVideos = launchMediaStoreScan,
                            onImportMultipleVideos = launchMultiVideoImport,
                            onImportVideoFolder = launchFolderImport,
                            onVideoClick = navigateToPlayer,
                            onPlayAsAudio = navigateToAudioPlayer,
                            onChannelClick = { channelId ->
                                viewModel.selectSubscriptionChannel(channelId)
                                navigateToTab(BtRoutes.SUBSCRIPTIONS)
                            },
                            onToggleWatchLater = { videoId ->
                                viewModel.toggleWatchLater(videoId)
                                showMessage("Updated Watch Later")
                            },
                            onToggleSubscribe = { channelId ->
                                viewModel.toggleSubscription(channelId)
                                showMessage("Updated channel subscription")
                            },
                            onShareVideo = { video ->
                                shareVideo(context, video.title, video.id)
                            },
                            onHideVideo = { videoId ->
                                viewModel.hideVideo(videoId)
                                showMessage("Video hidden from feed")
                            }
                        )
                    }

                    composable(BtRoutes.SHORTS) {
                        ShortsScreen(
                            shorts = shorts,
                            channelsById = channelsById,
                            likedShortIds = likedShortIds,
                            subscribedChannelIds = subscribedIds,
                            onToggleLike = viewModel::toggleShortLike,
                            onAddComment = viewModel::addShortComment,
                            onToggleFollow = viewModel::toggleSubscription,
                            onShareShort = { short ->
                                shareShort(context, short)
                            },
                            onScanDeviceMedia = launchMediaStoreScan
                        )
                    }

                    composable(BtRoutes.CREATE) {
                        CreateScreen(
                            onPublishContent = { title, description, category, sceneStyle, isShort, videoUri ->
                                val newId = viewModel.publishCreatorContent(
                                    title = title,
                                    description = description,
                                    category = category,
                                    sceneStyle = sceneStyle,
                                    isShort = isShort,
                                    videoUri = videoUri
                                )
                                if (isShort) {
                                    showMessage("Published Short to BT Tube")
                                    navigateToTab(BtRoutes.SHORTS)
                                } else {
                                    showMessage("Published Video to BT Tube")
                                    viewModel.openVideo(newId)
                                    navController.navigate(BtRoutes.playerRoute(newId))
                                }
                            },
                            onImportMultipleVideos = launchMultiVideoImport,
                            onImportVideoFolder = launchFolderImport
                        )
                    }

                    composable(BtRoutes.SUBSCRIPTIONS) {
                        SubscriptionsScreen(
                            state = subscriptionsState,
                            onSelectChannel = viewModel::selectSubscriptionChannel,
                            onToggleSubscribe = viewModel::toggleSubscription,
                            onVideoClick = navigateToPlayer,
                            onToggleWatchLater = { videoId ->
                                viewModel.toggleWatchLater(videoId)
                                showMessage("Updated Watch Later")
                            },
                            onShareVideo = { video ->
                                shareVideo(context, video.title, video.id)
                            },
                            onSearchClick = { navController.navigate(BtRoutes.SEARCH) },
                            onNotificationsClick = { showNotificationsSheet = true },
                            onExploreHome = { navigateToTab(BtRoutes.HOME) }
                        )
                    }

                    composable(BtRoutes.PROFILE) {
                        ProfileScreen(
                            state = profileState,
                            networkState = networkState,
                            onGoogleSignIn = onGoogleSignIn,
                            onSignOut = {
                                onSignOut()
                                showMessage("Signed out")
                            },
                            onVideoClick = navigateToPlayer,
                            onScanAllMediaStoreVideos = launchMediaStoreScan,
                            onImportMultipleVideos = launchMultiVideoImport,
                            onImportVideoFolder = launchFolderImport,
                            onClearHistory = {
                                viewModel.clearWatchHistory()
                                showMessage("Watch history cleared")
                            },
                            onUpdateThemeMode = viewModel::updateThemeMode,
                            onToggleAutoplay = viewModel::toggleAutoplayNext,
                            onToggleNotifications = viewModel::toggleNotificationsEnabled,
                            onToggleMobileData = {
                                viewModel.toggleStreamOnMobileData()
                                showMessage("Updated mobile data streaming preference")
                            },
                            onUpdateQuality = viewModel::updateDefaultQuality
                        )
                    }

                    composable(
                        route = BtRoutes.SEARCH,
                        enterTransition = {
                            slideIntoContainer(
                                AnimatedContentTransitionScope.SlideDirection.Start,
                                tween(220)
                            )
                        },
                        exitTransition = {
                            slideOutOfContainer(
                                AnimatedContentTransitionScope.SlideDirection.End,
                                tween(200)
                            )
                        }
                    ) {
                        SearchScreen(
                            state = searchState,
                            onBack = { navController.popBackStack() },
                            onQueryChange = viewModel::updateSearchQuery,
                            onSubmitSearch = viewModel::submitSearch,
                            onSelectFilter = viewModel::selectSearchFilter,
                            onRemoveRecentSearch = viewModel::removeRecentSearch,
                            onClearRecentSearches = viewModel::clearRecentSearches,
                            onVideoClick = navigateToPlayer,
                            onShortClick = { navigateToTab(BtRoutes.SHORTS) },
                            onToggleSubscribe = viewModel::toggleSubscription
                        )
                    }

                    composable(
                        route = BtRoutes.PLAYER,
                        arguments = listOf(
                            navArgument("videoId") { type = NavType.StringType },
                            navArgument("audioMode") {
                                type = NavType.BoolType
                                defaultValue = false
                            }
                        ),
                        enterTransition = {
                            slideIntoContainer(
                                AnimatedContentTransitionScope.SlideDirection.Up,
                                tween(240)
                            )
                        },
                        exitTransition = {
                            slideOutOfContainer(
                                AnimatedContentTransitionScope.SlideDirection.Down,
                                tween(220)
                            )
                        }
                    ) { entry ->
                        val initialAudio = entry.arguments?.getBoolean("audioMode") ?: false
                        VideoPlayerScreen(
                            state = playerState,
                            backgroundAudioState = backgroundAudioState,
                            initialAudioMode = initialAudio,
                            onBack = {
                                viewModel.setFullscreen(false)
                                navController.popBackStack()
                            },
                            onTogglePlayPause = viewModel::togglePlayPause,
                            onSeekTo = viewModel::seekTo,
                            onSeekBy = viewModel::seekBy,
                            onSyncPosition = viewModel::syncPlaybackPosition,
                            onAdvancePlayback = viewModel::advancePlayback,
                            onSelectSpeed = viewModel::setPlaybackSpeed,
                            onSelectQuality = viewModel::updateDefaultQuality,
                            onToggleFullscreen = viewModel::toggleFullscreen,
                            onExitFullscreen = { viewModel.setFullscreen(false) },
                            onStartBackgroundAudio = startBackgroundAudioForVideo,
                            onStopBackgroundAudio = { BackgroundAudioController.stop(context) },
                            onToggleBackgroundPlayPause = { BackgroundAudioController.togglePlayPause(context) },
                            onSeekBackgroundAudio = { sec -> BackgroundAudioController.seekTo(context, sec) },
                            onSetBackgroundSpeed = { speed -> BackgroundAudioController.setPlaybackSpeed(context, speed) },
                            onBackgroundNext = { BackgroundAudioController.playNext(context) },
                            onBackgroundPrevious = { BackgroundAudioController.playPrevious(context) },
                            onToggleSubscribe = viewModel::toggleSubscription,
                            onToggleLike = viewModel::toggleVideoLike,
                            onToggleDislike = viewModel::toggleVideoDislike,
                            onToggleWatchLater = { videoId ->
                                viewModel.toggleWatchLater(videoId)
                                showMessage("Updated Watch Later")
                            },
                            onShareVideo = { video ->
                                shareVideo(context, video.title, video.id)
                            },
                            onAddComment = viewModel::addVideoComment,
                            onSelectRelatedVideo = { related ->
                                viewModel.openVideo(related.id)
                            }
                        )
                    }
                }
            }
        }
    }

    if (showNotificationsSheet) {
        NotificationsSheet(
            notifications = notifications,
            channelsById = channelsById,
            onDismiss = { showNotificationsSheet = false },
            onNotificationClick = { notification ->
                viewModel.markNotificationRead(notification.id)
                showNotificationsSheet = false
                viewModel.openVideo(notification.videoId)
                navController.navigate(BtRoutes.playerRoute(notification.videoId))
            },
            onMarkAllRead = viewModel::markAllNotificationsRead
        )
    }
}

@Composable
private fun MiniAudioPlayerBar(
    state: BackgroundAudioState,
    onOpenPlayer: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onNext: () -> Unit,
    onClose: () -> Unit
) {
    val track = state.currentTrack ?: return
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 6.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpenPlayer)
            .testTag("mini_audio_player_bar")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(BtCrimson),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Headphones,
                    contentDescription = null,
                    tint = BtWhite,
                    modifier = Modifier.size(20.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = track.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = if (track.isVideoSource) "Video playing as audio • Background" else "Device audio • Background",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            IconButton(
                onClick = onTogglePlayPause,
                modifier = Modifier.testTag("mini_player_play_pause")
            ) {
                Icon(
                    imageVector = if (state.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    contentDescription = if (state.isPlaying) "Pause audio" else "Play audio",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
            IconButton(
                onClick = onNext,
                modifier = Modifier.testTag("mini_player_next")
            ) {
                Icon(
                    imageVector = Icons.Rounded.SkipNext,
                    contentDescription = "Next track",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
            IconButton(
                onClick = onClose,
                modifier = Modifier.testTag("mini_player_close")
            ) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = "Stop background audio",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun shareVideo(context: Context, title: String, videoId: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, title)
        putExtra(Intent.EXTRA_TEXT, "Watch \"$title\" on BT Tube (bttube.app/watch/$videoId)")
    }
    runCatching {
        context.startActivity(Intent.createChooser(intent, "Share video via"))
    }
}

private fun shareShort(context: Context, short: ShortItem) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, short.title)
        putExtra(Intent.EXTRA_TEXT, "Check out \"${short.title}\" on BT Tube Shorts!")
    }
    runCatching {
        context.startActivity(Intent.createChooser(intent, "Share Short via"))
    }
}
