package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.rounded.AddToQueue
import androidx.compose.material.icons.rounded.CellTower
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Whatshot
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material.icons.rounded.WifiOff
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.UiState
import com.example.model.VideoCategory
import com.example.model.VideoItem
import com.example.ui.components.BtTubeLogo
import com.example.ui.components.EmptyStateView
import com.example.ui.components.ErrorStateView
import com.example.ui.components.FeedLoadingPlaceholder
import com.example.ui.components.TrendingVideoCard
import com.example.ui.components.VideoFeedCard
import com.example.ui.theme.BtCrimson
import com.example.ui.theme.BtWhite
import com.example.ui.viewmodel.HomeFeedData
import com.example.util.NetworkConnectionState
import com.example.util.NetworkTransport

@Composable
fun HomeScreen(
    homeState: UiState<HomeFeedData>,
    isImportingVideos: Boolean,
    networkState: NetworkConnectionState,
    streamOnMobileData: Boolean,
    onSelectCategory: (VideoCategory) -> Unit,
    onRefresh: () -> Unit,
    onRestoreHidden: () -> Unit,
    onSearchClick: () -> Unit,
    onNotificationsClick: () -> Unit,
    onScanAllMediaStoreVideos: () -> Unit,
    onImportMultipleVideos: () -> Unit,
    onImportVideoFolder: () -> Unit,
    onVideoClick: (VideoItem) -> Unit,
    onPlayAsAudio: (VideoItem) -> Unit,
    onChannelClick: (String) -> Unit,
    onToggleWatchLater: (String) -> Unit,
    onToggleSubscribe: (String) -> Unit,
    onShareVideo: (VideoItem) -> Unit,
    onHideVideo: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val unreadCount = (homeState as? UiState.Success)?.data?.unreadNotificationsCount ?: 0

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
    ) {
        // Top Toolbar with BT Tube Logo, Search, Notifications, Refresh
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 1.dp
        ) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        BtTubeLogo(modifier = Modifier.testTag("home_bt_logo"))

                        if (networkState.transport == NetworkTransport.MOBILE_DATA || !networkState.isConnected) {
                            val badgeIcon = when {
                                !networkState.isConnected -> Icons.Rounded.WifiOff
                                networkState.transport == NetworkTransport.MOBILE_DATA -> Icons.Rounded.CellTower
                                else -> Icons.Rounded.Wifi
                            }
                            val badgeText = when {
                                !networkState.isConnected -> "Offline"
                                streamOnMobileData -> "Mobile Data"
                                else -> "Data Paused"
                            }
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(999.dp),
                                modifier = Modifier.testTag("home_network_badge")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = badgeIcon,
                                        contentDescription = badgeText,
                                        tint = if (networkState.isConnected) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        },
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Text(
                                        text = badgeText,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        IconButton(
                            onClick = onRefresh,
                            modifier = Modifier.testTag("home_refresh_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Refresh,
                                contentDescription = "Refresh feed",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        IconButton(
                            onClick = onNotificationsClick,
                            modifier = Modifier.testTag("home_notifications_button")
                        ) {
                            BadgedBox(
                                badge = {
                                    if (unreadCount > 0) {
                                        Badge(
                                            containerColor = BtCrimson,
                                            contentColor = BtWhite
                                        ) {
                                            Text(text = unreadCount.toString())
                                        }
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Notifications,
                                    contentDescription = stringResource(R.string.cd_notifications),
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                        IconButton(
                            onClick = onSearchClick,
                            modifier = Modifier.testTag("home_search_button")
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Search,
                                contentDescription = stringResource(R.string.cd_search),
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                // Category Filter Chips
                val selectedCategory =
                    (homeState as? UiState.Success)?.data?.selectedCategory ?: VideoCategory.ALL

                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(VideoCategory.entries) { category ->
                        val selected = category == selectedCategory
                        FilterChip(
                            selected = selected,
                            onClick = { onSelectCategory(category) },
                            label = {
                                Text(
                                    text = category.label,
                                    style = MaterialTheme.typography.labelLarge
                                )
                            },
                            shape = RoundedCornerShape(999.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            border = null,
                            modifier = Modifier.testTag("category_chip_${category.name.lowercase()}")
                        )
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
            }
        }

        when (homeState) {
            is UiState.Loading -> {
                FeedLoadingPlaceholder(modifier = Modifier.fillMaxSize())
            }

            is UiState.Error -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    ErrorStateView(
                        message = homeState.message,
                        onRetry = onRefresh
                    )
                }
            }

            is UiState.Success -> {
                val data = homeState.data
                BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                    val isWideScreen = maxWidth >= 680.dp
                    val columns = if (isWideScreen) 2 else 1

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("home_feed_list"),
                        contentPadding = PaddingValues(bottom = 24.dp)
                    ) {
                        // Phone Video Library Importer Card
                        item(key = "device_video_import_banner") {
                            Surface(
                                color = MaterialTheme.colorScheme.surface,
                                shape = RoundedCornerShape(18.dp),
                                tonalElevation = 2.dp,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 4.dp)
                                    .testTag("home_import_phone_videos_card")
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = if (data.deviceVideosCount + data.deviceAudioCount > 0) {
                                                    "Device Library (${data.deviceVideosCount} videos • ${data.deviceAudioCount} audio)"
                                                } else {
                                                    "Your Device Videos & Audio"
                                                },
                                                style = MaterialTheme.typography.titleMedium,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = "Only videos & audio songs on your phone appear here. Tap headphones on any video to play audio in background.",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        if (isImportingVideos) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(22.dp),
                                                strokeWidth = 2.5.dp,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }

                                    Button(
                                        onClick = onScanAllMediaStoreVideos,
                                        enabled = !isImportingVideos,
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.primary
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("scan_all_videos_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.AddToQueue,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.size(8.dp))
                                        Text("Load All Phone Videos & Audio", maxLines = 1)
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        OutlinedButton(
                                            onClick = onImportMultipleVideos,
                                            enabled = !isImportingVideos,
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier
                                                .weight(1f)
                                                .testTag("import_select_videos_button")
                                        ) {
                                            Text("Pick Files", maxLines = 1)
                                        }

                                        OutlinedButton(
                                            onClick = onImportVideoFolder,
                                            enabled = !isImportingVideos,
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier
                                                .weight(1f)
                                                .testTag("import_folder_button")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Outlined.FolderOpen,
                                                contentDescription = null,
                                                modifier = Modifier.size(17.dp)
                                            )
                                            Spacer(modifier = Modifier.size(6.dp))
                                            Text("Add Folder", maxLines = 1)
                                        }
                                    }
                                }
                            }
                        }

                        // Trending Video Section (shown only if items exist)
                        if (data.selectedCategory == VideoCategory.ALL && data.trendingVideos.isNotEmpty()) {
                            item(key = "trending_header") {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Surface(
                                        color = MaterialTheme.colorScheme.primaryContainer,
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Whatshot,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier
                                                .padding(6.dp)
                                                .size(18.dp)
                                        )
                                    }
                                    Text(
                                        text = stringResource(R.string.trending_now),
                                        style = MaterialTheme.typography.titleLarge,
                                        color = MaterialTheme.colorScheme.onBackground
                                    )
                                }
                            }

                            item(key = "trending_carousel") {
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                                    modifier = Modifier.testTag("trending_carousel")
                                ) {
                                    items(data.trendingVideos, key = { "tr_${it.id}" }) { video ->
                                        TrendingVideoCard(
                                            video = video,
                                            channel = data.channelsById[video.channelId],
                                            onClick = { onVideoClick(video) }
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                            }
                        }

                        // Device Media Feed Header
                        item(key = "recommended_header") {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = when (data.selectedCategory) {
                                        VideoCategory.ALL -> "Device Videos & Audio"
                                        VideoCategory.VIDEOS -> "Device Videos"
                                        VideoCategory.AUDIO -> "Device Audio Songs"
                                        VideoCategory.SHORTS -> "Short Device Videos"
                                    },
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                Text(
                                    text = "${data.recommendedVideos.size} items",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        if (data.recommendedVideos.isEmpty()) {
                            item(key = "empty_feed") {
                                EmptyStateView(
                                    icon = Icons.Outlined.LibraryMusic,
                                    title = if (data.deviceVideosCount + data.deviceAudioCount == 0) {
                                        "No device media loaded yet"
                                    } else {
                                        "No items in ${data.selectedCategory.label}"
                                    },
                                    subtitle = if (data.deviceVideosCount + data.deviceAudioCount == 0) {
                                        "Tap \"Load All Phone Videos & Audio\" above to bring in all videos and songs from your device."
                                    } else {
                                        "Switch back to All or pick another folder to load more files."
                                    },
                                    actionLabel = if (data.deviceVideosCount + data.deviceAudioCount == 0) {
                                        "Load Device Media"
                                    } else {
                                        "Show All"
                                    },
                                    onAction = {
                                        if (data.deviceVideosCount + data.deviceAudioCount == 0) {
                                            onScanAllMediaStoreVideos()
                                        } else {
                                            onSelectCategory(VideoCategory.ALL)
                                            onRestoreHidden()
                                        }
                                    }
                                )
                            }
                        } else {
                            val chunkedRows = data.recommendedVideos.chunked(columns)
                            items(chunkedRows, key = { row -> row.joinToString("_") { it.id } }) { rowVideos ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    rowVideos.forEach { video ->
                                        val channel = data.channelsById[video.channelId]
                                        VideoFeedCard(
                                            video = video,
                                            channel = channel,
                                            isSavedWatchLater = video.id in data.watchLaterIds,
                                            isSubscribed = video.channelId in data.subscribedChannelIds,
                                            onVideoClick = { onVideoClick(video) },
                                            onPlayAsAudio = { onPlayAsAudio(video) },
                                            onChannelClick = { onChannelClick(video.channelId) },
                                            onToggleWatchLater = { onToggleWatchLater(video.id) },
                                            onToggleSubscribe = { onToggleSubscribe(video.channelId) },
                                            onShare = { onShareVideo(video) },
                                            onHideVideo = { onHideVideo(video.id) },
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                    if (rowVideos.size < columns) {
                                        Spacer(modifier = Modifier.weight(1f))
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
