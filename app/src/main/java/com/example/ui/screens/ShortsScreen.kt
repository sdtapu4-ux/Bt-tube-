package com.example.ui.screens

import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.widget.VideoView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Comment
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.R
import com.example.model.Channel
import com.example.model.ShortItem
import com.example.ui.components.ChannelAvatar
import com.example.ui.components.CommentsBottomSheet
import com.example.ui.components.VideoThumbnailSurface
import com.example.ui.theme.BtCrimson
import com.example.ui.theme.BtCrimsonBright
import com.example.ui.theme.BtWhite
import com.example.ui.theme.OutfitFontFamily
import com.example.util.FormatUtils
import com.example.util.VideoAudioHelper
import kotlinx.coroutines.delay
import kotlin.math.sin

@Composable
fun ShortsScreen(
    shorts: List<ShortItem>,
    channelsById: Map<String, Channel>,
    likedShortIds: Set<String>,
    subscribedChannelIds: Set<String>,
    onToggleLike: (String) -> Unit,
    onAddComment: (String, String) -> Unit,
    onToggleFollow: (String) -> Unit,
    onShareShort: (ShortItem) -> Unit,
    modifier: Modifier = Modifier
) {
    if (shorts.isEmpty()) return

    val pagerState = rememberPagerState(pageCount = { shorts.size })
    var activeCommentShort by remember { mutableStateOf<ShortItem?>(null) }
    var isMuted by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        VerticalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                .testTag("shorts_vertical_pager")
        ) { page ->
            val short = shorts[page]
            val channel = channelsById[short.channelId]
            val isCurrentPage = pagerState.currentPage == page
            val isLiked = short.id in likedShortIds
            val isFollowing = short.channelId in subscribedChannelIds

            ShortPageItem(
                short = short,
                channel = channel,
                isActive = isCurrentPage,
                isLiked = isLiked,
                isFollowing = isFollowing,
                isMuted = isMuted,
                onToggleMute = { isMuted = !isMuted },
                onToggleLike = { onToggleLike(short.id) },
                onOpenComments = { activeCommentShort = short },
                onShare = { onShareShort(short) },
                onToggleFollow = { onToggleFollow(short.channelId) }
            )
        }

        // Top Shorts Brand Header
        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Surface(
                color = Color.Black.copy(alpha = 0.45f),
                shape = RoundedCornerShape(999.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(BtCrimsonBright)
                    )
                    Text(
                        text = "BT Shorts",
                        fontFamily = OutfitFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = BtWhite
                    )
                }
            }

            IconButton(
                onClick = { isMuted = !isMuted },
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.45f))
                    .testTag("shorts_mute_button")
            ) {
                Icon(
                    imageVector = if (isMuted) {
                        Icons.AutoMirrored.Rounded.VolumeOff
                    } else {
                        Icons.AutoMirrored.Rounded.VolumeUp
                    },
                    contentDescription = if (isMuted) "Unmute" else "Mute",
                    tint = BtWhite
                )
            }
        }
    }

    activeCommentShort?.let { target ->
        val freshShort = shorts.find { it.id == target.id } ?: target
        CommentsBottomSheet(
            title = "Short Comments",
            comments = freshShort.comments,
            onDismiss = { activeCommentShort = null },
            onAddComment = { text -> onAddComment(freshShort.id, text) }
        )
    }
}

