package com.example.player

import android.os.Handler
import android.os.Looper
import android.os.ParcelFileDescriptor
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.model.PlaybackQuality
import com.example.ui.theme.BtCrimson
import com.example.ui.theme.BtWhite
import kotlinx.coroutines.delay
import org.videolan.libvlc.LibVLC
import org.videolan.libvlc.MediaPlayer
import org.videolan.libvlc.util.VLCVideoLayout

@Composable
fun UniversalVlcVideoSurface(
    uriString: String,
    isPlaying: Boolean,
    isMuted: Boolean,
    playbackSpeed: Float,
    selectedQuality: PlaybackQuality,
    aspectMode: VideoAspectMode,
    forceSoftwareDecode: Boolean,
    selectedVideoTrackId: Int?,
    selectedAudioTrackId: Int?,
    pendingSeekSeconds: Float?,
    onConsumePendingSeek: () -> Unit,
    onPositionUpdate: (Float) -> Unit,
    onTracksDiscovered: (videoTracks: List<MediaTrackOption>, audioTracks: List<MediaTrackOption>) -> Unit,
    onRequestSoftwareDecode: () -> Unit,
    onSwitchToAudioFallback: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val mainHandler = remember { Handler(Looper.getMainLooper()) }
    var videoLayoutRef by remember { mutableStateOf<VLCVideoLayout?>(null) }
    var playerRef by remember { mutableStateOf<MediaPlayer?>(null) }
    var libVlcRef by remember { mutableStateOf<LibVLC?>(null) }
    var openedFd by remember { mutableStateOf<ParcelFileDescriptor?>(null) }
    var hasTriedAutoTrackPick by remember(uriString, forceSoftwareDecode) { mutableStateOf(false) }
    var playbackError by remember(uriString, forceSoftwareDecode) { mutableStateOf(false) }
    var retryNonce by remember { mutableIntStateOf(0) }

    val latestIsPlaying by rememberUpdatedState(isPlaying)
    val latestIsMuted by rememberUpdatedState(isMuted)
    val latestSpeed by rememberUpdatedState(playbackSpeed)
    val latestQuality by rememberUpdatedState(selectedQuality)
    val latestAspect by rememberUpdatedState(aspectMode)
    val latestForceSw by rememberUpdatedState(forceSoftwareDecode)
    val latestOnPositionUpdate by rememberUpdatedState(onPositionUpdate)
    val latestOnTracksDiscovered by rememberUpdatedState(onTracksDiscovered)
    val latestOnRequestSwDecode by rememberUpdatedState(onRequestSoftwareDecode)

    if (UniversalMediaEngine.isRunningUnderRobolectric()) {
        Box(
            modifier = modifier.background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Universal Video Stage",
                style = MaterialTheme.typography.labelMedium,
                color = BtWhite.copy(alpha = 0.6f)
            )
        }
        return
    }

    DisposableEffect(uriString, forceSoftwareDecode, retryNonce, videoLayoutRef) {
        val layout = videoLayoutRef
        if (layout == null) {
            onDispose { }
        } else {
            val vlc = runCatching {
                UniversalMediaEngine.createLibVlc(
                    context = context,
                    noVideo = false,
                    forceSoftwareDecode = forceSoftwareDecode
                )
            }.getOrNull()

            if (vlc == null) {
                playbackError = true
                onDispose { }
            } else {
                val player = MediaPlayer(vlc)
                libVlcRef = vlc
                playerRef = player
                playbackError = false

                var autoRetriedSw = false

                runCatching {
                    player.attachViews(layout, null, false, true)
                    player.videoScale = latestAspect.toVlcScaleType()
                }

                player.setEventListener { event ->
                    mainHandler.post {
                        when (event.type) {
                            MediaPlayer.Event.Playing,
                            MediaPlayer.Event.ESAdded -> {
                                playbackError = false
                                runCatching {
                                    player.volume = if (latestIsMuted) 0 else 100
                                    player.rate = latestSpeed
                                    player.videoScale = latestAspect.toVlcScaleType()
                                }
                                val vTracks = UniversalMediaEngine.extractVideoTracks(player)
                                val aTracks = UniversalMediaEngine.extractAudioTracks(player)
                                if (!hasTriedAutoTrackPick && vTracks.size >= 2) {
                                    hasTriedAutoTrackPick = true
                                    val bestId = MediaTrackSelectionHelper.choosePreferredVideoTrackId(
                                        tracks = vTracks,
                                        preferredQuality = latestQuality
                                    )
                                    if (bestId != null && bestId != player.videoTrack) {
                                        runCatching { player.setVideoTrack(bestId) }
                                    }
                                    latestOnTracksDiscovered(
                                        UniversalMediaEngine.extractVideoTracks(player),
                                        aTracks
                                    )
                                } else {
                                    latestOnTracksDiscovered(vTracks, aTracks)
                                }
                            }

                            MediaPlayer.Event.EndReached -> {
                                val lengthMs = runCatching { player.length }.getOrDefault(0L)
                                if (lengthMs > 0L) {
                                    latestOnPositionUpdate(lengthMs / 1000f)
                                }
                            }

                            MediaPlayer.Event.EncounteredError -> {
                                if (!latestForceSw && !autoRetriedSw) {
                                    autoRetriedSw = true
                                    latestOnRequestSwDecode()
                                } else {
                                    playbackError = true
                                }
                            }

                            else -> Unit
                        }
                    }
                }

                val opened = runCatching {
                    UniversalMediaEngine.openMedia(
                        context = context,
                        libVLC = vlc,
                        uriString = uriString,
                        forceSoftwareDecode = forceSoftwareDecode
                    )
                }.getOrNull()

                if (opened != null) {
                    openedFd = opened.fileDescriptor
                    runCatching {
                        player.media = opened.media
                        opened.media.release()
                        player.volume = if (latestIsMuted) 0 else 100
                        player.rate = latestSpeed
                        if (latestIsPlaying) {
                            player.play()
                        }
                    }.onFailure {
                        playbackError = true
                    }
                } else {
                    playbackError = true
                }

                onDispose {
                    runCatching { player.setEventListener(null) }
                    runCatching { player.stop() }
                    runCatching { player.detachViews() }
                    runCatching { player.release() }
                    runCatching { vlc.release() }
                    runCatching { openedFd?.close() }
                    openedFd = null
                    playerRef = null
                    libVlcRef = null
                }
            }
        }
    }

    LaunchedEffect(isPlaying, playerRef) {
        val player = playerRef ?: return@LaunchedEffect
        runCatching {
            if (isPlaying && !player.isPlaying) {
                player.play()
            } else if (!isPlaying && player.isPlaying) {
                player.pause()
            }
        }
    }

    LaunchedEffect(isMuted, playerRef) {
        val player = playerRef ?: return@LaunchedEffect
        runCatching {
            player.volume = if (isMuted) 0 else 100
        }
    }

    LaunchedEffect(playbackSpeed, playerRef) {
        val player = playerRef ?: return@LaunchedEffect
        runCatching {
            player.rate = playbackSpeed
        }
    }

    LaunchedEffect(aspectMode, playerRef) {
        val player = playerRef ?: return@LaunchedEffect
        runCatching {
            player.videoScale = aspectMode.toVlcScaleType()
        }
    }

    LaunchedEffect(selectedVideoTrackId, playerRef) {
        val targetId = selectedVideoTrackId ?: return@LaunchedEffect
        val player = playerRef ?: return@LaunchedEffect
        runCatching {
            if (player.videoTrack != targetId) {
                player.setVideoTrack(targetId)
            }
        }
    }

    LaunchedEffect(selectedAudioTrackId, playerRef) {
        val targetId = selectedAudioTrackId ?: return@LaunchedEffect
        val player = playerRef ?: return@LaunchedEffect
        runCatching {
            if (player.audioTrack != targetId) {
                player.setAudioTrack(targetId)
            }
        }
    }

    LaunchedEffect(pendingSeekSeconds, playerRef) {
        val target = pendingSeekSeconds ?: return@LaunchedEffect
        val player = playerRef ?: return@LaunchedEffect
        runCatching {
            val targetMs = (target * 1000f).toLong().coerceAtLeast(0L)
            if (player.isSeekable) {
                player.time = targetMs
            } else {
                val len = player.length
                if (len > 0L) {
                    player.position = (targetMs.toFloat() / len.toFloat()).coerceIn(0f, 1f)
                }
            }
        }
        onConsumePendingSeek()
    }

    LaunchedEffect(isPlaying, playerRef, uriString) {
        while (isPlaying) {
            val player = playerRef
            if (player != null) {
                runCatching {
                    val timeMs = player.time
                    if (timeMs >= 0L) {
                        onPositionUpdate(timeMs / 1000f)
                    }
                }
            }
            delay(250L)
        }
    }

    Box(modifier = modifier) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                VLCVideoLayout(ctx).also { videoLayoutRef = it }
            },
            update = { layout ->
                if (videoLayoutRef !== layout) {
                    videoLayoutRef = layout
                }
            }
        )

        if (playbackError) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.86f)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "All-format software decoder retry or audio mode available",
                        style = MaterialTheme.typography.bodyMedium,
                        color = BtWhite,
                        textAlign = TextAlign.Center
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(
                            onClick = {
                                if (!forceSoftwareDecode) {
                                    onRequestSoftwareDecode()
                                } else {
                                    playbackError = false
                                    retryNonce++
                                }
                            },
                            modifier = Modifier.testTag("video_retry_sw_button")
                        ) {
                            Text("Retry SW Decode", color = BtWhite)
                        }
                        Button(
                            onClick = onSwitchToAudioFallback,
                            colors = ButtonDefaults.buttonColors(containerColor = BtCrimson),
                            modifier = Modifier.testTag("video_fallback_audio_button")
                        ) {
                            Text("Play Audio Track")
                        }
                    }
                }
            }
        }
    }
}
