package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BookmarkAdd
import androidx.compose.material.icons.outlined.BookmarkAdded
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Whatshot
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.Channel
import com.example.model.VideoItem
import com.example.ui.theme.BtCrimson
import com.example.ui.theme.BtVerifiedBlue
import com.example.ui.theme.BtWhite
import com.example.util.FormatUtils

@Composable
fun TrendingVideoCard(
    video: VideoItem,
    channel: Channel?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .width(300.dp)
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .testTag("trending_card_${video.id}"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box {
            VideoThumbnailSurface(
                artwork = video.artwork,
                contentDescription = video.title,
                durationLabel = FormatUtils.formatDuration(video.durationSeconds),
                isLive = video.isLive,
                is4K = video.is4K
            )

            // Trending Rank Badge
            Surface(
                color = BtCrimson,
                shape = RoundedCornerShape(999.dp),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(10.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Whatshot,
                        contentDescription = null,
                        tint = BtWhite,
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = "#${video.trendingRank ?: 1} TRENDING",
                        style = MaterialTheme.typography.labelSmall,
                        color = BtWhite,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.Top
        ) {
            if (channel != null) {
                ChannelAvatar(
                    channel = channel,
                    size = 36.dp,
                    showVerifiedBadge = true
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = video.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = buildString {
                        append(channel?.name ?: "BT Tube")
                        append(" • ")
                        append(FormatUtils.formatViews(video.viewsCount, video.isLive))
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun VideoFeedCard(
    video: VideoItem,
    channel: Channel?,
    isSavedWatchLater: Boolean,
    isSubscribed: Boolean,
    onVideoClick: () -> Unit,
    onChannelClick: () -> Unit,
    onToggleWatchLater: () -> Unit,
    onToggleSubscribe: () -> Unit,
    onShare: () -> Unit,
    onHideVideo: () -> Unit,
    onPlayAsAudio: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onVideoClick)
            .testTag("video_card_${video.id}")
    ) {
        VideoThumbnailSurface(
            artwork = video.artwork,
            contentDescription = video.title,
            durationLabel = FormatUtils.formatDuration(video.durationSeconds),
            isLive = video.isLive,
            is4K = video.is4K,
            modifier = Modifier.clip(RoundedCornerShape(16.dp))
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 4.dp, end = 0.dp, top = 12.dp, bottom = 8.dp),
            verticalAlignment = Alignment.Top
        ) {
            if (channel != null) {
                Box(
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .clip(RoundedCornerShape(24.dp))
                        .clickable(onClick = onChannelClick)
                        .padding(end = 10.dp)
                ) {
                    ChannelAvatar(
                        channel = channel,
                        size = 40.dp,
                        showVerifiedBadge = false
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = video.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = channel?.name ?: "BT Creator",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (channel?.isVerified == true) {
                        Icon(
                            imageVector = Icons.Rounded.CheckCircle,
                            contentDescription = "Verified",
                            tint = BtVerifiedBlue,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                    val qualitySuffix = if (video.qualityLabel.isNotBlank()) " • ${video.qualityLabel}" else ""
                    Text(
                        text = " • ${FormatUtils.formatViews(video.viewsCount, video.isLive)}$qualitySuffix • ${video.uploadDate}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (onPlayAsAudio != null && video.videoUri != null) {
                IconButton(
                    onClick = onPlayAsAudio,
                    modifier = Modifier.testTag("play_audio_${video.id}")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Headphones,
                        contentDescription = "Play as audio",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Box {
                IconButton(
                    onClick = { menuExpanded = true },
                    modifier = Modifier.testTag("video_menu_${video.id}")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.MoreVert,
                        contentDescription = stringResource(R.string.cd_more_options),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false }
                ) {
                    if (onPlayAsAudio != null && video.videoUri != null) {
                        DropdownMenuItem(
                            text = { Text("Play as Audio (Background)") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Rounded.Headphones,
                                    contentDescription = null
                                )
                            },
                            onClick = {
                                menuExpanded = false
                                onPlayAsAudio()
                            }
                        )
                    }
                    DropdownMenuItem(
                        text = {
                            Text(
                                if (isSavedWatchLater) "Remove from Watch Later"
                                else "Save to Watch Later"
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = if (isSavedWatchLater) {
                                    Icons.Outlined.BookmarkAdded
                                } else {
                                    Icons.Outlined.BookmarkAdd
                                },
                                contentDescription = null
                            )
                        },
                        onClick = {
                            menuExpanded = false
                            onToggleWatchLater()
                        }
                    )
                    DropdownMenuItem(
                        text = {
                            Text(if (isSubscribed) "Unsubscribe" else "Subscribe to channel")
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Outlined.NotificationsActive,
                                contentDescription = null
                            )
                        },
                        onClick = {
                            menuExpanded = false
                            onToggleSubscribe()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Share video") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Outlined.Share,
                                contentDescription = null
                            )
                        },
                        onClick = {
                            menuExpanded = false
                            onShare()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Not interested") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Outlined.VisibilityOff,
                                contentDescription = null
                            )
                        },
                        onClick = {
                            menuExpanded = false
                            onHideVideo()
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun CompactVideoRowCard(
    video: VideoItem,
    channel: Channel?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp)
            .testTag("compact_video_${video.id}"),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        VideoThumbnailSurface(
            artwork = video.artwork,
            contentDescription = video.title,
            durationLabel = FormatUtils.formatDuration(video.durationSeconds),
            isLive = video.isLive,
            is4K = false,
            aspectRatio = null,
            modifier = Modifier
                .width(160.dp)
                .height(92.dp)
                .clip(RoundedCornerShape(12.dp))
        )

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
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
            val qualitySuffix = if (video.qualityLabel.isNotBlank()) " • ${video.qualityLabel}" else ""
            Text(
                text = "${FormatUtils.formatViews(video.viewsCount, video.isLive)}$qualitySuffix • ${video.uploadDate}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