@Composable
private fun ShortPageItem(
    short: ShortItem,
    channel: Channel?,
    isActive: Boolean,
    isLiked: Boolean,
    isFollowing: Boolean,
    isMuted: Boolean,
    onToggleMute: () -> Unit,
    onToggleLike: () -> Unit,
    onOpenComments: () -> Unit,
    onShare: () -> Unit,
    onToggleFollow: () -> Unit
) {
    var isPlaying by remember(short.id) { mutableStateOf(true) }
    var elapsed by remember(short.id) { mutableFloatStateOf(0f) }
    val context = LocalContext.current

    DisposableEffect(isActive, isPlaying, isMuted, short.id) {
        val abandonFocus = if (isActive && isPlaying && !isMuted) {
            VideoAudioHelper.requestMediaAudioFocus(context)
        } else {
            {}
        }
        onDispose {
            abandonFocus()
        }
    }

    LaunchedEffect(isActive, isPlaying, isMuted, short.id, short.videoUri) {
        if (short.videoUri == null && isActive && isPlaying && !isMuted) {
            VideoAudioHelper.playAmbientSoundtrack(
                seedKey = short.id,
                playbackSpeed = 1.0f
            )
        }
    }

    LaunchedEffect(isActive, isPlaying) {
        if (!isActive) {
            elapsed = 0f
            return@LaunchedEffect
        }
        while (isPlaying) {
            delay(80L)
            elapsed = (elapsed + 0.08f) % short.durationSeconds.toFloat().coerceAtLeast(10f)
        }
    }

    val likeScale by animateFloatAsState(
        targetValue = if (isLiked) 1.18f else 1.0f,
        animationSpec = spring(dampingRatio = 0.45f, stiffness = 500f),
        label = "short_like_scale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable { isPlaying = !isPlaying }
            .testTag("short_page_${short.id}")
    ) {
        if (short.videoUri != null) {
            ShortLocalVideoStage(
                uriString = short.videoUri,
                shouldPlay = isActive && isPlaying,
                isMuted = isMuted,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            val motionScale = if (isActive && isPlaying) {
                1.04f + 0.03f * sin(elapsed * 0.6f)
            } else {
                1.02f
            }

            VideoThumbnailSurface(
                artwork = short.artwork,
                contentDescription = short.title,
                aspectRatio = null,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(
                        scaleX = motionScale,
                        scaleY = motionScale
                    )
            )
        }

        // Contrast Scrim for vertical controls and creator info
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.35f),
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.25f),
                            Color.Black.copy(alpha = 0.85f)
                        )
                    )
                )
        )

        // Center Pause Indicator
        AnimatedVisibility(
            visible = !isPlaying,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Surface(
                color = Color.Black.copy(alpha = 0.6f),
                shape = CircleShape,
                modifier = Modifier.size(72.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Rounded.PlayArrow,
                        contentDescription = "Paused",
                        tint = BtWhite,
                        modifier = Modifier.size(40.dp)
                    )
                }
            }
        }

        // Right Action Rail (Like, Comment, Share, Play/Pause)
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 14.dp, bottom = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            ShortRailAction(
                icon = if (isLiked) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                label = FormatUtils.formatCompactCount(short.baseLikes + if (isLiked) 1 else 0),
                iconTint = if (isLiked) BtCrimsonBright else BtWhite,
                onClick = onToggleLike,
                modifier = Modifier
                    .scale(likeScale)
                    .testTag("short_like_${short.id}")
            )

            ShortRailAction(
                icon = Icons.AutoMirrored.Rounded.Comment,
                label = FormatUtils.formatCompactCount(short.baseCommentsCount),
                iconTint = BtWhite,
                onClick = onOpenComments,
                modifier = Modifier.testTag("short_comment_${short.id}")
            )

            ShortRailAction(
                icon = Icons.Rounded.Share,
                label = FormatUtils.formatCompactCount(short.baseSharesCount),
                iconTint = BtWhite,
                onClick = onShare,
                modifier = Modifier.testTag("short_share_${short.id}")
            )

            ShortRailAction(
                icon = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                label = if (isPlaying) "Pause" else "Play",
                iconTint = BtWhite,
                onClick = { isPlaying = !isPlaying },
                modifier = Modifier.testTag("short_play_pause_${short.id}")
            )
        }

        // Bottom-Left Creator Information + Follow Button + Audio Pill
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth(0.78f)
                .padding(start = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (channel != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ChannelAvatar(channel = channel, size = 38.dp)
                    Text(
                        text = channel.handle,
                        style = MaterialTheme.typography.titleSmall,
                        color = BtWhite,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Button(
                        onClick = onToggleFollow,
                        shape = RoundedCornerShape(999.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isFollowing) {
                                Color.White.copy(alpha = 0.22f)
                            } else {
                                BtCrimson
                            },
                            contentColor = BtWhite
                        ),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(
                            horizontal = 14.dp,
                            vertical = 4.dp
                        ),
                        modifier = Modifier
                            .height(34.dp)
                            .testTag("short_follow_${short.id}")
                    ) {
                        Text(
                            text = if (isFollowing) {
                                stringResource(R.string.following)
                            } else {
                                stringResource(R.string.follow)
                            },
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Text(
                text = short.title,
                style = MaterialTheme.typography.titleMedium,
                color = BtWhite,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = short.caption,
                style = MaterialTheme.typography.bodyMedium,
                color = BtWhite.copy(alpha = 0.90f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Surface(
                color = Color.White.copy(alpha = 0.16f),
                shape = RoundedCornerShape(999.dp),
                modifier = Modifier.clickable(onClick = onToggleMute)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.MusicNote,
                        contentDescription = null,
                        tint = BtWhite,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = short.audioTrack,
                        style = MaterialTheme.typography.labelMedium,
                        color = BtWhite,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        // Progress Indicator at Very Bottom
        val progress = (elapsed / short.durationSeconds.toFloat().coerceAtLeast(10f)).coerceIn(0f, 1f)
        Canvas(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(3.dp)
        ) {
            drawRect(color = Color.White.copy(alpha = 0.22f))
            drawRect(
                color = BtCrimsonBright,
                size = Size(size.width * progress, size.height)
            )
        }
    }
}

@Composable
private fun ShortRailAction(
    icon: ImageVector,
    label: String,
    iconTint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        IconButton(
            onClick = onClick,
            modifier = modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.35f))
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = iconTint,
                modifier = Modifier.size(26.dp)
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = BtWhite,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun ShortLocalVideoStage(
    uriString: String,
    shouldPlay: Boolean,
    isMuted: Boolean,
    modifier: Modifier = Modifier
) {
    var videoViewRef by remember(uriString) { mutableStateOf<VideoView?>(null) }
    var mediaPlayerRef by remember(uriString) { mutableStateOf<MediaPlayer?>(null) }

    DisposableEffect(uriString) {
        onDispose {
            runCatching { videoViewRef?.stopPlayback() }
            mediaPlayerRef = null
        }
    }

    LaunchedEffect(shouldPlay, videoViewRef) {
        val vv = videoViewRef ?: return@LaunchedEffect
        runCatching {
            if (shouldPlay && !vv.isPlaying) vv.start()
            else if (!shouldPlay && vv.isPlaying) vv.pause()
        }
    }

    LaunchedEffect(isMuted, mediaPlayerRef) {
        val mp = mediaPlayerRef ?: return@LaunchedEffect
        val volume = if (isMuted) 0f else 1f
        runCatching { mp.setVolume(volume, volume) }
    }

    key(uriString) {
        AndroidView(
            modifier = modifier,
            factory = { context ->
                VideoView(context).apply {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        runCatching {
                            setAudioFocusRequest(AudioManager.AUDIOFOCUS_GAIN)
                            setAudioAttributes(VideoAudioHelper.mediaAudioAttributes())
                        }
                    }
                    setVideoURI(Uri.parse(uriString))
                    setOnPreparedListener { mp ->
                        mediaPlayerRef = mp
                        mp.isLooping = true
                        runCatching {
                            mp.setAudioAttributes(VideoAudioHelper.mediaAudioAttributes())
                        }
                        val volume = if (isMuted) 0f else 1f
                        runCatching { mp.setVolume(volume, volume) }
                        if (shouldPlay) start()
                    }
                    videoViewRef = this
                }
            },
            update = { view ->
                videoViewRef = view
            }
        )
    }
}
