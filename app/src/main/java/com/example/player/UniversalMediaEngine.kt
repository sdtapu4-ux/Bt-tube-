package com.example.player

import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.ParcelFileDescriptor
import com.example.model.PlaybackQuality
import org.videolan.libvlc.LibVLC
import org.videolan.libvlc.Media
import org.videolan.libvlc.MediaPlayer
import org.videolan.libvlc.interfaces.IMedia

data class MediaTrackOption(
    val id: Int,
    val label: String,
    val width: Int = 0,
    val height: Int = 0,
    val bitrate: Int = 0,
    val codec: String = "",
    val isSelected: Boolean = false
)

enum class VideoAspectMode(val label: String) {
    FIT("Fit"),
    FILL("Fill"),
    RATIO_16_9("16:9"),
    RATIO_4_3("4:3");

    fun toVlcScaleType(): MediaPlayer.ScaleType = when (this) {
        FIT -> MediaPlayer.ScaleType.SURFACE_BEST_FIT
        FILL -> MediaPlayer.ScaleType.SURFACE_FILL
        RATIO_16_9 -> MediaPlayer.ScaleType.SURFACE_16_9
        RATIO_4_3 -> MediaPlayer.ScaleType.SURFACE_4_3
    }
}

object MediaTrackSelectionHelper {
    private val advancedCodecs = setOf("hevc", "h265", "hvc1", "hev1", "vp9", "vp09", "av01", "av1", "mp4v", "xvid", "divx", "dx50", "wmv3", "vc1")

    fun choosePreferredVideoTrackId(
        tracks: List<MediaTrackOption>,
        preferredQuality: PlaybackQuality
    ): Int? {
        val valid = tracks.filter { it.id >= 0 }
        if (valid.isEmpty()) return null
        if (valid.size == 1) return valid.first().id

        val targetShortSide = when (preferredQuality) {
            PlaybackQuality.SD_144 -> 144
            PlaybackQuality.SD_240 -> 240
            PlaybackQuality.SD_360 -> 360
            PlaybackQuality.SD_480 -> 480
            PlaybackQuality.HD_720 -> 720
            PlaybackQuality.HD_1080 -> 1080
            PlaybackQuality.AUTO -> Int.MAX_VALUE
        }

        // In multi-track files created by third-party downloaders (e.g. PLAYit / VidMate wrappers),
        // Track 0 is frequently a tiny low-bitrate H.264 "unsupported format" banner while Track 1+
        // holds the actual video stream (higher resolution, higher bitrate, or HEVC/VP9/AV1/Xvid).
        return valid.mapIndexed { index, track ->
            val shortSide = minOf(track.width, track.height).takeIf { it > 0 }
                ?: maxOf(track.width, track.height)
            val pixelArea = track.width.toLong() * track.height.toLong()
            val codecLower = track.codec.lowercase()
            val isAdvancedCodec = advancedCodecs.any { codecLower.contains(it) }

            val qualityScore = when {
                shortSide <= 0 -> 0L
                preferredQuality == PlaybackQuality.AUTO -> pixelArea
                shortSide <= targetShortSide -> 10_000_000L + shortSide
                else -> 5_000_000L - kotlin.math.abs(shortSide - targetShortSide)
            }

            val bitrateScore = track.bitrate.toLong().coerceAtLeast(0L) / 1000L
            val codecBonus = if (isAdvancedCodec) 250_000L else 0L
            // Slight tie-breaker toward secondary tracks when a file embeds 2+ video streams
            // with equal/unknown metadata so Track 0 placeholder screens are skipped when present.
            val secondaryBias = if (index > 0) 1L else 0L

            val totalScore = qualityScore + bitrateScore + codecBonus + secondaryBias
            track to totalScore
        }.maxByOrNull { it.second }?.first?.id
    }
}

object UniversalMediaEngine {

    fun isRunningUnderRobolectric(): Boolean {
        return Build.FINGERPRINT.equals("robolectric", ignoreCase = true)
    }

    fun createLibVlc(
        context: Context,
        noVideo: Boolean = false,
        forceSoftwareDecode: Boolean = false
    ): LibVLC {
        val options = arrayListOf(
            "--no-drop-late-frames",
            "--no-skip-frames",
            "--file-caching=600",
            "--network-caching=1500",
            "--audio-time-stretch"
        )
        if (noVideo) {
            options.add("--no-video")
        }
        if (forceSoftwareDecode) {
            options.add("--avcodec-hw=none")
        } else {
            options.add("--avcodec-fast")
        }
        return LibVLC(context.applicationContext, options)
    }

    data class OpenedVlcMedia(
        val media: Media,
        val fileDescriptor: ParcelFileDescriptor?
    )

    fun openMedia(
        context: Context,
        libVLC: LibVLC,
        uriString: String,
        forceSoftwareDecode: Boolean = false,
        startTimeSeconds: Float = 0f
    ): OpenedVlcMedia {
        val uri = Uri.parse(uriString)
        val pfd = if (uri.scheme.equals("content", ignoreCase = true) ||
            uri.scheme.equals("file", ignoreCase = true)
        ) {
            runCatching { context.contentResolver.openFileDescriptor(uri, "r") }.getOrNull()
        } else {
            null
        }

        val media = if (pfd != null) {
            Media(libVLC, pfd.fileDescriptor)
        } else {
            Media(libVLC, uri)
        }

        if (forceSoftwareDecode) {
            media.addOption(":avcodec-hw=none")
        } else {
            media.setHWDecoderEnabled(true, false)
        }
        if (startTimeSeconds > 0.5f) {
            media.addOption(":start-time=${startTimeSeconds.toInt()}")
        }
        return OpenedVlcMedia(media = media, fileDescriptor = pfd)
    }

