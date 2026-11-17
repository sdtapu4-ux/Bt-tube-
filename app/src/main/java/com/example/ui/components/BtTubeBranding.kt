package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.PlayCircleFilled
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.LocalVideoScanner
import com.example.model.Channel
import com.example.model.StudioSceneStyle
import com.example.model.ThumbnailArtwork
import com.example.ui.theme.BtCrimson
import com.example.ui.theme.BtCrimsonBright
import com.example.ui.theme.BtLiveEmerald
import com.example.ui.theme.BtVerifiedBlue
import com.example.ui.theme.BtWhite
import com.example.ui.theme.OutfitFontFamily

@Composable
fun BtTubeLogo(
    modifier: Modifier = Modifier,
    compact: Boolean = false
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Original BT Tube rounded cinema prism mark with broadcast arcs
        Box(
            modifier = Modifier
                .size(if (compact) 30.dp else 34.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(
                    Brush.linearGradient(
                        colors = listOf(BtCrimson, BtCrimsonBright)
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(if (compact) 18.dp else 20.dp)) {
                val w = size.width
                val h = size.height
                // Stylized bold prism triangle on left
                val playPath = Path().apply {
                    moveTo(w * 0.16f, h * 0.18f)
                    lineTo(w * 0.64f, h * 0.50f)
                    lineTo(w * 0.16f, h * 0.82f)
                    close()
                }
                drawPath(path = playPath, color = BtWhite)

                // Twin broadcast arcs on right (original BT Tube motif)
                drawArc(
                    color = BtWhite,
                    startAngle = -45f,
                    sweepAngle = 90f,
                    useCenter = false,
                    topLeft = Offset(w * 0.42f, h * 0.25f),
                    size = Size(w * 0.44f, h * 0.50f),
                    style = Stroke(width = w * 0.11f, cap = StrokeCap.Round)
                )
                drawArc(
                    color = BtWhite.copy(alpha = 0.85f),
                    startAngle = -50f,
                    sweepAngle = 100f,
                    useCenter = false,
                    topLeft = Offset(w * 0.54f, h * 0.12f),
                    size = Size(w * 0.48f, h * 0.76f),
                    style = Stroke(width = w * 0.10f, cap = StrokeCap.Round)
                )
            }
        }

        if (!compact) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.brand_bt),
                    fontFamily = OutfitFontFamily,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 21.sp,
                    color = BtCrimson,
                    letterSpacing = (-0.5).sp
                )
                Spacer(modifier = Modifier.width(2.dp))
                Text(
                    text = stringResource(R.string.brand_tube),
                    fontFamily = OutfitFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 21.sp,
                    color = MaterialTheme.colorScheme.onBackground,
                    letterSpacing = (-0.5).sp
                )
            }
        }
    }
}

@Composable
fun ChannelAvatar(
    channel: Channel,
    size: Dp = 40.dp,
    showVerifiedBadge: Boolean = false,
    hasActiveRing: Boolean = false,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (hasActiveRing) {
                        Modifier
                            .border(2.dp, BtCrimson, CircleShape)
                            .padding(3.dp)
                    } else {
                        Modifier
                    }
                )
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Color(channel.avatarColorHex),
                            Color(channel.avatarColorHex).copy(alpha = 0.75f)
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = channel.initials,
                color = BtWhite,
                fontFamily = OutfitFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = (size.value * 0.36f).sp
            )
        }

        if (showVerifiedBadge && channel.isVerified) {
            Icon(
                imageVector = Icons.Rounded.CheckCircle,
                contentDescription = "Verified channel",
                tint = BtVerifiedBlue,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(14.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface)
            )
        }
    }
}

