package com.example.audio

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.ParcelFileDescriptor
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.player.UniversalMediaEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.videolan.libvlc.LibVLC
import org.videolan.libvlc.MediaPlayer as VlcMediaPlayer

data class BackgroundAudioTrack(
    val id: String,
    val title: String,
    val subtitle: String,
    val uriString: String,
    val isVideoSource: Boolean,
    val durationSeconds: Int
)

data class BackgroundAudioState(
    val currentTrack: BackgroundAudioTrack? = null,
    val queue: List<BackgroundAudioTrack> = emptyList(),
    val isPlaying: Boolean = false,
    val positionSeconds: Float = 0f,
    val durationSeconds: Int = 0,
    val playbackSpeed: Float = 1.0f
)

object BackgroundAudioController {
    private val _state = MutableStateFlow(BackgroundAudioState())
    val state: StateFlow<BackgroundAudioState> = _state.asStateFlow()

    internal fun updateState(transform: (BackgroundAudioState) -> BackgroundAudioState) {
        _state.update(transform)
    }

    fun playTrackAsAudio(
        context: Context,
        track: BackgroundAudioTrack,
        queue: List<BackgroundAudioTrack> = listOf(track),
        startPositionSeconds: Float = 0f,
        playbackSpeed: Float = 1.0f
    ) {
        val resolvedQueue = if (queue.any { it.id == track.id }) queue else listOf(track) + queue
        _state.value = BackgroundAudioState(
            currentTrack = track,
            queue = resolvedQueue,
            isPlaying = true,
            positionSeconds = startPositionSeconds.coerceAtLeast(0f),
            durationSeconds = track.durationSeconds.coerceAtLeast(1),
            playbackSpeed = playbackSpeed
        )
        startServiceAction(context, BackgroundAudioService.ACTION_PLAY_TRACK) {
            putExtra(BackgroundAudioService.EXTRA_SEEK_MS, (startPositionSeconds * 1000f).toInt().coerceAtLeast(0))
            putExtra(BackgroundAudioService.EXTRA_SPEED, playbackSpeed)
        }
    }

    fun togglePlayPause(context: Context) {
        if (_state.value.currentTrack == null) return
        startServiceAction(context, BackgroundAudioService.ACTION_TOGGLE_PLAY_PAUSE)
    }

    fun seekTo(context: Context, positionSeconds: Float) {
        val track = _state.value.currentTrack ?: return
        val clamped = positionSeconds.coerceIn(0f, track.durationSeconds.toFloat().coerceAtLeast(1f))
        _state.update { it.copy(positionSeconds = clamped) }
        startServiceAction(context, BackgroundAudioService.ACTION_SEEK) {
            putExtra(BackgroundAudioService.EXTRA_SEEK_MS, (clamped * 1000f).toInt())
        }
    }

    fun setPlaybackSpeed(context: Context, speed: Float) {
        _state.update { it.copy(playbackSpeed = speed) }
        if (_state.value.currentTrack != null) {
            startServiceAction(context, BackgroundAudioService.ACTION_SET_SPEED) {
                putExtra(BackgroundAudioService.EXTRA_SPEED, speed)
            }
        }
    }

    fun playNext(context: Context) {
        if (_state.value.currentTrack == null) return
        startServiceAction(context, BackgroundAudioService.ACTION_NEXT)
    }

    fun playPrevious(context: Context) {
        if (_state.value.currentTrack == null) return
        startServiceAction(context, BackgroundAudioService.ACTION_PREVIOUS)
    }

    fun stop(context: Context) {
        _state.value = BackgroundAudioState()
        runCatching {
            context.stopService(Intent(context, BackgroundAudioService::class.java))
        }
    }

    private fun startServiceAction(
        context: Context,
        action: String,
        extras: Intent.() -> Unit = {}
    ) {
        val intent = Intent(context, BackgroundAudioService::class.java).apply {
            this.action = action
            extras()
        }
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                ContextCompat.startForegroundService(context, intent)
            } else {
                context.startService(intent)
            }
        }
    }
}

