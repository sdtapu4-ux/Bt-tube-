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
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Whatshot
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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

@Composable
fun HomeScreen(
    homeState: UiState<HomeFeedData>,
    onSelectCategory: (VideoCategory) -> Unit,
    onRefresh: () -> Unit,
    onRestoreHidden: () -> Unit,
    onSearchClick: () -> Unit,
    onNotificationsClick: () -> Unit,
    onVideoClick: (VideoItem) -> Unit,
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
                    BtTubeLogo(modifier = Modifier.testTag("home_bt_logo"))

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
                        // Trending Video Section
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

                        // Recommended Section Header
                        item(key = "recommended_header") {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = if (data.selectedCategory == VideoCategory.ALL) {
                                        stringResource(R.string.recommended_for_you)
                                    } else {
                                        "${data.selectedCategory.label} Videos"
                                    },
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                Text(
                                    text = "${data.recommendedVideos.size} videos",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        if (data.recommendedVideos.isEmpty()) {
                            item(key = "empty_feed") {
                                EmptyStateView(
                                    icon = Icons.Outlined.VideoLibrary,
                                    title = "No videos in ${data.selectedCategory.label}",
                                    subtitle = "Try another category or restore hidden recommendations.",
                                    actionLabel = "Show All Videos",
                                    onAction = {
                                        onSelectCategory(VideoCategory.ALL)
                                        onRestoreHidden()
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