@Composable
fun VideoThumbnailSurface(
    artwork: ThumbnailArtwork,
    contentDescription: String,
    modifier: Modifier = Modifier,
    durationLabel: String? = null,
    isLive: Boolean = false,
    is4K: Boolean = false,
    aspectRatio: Float? = 16f / 9f
) {
    val sizedModifier = if (aspectRatio != null) {
        modifier.fillMaxWidth().aspectRatio(aspectRatio)
    } else {
        modifier
    }

    Box(
        modifier = sizedModifier.background(Color(0xFF14141A))
    ) {
        when (artwork) {
            is ThumbnailArtwork.PhotoDrawable -> {
                Image(
                    painter = painterResource(id = artwork.resId),
                    contentDescription = contentDescription,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
            is ThumbnailArtwork.VectorScene -> {
                StudioVectorArtwork(
                    artwork = artwork,
                    modifier = Modifier.fillMaxSize()
                )
            }
            is ThumbnailArtwork.LocalVideoFrame -> {
                val context = LocalContext.current
                val bitmap by produceState<ImageBitmap?>(initialValue = null, key1 = artwork.uriString) {
                    value = LocalVideoScanner.loadVideoThumbnailBitmap(context, artwork.uriString)
                        ?.asImageBitmap()
                }
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap!!,
                        contentDescription = contentDescription,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(modifier = Modifier.fillMaxSize()) {
                        StudioVectorArtwork(
                            artwork = ThumbnailArtwork.VectorScene(
                                StudioSceneStyle.CRIMSON_STUDIO,
                                "DEVICE VIDEO"
                            ),
                            modifier = Modifier.fillMaxSize()
                        )
                        Icon(
                            imageVector = Icons.Rounded.PlayCircleFilled,
                            contentDescription = null,
                            tint = BtWhite.copy(alpha = 0.85f),
                            modifier = Modifier
                                .align(Alignment.Center)
                                .size(48.dp)
                        )
                    }
                }
            }
        }

        // Subtle cinematic vignette for pill legibility
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.45f)
                        )
                    )
                )
        )

        // Quality / 4K badge at top-left if applicable
        if (is4K && !isLive) {
            Surface(
                color = Color.Black.copy(alpha = 0.72f),
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(10.dp)
            ) {
                Text(
                    text = "4K HDR",
                    style = MaterialTheme.typography.labelSmall,
                    color = BtWhite,
                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                )
            }
        }

        // Duration or LIVE badge at bottom-right
        if (isLive) {
            Surface(
                color = BtCrimson,
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(10.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(BtWhite)
                    )
                    Text(
                        text = "LIVE",
                        style = MaterialTheme.typography.labelSmall,
                        color = BtWhite,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        } else if (!durationLabel.isNullOrEmpty()) {
            Surface(
                color = Color.Black.copy(alpha = 0.80f),
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(10.dp)
            ) {
                Text(
                    text = durationLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = BtWhite,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                )
            }
        }
    }
}

@Composable
private fun StudioVectorArtwork(
    artwork: ThumbnailArtwork.VectorScene,
    modifier: Modifier = Modifier
) {
    val start = Color(artwork.style.startHex)
    val end = Color(artwork.style.endHex)
    val accent = Color(artwork.style.accentHex)

    Box(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(
                brush = Brush.linearGradient(
                    colors = listOf(start, end),
                    start = Offset.Zero,
                    end = Offset(size.width, size.height)
                )
            )
            // Abstract studio rings & grid waves
            drawCircle(
                color = accent.copy(alpha = 0.16f),
                radius = size.minDimension * 0.55f,
                center = Offset(size.width * 0.78f, size.height * 0.25f)
            )
            drawCircle(
                color = BtWhite.copy(alpha = 0.08f),
                radius = size.minDimension * 0.35f,
                center = Offset(size.width * 0.2f, size.height * 0.8f)
            )
            drawRoundRect(
                color = accent.copy(alpha = 0.35f),
                topLeft = Offset(size.width * 0.08f, size.height * 0.16f),
                size = Size(size.width * 0.14f, 4.dp.toPx()),
                cornerRadius = CornerRadius(4f, 4f)
            )
            // Stylized waveform bars
            val barCount = 14
            val barWidth = size.width * 0.018f
            val gap = size.width * 0.022f
            val startX = size.width * 0.52f
            val baseY = size.height * 0.78f
            for (i in 0 until barCount) {
                val factor = ((i * 37 + 13) % 10) / 10f + 0.25f
                val barHeight = size.height * 0.28f * factor
                drawRoundRect(
                    color = accent.copy(alpha = 0.55f),
                    topLeft = Offset(startX + i * (barWidth + gap), baseY - barHeight),
                    size = Size(barWidth, barHeight),
                    cornerRadius = CornerRadius(6f, 6f)
                )
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Surface(
                color = BtWhite.copy(alpha = 0.16f),
                shape = RoundedCornerShape(6.dp)
            ) {
                Text(
                    text = "BT TUBE STUDIO",
                    style = MaterialTheme.typography.labelSmall,
                    color = BtWhite,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }
            Text(
                text = artwork.badgeText,
                fontFamily = OutfitFontFamily,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 24.sp,
                color = BtWhite,
                letterSpacing = (-0.5).sp
            )
        }
    }
}
