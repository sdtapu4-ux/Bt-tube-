package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Subscriptions
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.VideoItem
import com.example.ui.components.BtTubeLogo
import com.example.ui.components.ChannelAvatar
import com.example.ui.components.EmptyStateView
import com.example.ui.components.VideoFeedCard
import com.example.ui.theme.BtCrimson
import com.example.ui.theme.BtWhite
import com.example.ui.viewmodel.SubscriptionsUiState
import com.example.util.FormatUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscriptionsScreen(
    state: SubscriptionsUiState,
    onSelectChannel: (String?) -> Unit,
    onToggleSubscribe: (String) -> Unit,
    onVideoClick: (VideoItem) -> Unit,
    onToggleWatchLater: (String) -> Unit,
    onShareVideo: (VideoItem) -> Unit,
    onSearchClick: () -> Unit,
    onNotificationsClick: () -> Unit,
    onExploreHome: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showManageSheet by remember { mutableStateOf(false) }

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
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        BtTubeLogo(compact = true)
                        Text(
                            text = stringResource(R.string.nav_subscriptions),
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TextButton(
                            onClick = { showManageSheet = true },
                            modifier = Modifier.testTag("manage_subscriptions_button")
                        ) {
                            Text("Manage")
                        }
                        IconButton(onClick = onNotificationsClick) {
                            BadgedBox(
                                badge = {
                                    if (state.unreadNotificationsCount > 0) {
                                        Badge(
                                            containerColor = BtCrimson,
                                            contentColor = BtWhite
                                        ) {
                                            Text(state.unreadNotificationsCount.toString())
                                        }
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Notifications,
                                    contentDescription = stringResource(R.string.cd_notifications)
                                )
                            }
                        }
                        IconButton(onClick = onSearchClick) {
                            Icon(
                                imageVector = Icons.Outlined.Search,
                                contentDescription = stringResource(R.string.cd_search)
                            )
                        }
                    }
                }

                if (state.subscribedChannels.isNotEmpty()) {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        item(key = "all_channels_pill") {
                            val allSelected = state.selectedChannelId == null
                            Surface(
                                color = if (allSelected) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant
                                },
                                shape = RoundedCornerShape(999.dp),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(999.dp))
                                    .clickable { onSelectChannel(null) }
                                    .testTag("sub_filter_all")
                            ) {
                                Text(
                                    text = "All",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (allSelected) {
                                        MaterialTheme.colorScheme.onPrimary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                                )
                            }
                        }

                        items(state.subscribedChannels, key = { it.id }) { channel ->
                            val isSelected = state.selectedChannelId == channel.id
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .width(66.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        onSelectChannel(if (isSelected) null else channel.id)
                                    }
                                    .padding(vertical = 2.dp)
                                    .testTag("sub_channel_${channel.id}")
                            ) {
                                ChannelAvatar(
                                    channel = channel,
                                    size = 48.dp,
                                    hasActiveRing = isSelected
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = channel.name.split(" ").first(),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurface
                                    },
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
            }
        }

        if (state.subscribedChannels.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                EmptyStateView(
                    icon = Icons.Outlined.Subscriptions,
                    title = "No subscriptions yet",
                    subtitle = "Subscribe to your favorite BT Tube creators to see their latest uploads here.",
                    actionLabel = "Explore Channels",
                    onAction = { showManageSheet = true }
                )
            }
        } else if (state.feedVideos.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                EmptyStateView(
                    icon = Icons.Outlined.Subscriptions,
                    title = "No recent uploads",
                    subtitle = "Switch back to All channels or explore the Home feed.",
                    actionLabel = "Back to Home",
                    onAction = onExploreHome
                )
            }
        } else {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val columns = if (maxWidth >= 680.dp) 2 else 1
                val rows = state.feedVideos.chunked(columns)
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    items(rows, key = { row -> row.joinToString("_") { it.id } }) { rowVideos ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            rowVideos.forEach { video ->
                                VideoFeedCard(
                                    video = video,
                                    channel = state.channelsById[video.channelId],
                                    isSavedWatchLater = video.id in state.watchLaterIds,
                                    isSubscribed = true,
                                    onVideoClick = { onVideoClick(video) },
                                    onChannelClick = { onSelectChannel(video.channelId) },
                                    onToggleWatchLater = { onToggleWatchLater(video.id) },
                                    onToggleSubscribe = { onToggleSubscribe(video.channelId) },
                                    onShare = { onShareVideo(video) },
                                    onHideVideo = {},
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

    if (showManageSheet) {
        val subIds = state.subscribedChannels.map { it.id }.toSet()
        ModalBottomSheet(
            onDismissRequest = { showManageSheet = false },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp)
            ) {
                Text(
                    text = "Manage Subscriptions",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                    items(state.allChannels, key = { "manage_${it.id}" }) { channel ->
                        val isSub = channel.id in subIds
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            ChannelAvatar(channel = channel, size = 44.dp, showVerifiedBadge = true)
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = channel.name,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${FormatUtils.formatCompactCount(channel.baseSubscriberCount)} subscribers",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Button(
                                onClick = { onToggleSubscribe(channel.id) },
                                shape = RoundedCornerShape(999.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isSub) {
                                        MaterialTheme.colorScheme.surfaceVariant
                                    } else {
                                        MaterialTheme.colorScheme.primary
                                    },
                                    contentColor = if (isSub) {
                                        MaterialTheme.colorScheme.onSurface
                                    } else {
                                        MaterialTheme.colorScheme.onPrimary
                                    }
                                )
                            ) {
                                Text(if (isSub) "Subscribed" else "Subscribe")
                            }
                        }
                    }
                }
            }
        }
    }
}