    fun extractVideoTracks(player: MediaPlayer): List<MediaTrackOption> {
        val descriptions = runCatching { player.videoTracks }.getOrNull().orEmpty()
        val currentId = runCatching { player.videoTrack }.getOrDefault(-1)
        val mediaMeta = readMediaVideoTrackMeta(player.media)

        return descriptions
            .filter { it.id >= 0 }
            .mapIndexed { index, desc ->
                val meta = mediaMeta[desc.id] ?: mediaMeta.values.elementAtOrNull(index)
                val codecName = normalizeCodecName(meta?.codec)
                val resLabel = if (meta != null && meta.width > 0 && meta.height > 0) {
                    val shortSide = minOf(meta.width, meta.height)
                    "${shortSide}p"
                } else {
                    ""
                }
                val cleanName = desc.name?.takeIf { it.isNotBlank() && !it.startsWith("Track", ignoreCase = true) }
                val parts = listOfNotNull(
                    "Video ${index + 1}",
                    resLabel.takeIf { it.isNotBlank() },
                    codecName.takeIf { it.isNotBlank() },
                    cleanName
                )
                MediaTrackOption(
                    id = desc.id,
                    label = parts.joinToString(" • "),
                    width = meta?.width ?: 0,
                    height = meta?.height ?: 0,
                    bitrate = meta?.bitrate ?: 0,
                    codec = codecName,
                    isSelected = desc.id == currentId
                )
            }
    }

    fun extractAudioTracks(player: MediaPlayer): List<MediaTrackOption> {
        val descriptions = runCatching { player.audioTracks }.getOrNull().orEmpty()
        val currentId = runCatching { player.audioTrack }.getOrDefault(-1)
        val mediaMeta = readMediaAudioTrackMeta(player.media)

        return descriptions
            .filter { it.id >= 0 }
            .mapIndexed { index, desc ->
                val meta = mediaMeta[desc.id] ?: mediaMeta.values.elementAtOrNull(index)
                val codecName = normalizeCodecName(meta?.codec)
                val lang = meta?.language?.takeIf { it.isNotBlank() && it != "und" }
                val channelsLabel = meta?.channels?.takeIf { it > 0 }?.let { "${it}ch" }
                val rawName = desc.name?.takeIf { it.isNotBlank() }
                val parts = listOfNotNull(
                    rawName ?: "Audio ${index + 1}",
                    lang?.uppercase(),
                    codecName.takeIf { it.isNotBlank() },
                    channelsLabel
                )
                MediaTrackOption(
                    id = desc.id,
                    label = parts.distinct().joinToString(" • "),
                    bitrate = meta?.bitrate ?: 0,
                    codec = codecName,
                    isSelected = desc.id == currentId
                )
            }
    }

    private fun readMediaVideoTrackMeta(media: IMedia?): Map<Int, IMedia.VideoTrack> {
        if (media == null) return emptyMap()
        val map = linkedMapOf<Int, IMedia.VideoTrack>()
        runCatching {
            val count = media.trackCount
            for (i in 0 until count) {
                val track = media.getTrack(i) ?: continue
                if (track.type == IMedia.Track.Type.Video && track is IMedia.VideoTrack) {
                    map[track.id] = track
                }
            }
        }
        return map
    }

    private fun readMediaAudioTrackMeta(media: IMedia?): Map<Int, IMedia.AudioTrack> {
        if (media == null) return emptyMap()
        val map = linkedMapOf<Int, IMedia.AudioTrack>()
        runCatching {
            val count = media.trackCount
            for (i in 0 until count) {
                val track = media.getTrack(i) ?: continue
                if (track.type == IMedia.Track.Type.Audio && track is IMedia.AudioTrack) {
                    map[track.id] = track
                }
            }
        }
        return map
    }

    private fun normalizeCodecName(codec: String?): String {
        val raw = codec?.trim()?.replace(Regex("[^A-Za-z0-9]"), "").orEmpty()
        if (raw.isEmpty()) return ""
        return when (raw.lowercase()) {
            "h264", "avc1" -> "H.264"
            "hevc", "hvc1", "hev1", "h265" -> "HEVC"
            "vp90", "vp09", "vp9" -> "VP9"
            "vp80", "vp8" -> "VP8"
            "av01", "av1" -> "AV1"
            "mp4v", "xvid", "divx", "dx50" -> "MPEG-4"
            "mpgv", "mp2v" -> "MPEG-2"
            "wmv3", "wmv2", "wmv1" -> "WMV"
            "mp4a" -> "AAC"
            "a52", "ac3" -> "AC3"
            "eac3" -> "EAC3"
            "dts" -> "DTS"
            "opus" -> "Opus"
            "vorb" -> "Vorbis"
            "flac" -> "FLAC"
            "mpga", "mp3" -> "MP3"
            else -> raw.uppercase()
        }
    }
}
