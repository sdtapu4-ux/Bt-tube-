package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Login
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material.icons.rounded.CellTower
import androidx.compose.material.icons.rounded.HighQuality
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.Channel
import com.example.model.PlaybackQuality
import com.example.model.ThemeMode
import com.example.model.VideoItem
import com.example.ui.components.BtTubeLogo
import com.example.ui.components.CompactVideoRowCard
import com.example.ui.components.VideoThumbnailSurface
import com.example.ui.theme.BtCrimson
import com.example.ui.theme.BtCrimsonDark
import com.example.ui.theme.BtWhite
import com.example.ui.theme.OutfitFontFamily
import com.example.ui.viewmodel.ProfileUiState
import com.example.util.FormatUtils
import com.example.util.NetworkConnectionState

private enum class AuthDialogMode { NONE, SIGN_IN, SIGN_UP }
private enum class LibrarySheetMode { NONE, HISTORY, LIKED, WATCH_LATER }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    state: ProfileUiState,
    networkState: NetworkConnectionState,
    onSignIn: (email: String, displayName: String?) -> Unit,
    onSignOut: () -> Unit,
    onVideoClick: (VideoItem) -> Unit,
    onScanAllMediaStoreVideos: () -> Unit,
    onImportMultipleVideos: () -> Unit,
    onImportVideoFolder: () -> Unit,
    onClearHistory: () -> Unit,
    onUpdateThemeMode: (ThemeMode) -> Unit,
    onToggleAutoplay: () -> Unit,
    onToggleNotifications: () -> Unit,
    onToggleMobileData: () -> Unit,
    onUpdateQuality: (PlaybackQuality) -> Unit,
    modifier: Modifier = Modifier
) {
    var authMode by remember { mutableStateOf(AuthDialogMode.NONE) }
    var librarySheet by remember { mutableStateOf(LibrarySheetMode.NONE) }
    var qualityMenuExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 1.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    BtTubeLogo(compact = true)
                    Text(
                        text = stringResource(R.string.nav_profile),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                if (state.userProfile.isSignedIn) {
                    TextButton(
                        onClick = onSignOut,
                        modifier = Modifier.testTag("profile_sign_out_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.Logout,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.sign_out))
                    }
                }
            }
        }

        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.TopCenter
        ) {
            LazyColumn(
                modifier = Modifier
                    .widthIn(max = 760.dp)
                    .fillMaxSize()
                    .testTag("profile_list"),
                contentPadding = PaddingValues(bottom = 28.dp)
            ) {
                // Profile Identity Card + Login/Signup Actions
                item(key = "profile_card") {
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(20.dp),
                        tonalElevation = 2.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(64.dp)
                                        .clip(CircleShape)
                                        .background(
                                            Brush.linearGradient(listOf(BtCrimson, BtCrimsonDark))
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = state.userProfile.avatarInitials,
                                        fontFamily = OutfitFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 22.sp,
                                        color = BtWhite
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = state.userProfile.name,
                                        style = MaterialTheme.typography.titleLarge,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.testTag("profile_user_name")
                                    )
                                    Text(
                                        text = state.userProfile.handle,
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = state.userProfile.bio,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            if (!state.userProfile.isSignedIn) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            authMode = if (authMode == AuthDialogMode.SIGN_IN) {
                                                AuthDialogMode.NONE
                                            } else {
                                                AuthDialogMode.SIGN_IN
                                            }
                                        },
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.primary
                                        ),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(48.dp)
                                            .testTag("profile_sign_in_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Rounded.Login,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(stringResource(R.string.sign_in))
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            authMode = if (authMode == AuthDialogMode.SIGN_UP) {
                                                AuthDialogMode.NONE
                                            } else {
                                                AuthDialogMode.SIGN_UP
                                            }
                                        },
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(48.dp)
                                            .testTag("profile_sign_up_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.PersonAdd,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(stringResource(R.string.create_account))
                                    }
                                }

                                AnimatedVisibility(visible = authMode != AuthDialogMode.NONE) {
                                    InlineAuthForm(
                                        mode = authMode,
                                        onCancel = { authMode = AuthDialogMode.NONE },
                                        onSwitchMode = {
                                            authMode = if (authMode == AuthDialogMode.SIGN_IN) {
                                                AuthDialogMode.SIGN_UP
                                            } else {
                                                AuthDialogMode.SIGN_IN
                                            }
                                        },
                                        onConfirm = { email, name ->
                                            onSignIn(email, name)
                                            authMode = AuthDialogMode.NONE
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // Watch History Shelf
                item(key = "watch_history_shelf") {
                    ProfileVideoShelf(
                        title = stringResource(R.string.watch_history),
                        icon = Icons.Outlined.History,
                        videos = state.watchHistoryVideos,
                        channelsById = state.channelsById,
                        emptyText = "Videos you watch will appear here.",
                        onSeeAll = { librarySheet = LibrarySheetMode.HISTORY },
                        onVideoClick = onVideoClick,
                        testTag = "watch_history_shelf"
                    )
                }

                // Liked Videos Shelf
                item(key = "liked_videos_shelf") {
                    ProfileVideoShelf(
                        title = stringResource(R.string.liked_videos),
                        icon = Icons.Outlined.ThumbUp,
                        videos = state.likedVideos,
                        channelsById = state.channelsById,
                        emptyText = "Tap Like on any video to build your favorites list.",
                        onSeeAll = { librarySheet = LibrarySheetMode.LIKED },
                        onVideoClick = onVideoClick,
                        testTag = "liked_videos_shelf"
                    )
                }

                // Watch Later Shelf
                item(key = "watch_later_shelf") {
                    ProfileVideoShelf(
                        title = stringResource(R.string.watch_later),
                        icon = Icons.Outlined.BookmarkBorder,
                        videos = state.watchLaterVideos,
                        channelsById = state.channelsById,
                        emptyText = "Save videos from the 3-dot menu or player.",
                        onSeeAll = { librarySheet = LibrarySheetMode.WATCH_LATER },
                        onVideoClick = onVideoClick,
                        testTag = "watch_later_shelf"
                    )
                }

                // My Phone Videos Shelf
                item(key = "device_videos_shelf") {
                    Column {
                        ProfileVideoShelf(
                            title = "My Phone Videos",
                            icon = Icons.Outlined.PhoneAndroid,
                            videos = state.deviceVideos,
                            channelsById = state.channelsById,
                            emptyText = "Add videos from your phone gallery or folder to watch offline in BT Tube.",
                            onSeeAll = null,
                            onVideoClick = onVideoClick,
                            testTag = "device_videos_shelf"
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = onScanAllMediaStoreVideos,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Scan All", maxLines = 1)
                            }
                            OutlinedButton(
                                onClick = onImportMultipleVideos,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Pick", maxLines = 1)
                            }
                            OutlinedButton(
                                onClick = onImportVideoFolder,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Folder", maxLines = 1)
                            }
                        }
                    }
                }

                // Your Uploads (if user published via Create)
                if (state.uploadedVideos.isNotEmpty()) {
                    item(key = "uploads_shelf") {
                        ProfileVideoShelf(
                            title = stringResource(R.string.your_videos),
                            icon = Icons.Outlined.VideoLibrary,
                            videos = state.uploadedVideos,
                            channelsById = state.channelsById,
                            emptyText = "",
                            onSeeAll = null,
                            onVideoClick = onVideoClick,
                            testTag = "uploads_shelf"
                        )
                    }
                }

                // Settings Card
                item(key = "settings_section") {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.settings_title),
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onBackground
                        )

                        Surface(
                            color = MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(18.dp),
                            tonalElevation = 1.dp
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                // Theme Selector
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Palette,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = "Appearance",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }

                                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                                        ThemeMode.entries.forEachIndexed { index, mode ->
                                            SegmentedButton(
                                                selected = state.settings.themeMode == mode,
                                                onClick = { onUpdateThemeMode(mode) },
                                                shape = SegmentedButtonDefaults.itemShape(
                                                    index = index,
                                                    count = ThemeMode.entries.size
                                                ),
                                                modifier = Modifier.testTag("theme_option_${mode.name.lowercase()}")
                                            ) {
                                                Text(mode.label)
                                            }
                                        }
                                    }
                                }

                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                                // Autoplay Toggle
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onToggleAutoplay() }
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.PlayCircle,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Column {
                                            Text(
                                                text = "Autoplay next video",
                                                style = MaterialTheme.typography.titleSmall,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = "Play Up Next automatically when a video ends",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    Switch(
                                        checked = state.settings.autoplayNext,
                                        onCheckedChange = { onToggleAutoplay() },
                                        modifier = Modifier.testTag("settings_autoplay_switch")
                                    )
                                }

                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                                // Notifications Toggle
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onToggleNotifications() }
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.NotificationsActive,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Column {
                                            Text(
                                                text = "Creator notifications",
                                                style = MaterialTheme.typography.titleSmall,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = "Receive alerts for new uploads and live streams",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    Switch(
                                        checked = state.settings.notificationsEnabled,
                                        onCheckedChange = { onToggleNotifications() },
                                        modifier = Modifier.testTag("settings_notifications_switch")
                                    )
                                }

                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                                // Stream on Mobile Data Toggle
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onToggleMobileData() }
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        modifier = Modifier.weight(1f),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.CellTower,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                        Column {
                                            Text(
                                                text = "Stream with mobile data",
                                                style = MaterialTheme.typography.titleSmall,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = "Connection: ${networkState.transport.displayLabel} • Works on 4G/5G & Wi-Fi",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    Switch(
                                        checked = state.settings.streamOnMobileData,
                                        onCheckedChange = { onToggleMobileData() },
                                        modifier = Modifier.testTag("settings_mobile_data_switch")
                                    )
                                }

                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                                // Playback Quality Selector
                                Box {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { qualityMenuExpanded = true }
                                            .padding(horizontal = 16.dp, vertical = 14.dp)
                                            .testTag("settings_quality_row"),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.HighQuality,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Column {
                                                Text(
                                                    text = "Default playback quality",
                                                    style = MaterialTheme.typography.titleSmall,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                Text(
                                                    text = state.settings.defaultQuality.label,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }
                                        Text(
                                            text = "Change",
                                            style = MaterialTheme.typography.labelLarge,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }

                                    DropdownMenu(
                                        expanded = qualityMenuExpanded,
                                        onDismissRequest = { qualityMenuExpanded = false }
                                    ) {
                                        PlaybackQuality.entries.forEach { quality ->
                                            DropdownMenuItem(
                                                text = { Text(quality.label) },
                                                onClick = {
                                                    qualityMenuExpanded = false
                                                    onUpdateQuality(quality)
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Library See-All Sheet (Watch History / Liked Videos / Watch Later)
    if (librarySheet != LibrarySheetMode.NONE) {
        val (title, items) = when (librarySheet) {
            LibrarySheetMode.HISTORY -> stringResource(R.string.watch_history) to state.watchHistoryVideos
            LibrarySheetMode.LIKED -> stringResource(R.string.liked_videos) to state.likedVideos
            LibrarySheetMode.WATCH_LATER -> stringResource(R.string.watch_later) to state.watchLaterVideos
            LibrarySheetMode.NONE -> "" to emptyList()
        }

        ModalBottomSheet(
            onDismissRequest = { librarySheet = LibrarySheetMode.NONE },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (librarySheet == LibrarySheetMode.HISTORY && items.isNotEmpty()) {
                        TextButton(onClick = onClearHistory) {
                            Text("Clear history")
                        }
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    items(items, key = { "lib_${it.id}" }) { video ->
                        CompactVideoRowCard(
                            video = video,
                            channel = state.channelsById[video.channelId],
                            onClick = {
                                librarySheet = LibrarySheetMode.NONE
                                onVideoClick(video)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileVideoShelf(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    videos: List<VideoItem>,
    channelsById: Map<String, Channel>,
    emptyText: String,
    onSeeAll: (() -> Unit)?,
    onVideoClick: (VideoItem) -> Unit,
    testTag: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
            if (onSeeAll != null && videos.isNotEmpty()) {
                TextButton(onClick = onSeeAll) {
                    Text("View all")
                }
            }
        }

        if (videos.isEmpty()) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            ) {
                Text(
                    text = emptyText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp)
                )
            }
        } else {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(videos, key = { "${testTag}_${it.id}" }) { video ->
                    val channel = channelsById[video.channelId]
                    Column(
                        modifier = Modifier
                            .width(172.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onVideoClick(video) }
                    ) {
                        VideoThumbnailSurface(
                            artwork = video.artwork,
                            contentDescription = video.title,
                            durationLabel = FormatUtils.formatDuration(video.durationSeconds),
                            isLive = video.isLive,
                            is4K = false,
                            modifier = Modifier.clip(RoundedCornerShape(12.dp))
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = video.title,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onBackground,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = channel?.name ?: "BT Creator",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun InlineAuthForm(
    mode: AuthDialogMode,
    onCancel: () -> Unit,
    onSwitchMode: () -> Unit,
    onConfirm: (email: String, name: String?) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    val isSignUp = mode == AuthDialogMode.SIGN_UP

    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = if (isSignUp) "Create BT Tube Account" else "Sign in to BT Tube",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (isSignUp) {
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        errorMsg = null
                    },
                    label = { Text("Display Name") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("auth_name_input")
                )
            }
            OutlinedTextField(
                value = email,
                onValueChange = {
                    email = it
                    errorMsg = null
                },
                label = { Text("Email") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("auth_email_input")
            )
            OutlinedTextField(
                value = password,
                onValueChange = {
                    password = it
                    errorMsg = null
                },
                label = { Text("Password") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("auth_password_input")
            )
            if (errorMsg != null) {
                Text(
                    text = errorMsg.orEmpty(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onSwitchMode) {
                    Text(
                        if (isSignUp) "Have an account? Sign In"
                        else "New? Create Account"
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = onCancel) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = {
                            when {
                                !email.contains("@") -> errorMsg = "Enter a valid email address"
                                password.length < 4 -> errorMsg = "Password must be at least 4 characters"
                                isSignUp && name.isBlank() -> errorMsg = "Enter your display name"
                                else -> onConfirm(email, if (isSignUp) name else null)
                            }
                        },
                        modifier = Modifier.testTag("auth_confirm_button")
                    ) {
                        Text(if (isSignUp) "Sign Up" else "Sign In")
                    }
                }
            }
        }
    }
}
