package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.BookmarkAdd
import androidx.compose.material.icons.outlined.BookmarkAdded
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.ThumbDown
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Forward10
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.FullscreenExit
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Replay10
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.ThumbDown
import androidx.compose.material.icons.rounded.ThumbUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.VideoItem
import com.example.ui.components.ChannelAvatar
import com.example.ui.components.CommentsBottomSheet
import com.example.ui.components.CompactVideoRowCard
import com.example.ui.components.VideoThumbnailSurface
import com.example.ui.theme.BtCrimson
import com.example.ui.theme.BtCrimsonBright
import com.example.ui.theme.BtVerifiedBlue
import com.example.ui.theme.BtWhite
import com.example.ui.viewmodel.PlayerUiState
import com.example.util.FormatUtils
import kotlinx.coroutines.delay
import kotlin.math.sin

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun VideoPlayerScreen(
    state: PlayerUiState,
    onBack: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onSeekTo: (Float) -> Unit,
    onSeekBy: (Float) -> Unit,
    onAdvancePlayback: (Float) -> Unit,
    onSelectSpeed: (Float) -> Unit,
    onToggleFullscreen: () -> Unit,
    onExitFullscreen: () -> Unit,
    onToggleSubscribe: (String) -> Unit,
    onToggleLike: (String) -> Unit,
    onToggleDislike: (String) -> Unit,
    onToggleWatchLater: (String) -> Unit,
    onShareVideo: (VideoItem) -> Unit,
    onAddComment: (String, String) -> Unit,
    onSelectRelatedVideo: (VideoItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val video = state.video ?: return
    val channel = state.channel

    var controlsVisible by remember { mutableStateOf(true) }
    var descriptionExpanded by remember { mutableStateOf(false) }
    var showCommentsSheet by remember { mutableStateOf(false) }
    var seekFeedbackText by remember { mutableStateOf<String?>(null) }

    BackHandler(enabled = state.isFullscreen) {
        onExitFullscreen()
    }

    // Real-time playback ticker scaled by playbackSpeed
    LaunchedEffect(state.isPlaying, state.playbackSpeed, video.id) {
        while (state.isPlaying) {
            delay(100L)
            onAdvancePlayback(0.1f * state.playbackSpeed)
        }
    }

    // Auto-hide seek feedback pill
    LaunchedEffect(seekFeedbackText) {
        if (seekFeedbackText != null) {
            delay(750L)
            seekFeedbackText = null
        }
    }

    val playerSurface: @Composable (Modifier) -> Unit = { playerMod ->
        VideoPlaybackStage(
            video = video,
            isPlaying = state.isPlaying,
            isFullscreen = state.isFullscreen,
            currentPositionSeconds = state.currentPositionSeconds,
            playbackSpeed = state.playbackSpeed,
            controlsVisible = controlsVisible,
            seekFeedbackText = seekFeedbackText,
            onToggleControls = { controlsVisible = !controlsVisible },
            onDoubleTapLeft = {
                onSeekBy(-10f)
                seekFeedbackText = "-10s"
            },
            onDoubleTapRight = {
                onSeekBy(10f)
                seekFeedbackText = "+10s"
            },
            onBack = {
                if (state.isFullscreen) onExitFullscreen() else onBack()
            },
            onTogglePlayPause = onTogglePlayPause,
            onSeekTo = onSeekTo,
            onSelectSpeed = onSelectSpeed,
            onToggleFullscreen = onToggleFullscreen,
            modifier = playerMod
        )
    }

    if (state.isFullscreen) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            playerSurface(Modifier.fillMaxSize())
        }
        return
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // Video Playback Area (16:9)
        playerSurface(
            Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
        )

        // Scrollable Metadata, Actions, Comments & Related Videos
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("player_details_list"),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            item(key = "title_and_meta") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { descriptionExpanded = !descriptionExpanded }
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                ) {
                    Text(
                        text = video.title,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "${FormatUtils.formatViews(video.viewsCount, video.isLive)} • ${video.uploadDate}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Icon(
                            imageVector = if (descriptionExpanded) {
                                Icons.Rounded.KeyboardArrowUp
                            } else {
                                Icons.Rounded.KeyboardArrowDown
                            },
                            contentDescription = "Toggle description",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    AnimatedVisibility(visible = descriptionExpanded) {
                        Column(modifier = Modifier.padding(top = 12.dp)) {
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text(
                                        text = video.description,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    if (video.tags.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(10.dp))
                                        FlowRow(
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            verticalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            video.tags.forEach { tag ->
                                                Surface(
                                                    color = MaterialTheme.colorScheme.surface,
                                                    shape = RoundedCornerShape(8.dp)
                                                ) {
                                                    Text(
                                                        text = "#$tag",
                                                        style = MaterialTheme.typography.labelMedium,
                                                        color = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
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

            // Channel Information + Subscribe Button
            item(key = "channel_row") {
                if (channel != null) {
                    val subscriberDisplay = channel.baseSubscriberCount + if (state.isSubscribed) 1 else 0
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            ChannelAvatar(channel = channel, size = 44.dp)
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = channel.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onBackground,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (channel.isVerified) {
                                        Icon(
                                            imageVector = Icons.Rounded.CheckCircle,
                                            contentDescription = "Verified",
                                            tint = BtVerifiedBlue,
                                            modifier = Modifier.size(15.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = "${FormatUtils.formatCompactCount(subscriberDisplay)} subscribers",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Button(
                            onClick = { onToggleSubscribe(channel.id) },
                            shape = RoundedCornerShape(999.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (state.isSubscribed) {
                                    MaterialTheme.colorScheme.surfaceVariant
                                } else {
                                    MaterialTheme.colorScheme.primary
                                },
                                contentColor = if (state.isSubscribed) {
                                    MaterialTheme.colorScheme.onSurface
                                } else {
                                    MaterialTheme.colorScheme.onPrimary
                                }
                            ),
                            modifier = Modifier.testTag("player_subscribe_button")
                        ) {
                            if (state.isSubscribed) {
                                Icon(
                                    imageVector = Icons.Rounded.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                            }
                            Text(
                                text = if (state.isSubscribed) {
                                    stringResource(R.string.subscribed)
                                } else {
                                    stringResource(R.string.subscribe)
                                }
                            )
                        }
                    }
                }
            }

            // Action Strip: Like, Dislike, Share, Save
            item(key = "action_pills") {
                val likeCount = video.baseLikes + if (state.isLiked) 1 else 0
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        ActionPillButton(
                            icon = if (state.isLiked) Icons.Rounded.ThumbUp else Icons.Outlined.ThumbUp,
                            label = FormatUtils.formatCompactCount(likeCount),
                            selected = state.isLiked,
                            onClick = { onToggleLike(video.id) },
                            testTag = "player_like_button"
                        )
                    }
                    item {
                        ActionPillButton(
                            icon = if (state.isDisliked) Icons.Rounded.ThumbDown else Icons.Outlined.ThumbDown,
                            label = if (state.isDisliked) "Disliked" else "Dislike",
                            selected = state.isDisliked,
                            onClick = { onToggleDislike(video.id) },
                            testTag = "player_dislike_button"
                        )
                    }
                    item {
                        ActionPillButton(
                            icon = Icons.Outlined.Share,
                            label = "Share",
                            selected = false,
                            onClick = { onShareVideo(video) },
                            testTag = "player_share_button"
                        )
                    }
                    item {
                        ActionPillButton(
                            icon = if (state.isWatchLater) {
                                Icons.Outlined.BookmarkAdded
                            } else {
                                Icons.Outlined.BookmarkAdd
                            },
                            label = if (state.isWatchLater) "Saved" else "Save",
                            selected = state.isWatchLater,
                            onClick = { onToggleWatchLater(video.id) },
                            testTag = "player_save_button"
                        )
                    }
                }
            }

            // Comments Preview Card
            item(key = "comments_preview") {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { showCommentsSheet = true }
                        .testTag("player_comments_card")
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Comments",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = video.comments.size.toString(),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        val firstComment = video.comments.firstOrNull()
                        Text(
                            text = if (firstComment != null) {
                                "${firstComment.authorName}: ${firstComment.text}"
                            } else {
                                "Tap to add the first comment..."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }

            // Related Videos Header
            item(key = "related_header") {
                Text(
                    text = stringResource(R.string.related_videos),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp)
                )
            }

            // Related Videos List
            items(state.relatedVideos, key = { "rel_${it.id}" }) { related ->
                CompactVideoRowCard(
                    video = related,
                    channel = state.channelsById[related.channelId],
                    onClick = { onSelectRelatedVideo(related) },
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }
    }

    if (showCommentsSheet) {
        CommentsBottomSheet(
            title = "Comments",
            comments = video.comments,
            onDismiss = { showCommentsSheet = false },
            onAddComment = { text -> onAddComment(video.id, text) }
        )
    }
}

@Composable
private fun VideoPlaybackStage(
    video: VideoItem,
    isPlaying: Boolean,
    isFullscreen: Boolean,
    currentPositionSeconds: Float,
    playbackSpeed: Float,
    controlsVisible: Boolean,
    seekFeedbackText: String?,
    onToggleControls: () -> Unit,
    onDoubleTapLeft: () -> Unit,
    onDoubleTapRight: () -> Unit,
    onBack: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onSeekTo: (Float) -> Unit,
    onSelectSpeed: (Float) -> Unit,
    onToggleFullscreen: () -> Unit,
    modifier: Modifier = Modifier
) {
    var speedMenuExpanded by remember { mutableStateOf(false) }
    val duration = video.durationSeconds.toFloat().coerceAtLeast(1f)
    val speedOptions = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f)

    Box(
        modifier = modifier
            .background(Color.Black)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onToggleControls() },
                    onDoubleTap = { offset ->
                        if (offset.x < size.width / 2f) {
                            onDoubleTapLeft()
                        } else {
                            onDoubleTapRight()
                        }
                    }
                )
            }
    ) {
        // Subtle live zoom motion during active playback
        val motionScale = if (isPlaying) {
            1f + 0.03f * sin(currentPositionSeconds * 0.45f)
        } else {
            1f
        }

        VideoThumbnailSurface(
            artwork = video.artwork,
            contentDescription = video.title,
            aspectRatio = null,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                    scaleX = motionScale,
                    scaleY = motionScale
                )
        )

        // Live animated audio/frame equalizer bar strip at very bottom when controls are hidden
        if (isPlaying && !controlsVisible) {
            Canvas(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(3.dp)
            ) {
                drawRect(color = Color.White.copy(alpha = 0.25f))
                val progress = (currentPositionSeconds / duration).coerceIn(0f, 1f)
                drawRect(
                    color = BtCrimsonBright,
                    size = Size(size.width * progress, size.height)
                )
            }
        }

        // Double-tap seek feedback badge
        AnimatedVisibility(
            visible = seekFeedbackText != null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Surface(
                color = Color.Black.copy(alpha = 0.75f),
                shape = RoundedCornerShape(999.dp)
            ) {
                Text(
                    text = seekFeedbackText.orEmpty(),
                    style = MaterialTheme.typography.titleMedium,
                    color = BtWhite,
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp)
                )
            }
        }

        // Controls Overlay
        AnimatedVisibility(
            visible = controlsVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.65f),
                                Color.Black.copy(alpha = 0.28f),
                                Color.Black.copy(alpha = 0.78f)
                            )
                        )
                    )
            ) {
                // Top Player Bar: Back + Title + Speed Selector
                Row(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier.testTag("player_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                contentDescription = stringResource(R.string.cd_back),
                                tint = BtWhite
                            )
                        }
                        if (isFullscreen) {
                            Text(
                                text = video.title,
                                style = MaterialTheme.typography.titleSmall,
                                color = BtWhite,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // Playback Speed Selector
                    Box {
                        Surface(
                            color = Color.Black.copy(alpha = 0.55f),
                            shape = RoundedCornerShape(999.dp),
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .clickable { speedMenuExpanded = true }
                                .testTag("playback_speed_button")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Speed,
                                    contentDescription = "Playback speed",
                                    tint = BtWhite,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "${playbackSpeed}x",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = BtWhite,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = speedMenuExpanded,
                            onDismissRequest = { speedMenuExpanded = false }
                        ) {
                            speedOptions.forEach { speed ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = if (speed == 1.0f) "1.0x (Normal)" else "${speed}x",
                                            fontWeight = if (speed == playbackSpeed) FontWeight.Bold else FontWeight.Normal,
                                            color = if (speed == playbackSpeed) BtCrimson else MaterialTheme.colorScheme.onSurface
                                        )
                                    },
                                    onClick = {
                                        speedMenuExpanded = false
                                        onSelectSpeed(speed)
                                    }
                                )
                            }
                        }
                    }
                }

                // Center Transport Controls: -10s, Play/Pause, +10s
                Row(
                    modifier = Modifier.align(Alignment.Center),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    IconButton(
                        onClick = { onDoubleTapLeft() },
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.45f))
                            .testTag("player_replay_10")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Replay10,
                            contentDescription = "Rewind 10 seconds",
                            tint = BtWhite,
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    IconButton(
                        onClick = onTogglePlayPause,
                        modifier = Modifier
                            .size(60.dp)
                            .clip(CircleShape)
                            .background(BtCrimson.copy(alpha = 0.92f))
                            .testTag("player_play_pause_button")
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            contentDescription = stringResource(R.string.cd_play_pause),
                            tint = BtWhite,
                            modifier = Modifier.size(34.dp)
                        )
                    }

                    IconButton(
                        onClick = { onDoubleTapRight() },
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.45f))
                            .testTag("player_forward_10")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Forward10,
                            contentDescription = "Forward 10 seconds",
                            tint = BtWhite,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }

                // Bottom Bar: Timestamps + Seek Bar + Fullscreen Toggle
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "${FormatUtils.formatDuration(currentPositionSeconds.toInt())} / ${FormatUtils.formatDuration(video.durationSeconds)}",
                                style = MaterialTheme.typography.labelMedium,
                                color = BtWhite,
                                modifier = Modifier.testTag("player_time_label")
                            )
                            if (isPlaying) {
                                // Subtle live equalizer indicator
                                Canvas(modifier = Modifier.size(width = 16.dp, height = 10.dp)) {
                                    for (bar in 0..3) {
                                        val hFactor = 0.35f + 0.65f * kotlin.math.abs(
                                            sin(currentPositionSeconds * 5f + bar * 1.1f)
                                        )
                                        val barH = size.height * hFactor
                                        drawRoundRect(
                                            color = BtCrimsonBright,
                                            topLeft = Offset(bar * 4.dp.toPx(), size.height - barH),
                                            size = Size(2.5.dp.toPx(), barH),
                                            cornerRadius = CornerRadius(2f, 2f)
                                        )
                                    }
                                }
                            }
                        }

                        IconButton(
                            onClick = onToggleFullscreen,
                            modifier = Modifier.testTag("player_fullscreen_button")
                        ) {
                            Icon(
                                imageVector = if (isFullscreen) {
                                    Icons.Rounded.FullscreenExit
                                } else {
                                    Icons.Rounded.Fullscreen
                                },
                                contentDescription = stringResource(R.string.cd_fullscreen),
                                tint = BtWhite
                            )
                        }
                    }

                    Slider(
                        value = currentPositionSeconds.coerceIn(0f, duration),
                        onValueChange = onSeekTo,
                        valueRange = 0f..duration,
                        colors = SliderDefaults.colors(
                            thumbColor = BtCrimsonBright,
                            activeTrackColor = BtCrimsonBright,
                            inactiveTrackColor = Color.White.copy(alpha = 0.35f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(28.dp)
                            .testTag("player_seek_bar")
                    )
                }
            }
        }
    }
}

@Composable
private fun ActionPillButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    Surface(
        color = if (selected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        },
        shape = RoundedCornerShape(999.dp),
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .clickable(onClick = onClick)
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (selected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = if (selected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            )
        }
    }
}