class BackgroundAudioService : Service() {

    private var mediaPlayer: MediaPlayer? = null
    private var libVLC: LibVLC? = null
    private var vlcPlayer: VlcMediaPlayer? = null
    private var vlcPfd: ParcelFileDescriptor? = null
    private var wakeLock: PowerManager.WakeLock? = null

    private var audioFocusRequest: AudioFocusRequest? = null
    private var noisyReceiverRegistered = false
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val mainHandler = Handler(Looper.getMainLooper())
    private var tickerJob: Job? = null

    private val noisyReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == AudioManager.ACTION_AUDIO_BECOMING_NOISY) {
                pausePlayback()
            }
        }
    }

    private val focusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        when (focusChange) {
            AudioManager.AUDIOFOCUS_LOSS,
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> pausePlayback()
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                runCatching { vlcPlayer?.volume = 25 }
                runCatching { mediaPlayer?.setVolume(0.25f, 0.25f) }
            }
            AudioManager.AUDIOFOCUS_GAIN -> {
                runCatching { vlcPlayer?.volume = 100 }
                runCatching { mediaPlayer?.setVolume(1f, 1f) }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        runCatching {
            ContextCompat.registerReceiver(
                this,
                noisyReceiver,
                IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY),
                ContextCompat.RECEIVER_NOT_EXPORTED
            )
            noisyReceiverRegistered = true
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        promoteToForeground()

        when (intent?.action) {
            ACTION_PLAY_TRACK -> {
                val seekMs = intent.getIntExtra(EXTRA_SEEK_MS, 0)
                val speed = intent.getFloatExtra(EXTRA_SPEED, 1.0f)
                prepareAndPlayCurrentTrack(startMs = seekMs, playbackSpeed = speed)
            }
            ACTION_TOGGLE_PLAY_PAUSE -> {
                val vlc = vlcPlayer
                val mp = mediaPlayer
                when {
                    vlc != null -> if (runCatching { vlc.isPlaying }.getOrDefault(false)) pausePlayback() else resumePlayback()
                    mp != null -> if (runCatching { mp.isPlaying }.getOrDefault(false)) pausePlayback() else resumePlayback()
                    else -> {
                        val state = BackgroundAudioController.state.value
                        if (state.currentTrack != null) {
                            prepareAndPlayCurrentTrack(
                                startMs = (state.positionSeconds * 1000f).toInt(),
                                playbackSpeed = state.playbackSpeed
                            )
                        }
                    }
                }
            }
            ACTION_SEEK -> {
                val seekMs = intent.getIntExtra(EXTRA_SEEK_MS, 0)
                runCatching {
                    vlcPlayer?.let { vp ->
                        if (vp.isSeekable) {
                            vp.time = seekMs.toLong().coerceAtLeast(0L)
                        } else {
                            val len = vp.length
                            if (len > 0L) {
                                vp.position = (seekMs.toFloat() / len.toFloat()).coerceIn(0f, 1f)
                            }
                        }
                    }
                }
                runCatching { mediaPlayer?.seekTo(seekMs) }
            }
            ACTION_SET_SPEED -> {
                val speed = intent.getFloatExtra(EXTRA_SPEED, 1.0f)
                runCatching { vlcPlayer?.rate = speed }
                runCatching {
                    mediaPlayer?.let { mp ->
                        val params = mp.playbackParams
                        params.speed = speed
                        mp.playbackParams = params
                    }
                }
            }
            ACTION_NEXT -> playAdjacentTrack(step = 1)
            ACTION_PREVIOUS -> playAdjacentTrack(step = -1)
            ACTION_STOP -> {
                releasePlayer()
                BackgroundAudioController.updateState { BackgroundAudioState() }
                stopForegroundCompat()
                stopSelf()
                return START_NOT_STICKY
            }
        }
        return START_STICKY
    }

    private fun prepareAndPlayCurrentTrack(startMs: Int, playbackSpeed: Float) {
        val track = BackgroundAudioController.state.value.currentTrack ?: return
        requestAudioFocus()
        acquireWakeLock()
        releasePlayer()

        if (!UniversalMediaEngine.isRunningUnderRobolectric()) {
            val startedVlc = prepareWithLibVlc(track, startMs, playbackSpeed)
            if (startedVlc) return
        }

        prepareWithAndroidMediaPlayer(track, startMs, playbackSpeed)
    }

    private fun prepareWithLibVlc(
        track: BackgroundAudioTrack,
        startMs: Int,
        playbackSpeed: Float
    ): Boolean {
        return runCatching {
            val vlc = UniversalMediaEngine.createLibVlc(
                context = applicationContext,
                noVideo = true,
                forceSoftwareDecode = true
            )
            libVLC = vlc
            val player = VlcMediaPlayer(vlc)
            vlcPlayer = player

            var fallbackTriggered = false
            var initialSeekPending = startMs > 0

            player.setEventListener { event ->
                mainHandler.post {
                    when (event.type) {
                        VlcMediaPlayer.Event.Playing -> {
                            val lenMs = runCatching { player.length }.getOrDefault(0L)
                            val durSec = if (lenMs > 0L) {
                                (lenMs / 1000L).toInt().coerceAtLeast(1)
                            } else {
                                track.durationSeconds.coerceAtLeast(1)
                            }
                            if (initialSeekPending) {
                                initialSeekPending = false
                                runCatching {
                                    if (player.isSeekable) {
                                        player.time = startMs.toLong()
                                    }
                                }
                            }
                            runCatching {
                                player.volume = 100
                                player.rate = playbackSpeed
                            }
                            BackgroundAudioController.updateState {
                                it.copy(isPlaying = true, durationSeconds = durSec)
                            }
                            startPositionTicker()
                            updateNotification()
                        }

                        VlcMediaPlayer.Event.EndReached -> {
                            playAdjacentTrack(step = 1)
                        }

                        VlcMediaPlayer.Event.EncounteredError -> {
                            if (!fallbackTriggered) {
                                fallbackTriggered = true
                                releasePlayer()
                                prepareWithAndroidMediaPlayer(track, startMs, playbackSpeed)
                            } else {
                                BackgroundAudioController.updateState { it.copy(isPlaying = false) }
                                updateNotification()
                            }
                        }

                        else -> Unit
                    }
                }
            }

            val opened = UniversalMediaEngine.openMedia(
                context = applicationContext,
                libVLC = vlc,
                uriString = track.uriString,
                forceSoftwareDecode = true,
                startTimeSeconds = startMs / 1000f
            )
            vlcPfd = opened.fileDescriptor
            player.media = opened.media
            opened.media.release()
            player.volume = 100
            player.rate = playbackSpeed
            player.play()
            true
        }.getOrDefault(false)
    }

    private fun prepareWithAndroidMediaPlayer(
        track: BackgroundAudioTrack,
        startMs: Int,
        playbackSpeed: Float
    ) {
        val mp = MediaPlayer()
        mediaPlayer = mp
        runCatching {
            mp.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            mp.setWakeMode(applicationContext, PowerManager.PARTIAL_WAKE_LOCK)
            mp.setDataSource(applicationContext, Uri.parse(track.uriString))
            mp.setOnPreparedListener { prepared ->
                val actualDurSec = (prepared.duration / 1000).coerceAtLeast(1)
                if (startMs > 0) {
                    runCatching { prepared.seekTo(startMs.coerceAtMost(prepared.duration)) }
                }
                runCatching {
                    val params = prepared.playbackParams
                    params.speed = playbackSpeed
                    prepared.playbackParams = params
                }
                prepared.setVolume(1f, 1f)
                prepared.start()
                BackgroundAudioController.updateState {
                    it.copy(
                        isPlaying = true,
                        durationSeconds = actualDurSec
                    )
                }
                startPositionTicker()
                updateNotification()
            }
            mp.setOnCompletionListener {
                playAdjacentTrack(step = 1)
            }
            mp.setOnErrorListener { _, _, _ ->
                BackgroundAudioController.updateState { it.copy(isPlaying = false) }
                updateNotification()
                true
            }
            mp.prepareAsync()
        }.onFailure {
            BackgroundAudioController.updateState { it.copy(isPlaying = false) }
            updateNotification()
        }
    }

    private fun playAdjacentTrack(step: Int) {
        val state = BackgroundAudioController.state.value
        val current = state.currentTrack ?: return
        val queue = state.queue
        if (queue.size <= 1) {
            prepareAndPlayCurrentTrack(startMs = 0, playbackSpeed = state.playbackSpeed)
            return
        }
        val idx = queue.indexOfFirst { it.id == current.id }.coerceAtLeast(0)
        val nextIdx = (idx + step + queue.size) % queue.size
        val nextTrack = queue[nextIdx]
        BackgroundAudioController.updateState {
            it.copy(
                currentTrack = nextTrack,
                isPlaying = true,
                positionSeconds = 0f,
                durationSeconds = nextTrack.durationSeconds.coerceAtLeast(1)
            )
        }
        prepareAndPlayCurrentTrack(startMs = 0, playbackSpeed = state.playbackSpeed)
    }

    private fun pausePlayback() {
        runCatching {
            if (vlcPlayer?.isPlaying == true) {
                vlcPlayer?.pause()
            }
        }
        runCatching {
            if (mediaPlayer?.isPlaying == true) {
                mediaPlayer?.pause()
            }
        }
        tickerJob?.cancel()
        releaseWakeLock()
        BackgroundAudioController.updateState { it.copy(isPlaying = false) }
        updateNotification()
    }

    private fun resumePlayback() {
        requestAudioFocus()
        acquireWakeLock()
        runCatching {
            vlcPlayer?.let { vp ->
                vp.volume = 100
                vp.play()
            }
        }
        runCatching {
            mediaPlayer?.setVolume(1f, 1f)
            mediaPlayer?.start()
        }
        BackgroundAudioController.updateState { it.copy(isPlaying = true) }
        startPositionTicker()
        updateNotification()
    }

    private fun startPositionTicker() {
        tickerJob?.cancel()
        tickerJob = serviceScope.launch {
            while (isActive) {
                val vp = vlcPlayer
                val mp = mediaPlayer
                if (vp != null && runCatching { vp.isPlaying }.getOrDefault(false)) {
                    val timeMs = runCatching { vp.time }.getOrDefault(-1L)
                    if (timeMs >= 0L) {
                        BackgroundAudioController.updateState { it.copy(positionSeconds = timeMs / 1000f) }
                    }
                } else if (mp != null && runCatching { mp.isPlaying }.getOrDefault(false)) {
                    val posSec = runCatching { mp.currentPosition / 1000f }.getOrDefault(0f)
                    BackgroundAudioController.updateState { it.copy(positionSeconds = posSec) }
                }
                delay(300L)
            }
        }
    }

    private fun acquireWakeLock() {
        runCatching {
            if (wakeLock?.isHeld == true) return
            val pm = getSystemService(Context.POWER_SERVICE) as? PowerManager ?: return
            wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "BTTube::BackgroundAudioWakeLock")
                .apply {
                    setReferenceCounted(false)
                    acquire(4 * 60 * 60 * 1000L)
                }
        }
    }

    private fun releaseWakeLock() {
        runCatching {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
            }
        }
    }

    private fun promoteToForeground() {
        val notification = buildNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun updateNotification() {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        runCatching { manager.notify(NOTIFICATION_ID, buildNotification()) }
    }

    private fun buildNotification(): Notification {
        val state = BackgroundAudioController.state.value
        val track = state.currentTrack
        val openAppIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val prevIntent = actionPendingIntent(ACTION_PREVIOUS, 1)
        val toggleIntent = actionPendingIntent(ACTION_TOGGLE_PLAY_PAUSE, 2)
        val nextIntent = actionPendingIntent(ACTION_NEXT, 3)
        val stopIntent = actionPendingIntent(ACTION_STOP, 4)

        val subtitlePrefix = if (track?.isVideoSource == true) "Video as Audio" else "Device Audio"
        val bodyText = if (track != null) {
            "$subtitlePrefix • ${track.subtitle}"
        } else {
            "BT Tube Background Audio"
        }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(track?.title ?: getString(R.string.app_name))
            .setContentText(bodyText)
            .setContentIntent(openAppIntent)
            .setOngoing(state.isPlaying)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .addAction(android.R.drawable.ic_media_previous, "Previous", prevIntent)
            .addAction(
                if (state.isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play,
                if (state.isPlaying) "Pause" else "Play",
                toggleIntent
            )
            .addAction(android.R.drawable.ic_media_next, "Next", nextIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop", stopIntent)
            .build()
    }

    private fun actionPendingIntent(action: String, requestCode: Int): PendingIntent {
        val intent = Intent(this, BackgroundAudioService::class.java).apply {
            this.action = action
        }
        return PendingIntent.getService(
            this,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
            val channel = NotificationChannel(
                CHANNEL_ID,
                "BT Tube Background Audio",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Plays device songs and video songs in the background"
                setShowBadge(false)
            }
            manager.createNotificationChannel(channel)
        }
    }

    private fun requestAudioFocus() {
        val audioManager = getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val req = audioFocusRequest ?: AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setAcceptsDelayedFocusGain(true)
                .setOnAudioFocusChangeListener(focusChangeListener)
                .build()
                .also { audioFocusRequest = it }
            runCatching { audioManager.requestAudioFocus(req) }
        } else {
            @Suppress("DEPRECATION")
            runCatching {
                audioManager.requestAudioFocus(
                    focusChangeListener,
                    AudioManager.STREAM_MUSIC,
                    AudioManager.AUDIOFOCUS_GAIN
                )
            }
        }
    }

    private fun abandonAudioFocus() {
        val audioManager = getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest?.let { req ->
                runCatching { audioManager.abandonAudioFocusRequest(req) }
            }
        } else {
            @Suppress("DEPRECATION")
            runCatching { audioManager.abandonAudioFocus(focusChangeListener) }
        }
    }

    private fun releasePlayer() {
        tickerJob?.cancel()
        runCatching {
            vlcPlayer?.let { vp ->
                vp.setEventListener(null)
                if (vp.isPlaying) vp.stop()
                vp.release()
            }
        }
        vlcPlayer = null
        runCatching { libVLC?.release() }
        libVLC = null
        runCatching { vlcPfd?.close() }
        vlcPfd = null

        runCatching {
            mediaPlayer?.let { mp ->
                if (mp.isPlaying) mp.stop()
                mp.reset()
                mp.release()
            }
        }
        mediaPlayer = null
    }

    private fun stopForegroundCompat() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
    }

    override fun onDestroy() {
        releasePlayer()
        releaseWakeLock()
        abandonAudioFocus()
        if (noisyReceiverRegistered) {
            runCatching { unregisterReceiver(noisyReceiver) }
            noisyReceiverRegistered = false
        }
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val CHANNEL_ID = "bt_tube_audio_playback"
        private const val NOTIFICATION_ID = 4102

        const val ACTION_PLAY_TRACK = "com.example.audio.ACTION_PLAY_TRACK"
        const val ACTION_TOGGLE_PLAY_PAUSE = "com.example.audio.ACTION_TOGGLE_PLAY_PAUSE"
        const val ACTION_SEEK = "com.example.audio.ACTION_SEEK"
        const val ACTION_SET_SPEED = "com.example.audio.ACTION_SET_SPEED"
        const val ACTION_NEXT = "com.example.audio.ACTION_NEXT"
        const val ACTION_PREVIOUS = "com.example.audio.ACTION_PREVIOUS"
        const val ACTION_STOP = "com.example.audio.ACTION_STOP"

        const val EXTRA_SEEK_MS = "extra_seek_ms"
        const val EXTRA_SPEED = "extra_speed"
    }
}
