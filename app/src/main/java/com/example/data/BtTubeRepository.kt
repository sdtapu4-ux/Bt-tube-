package com.example.data

import com.example.model.AppSettings
import com.example.model.Channel
import com.example.model.NotificationItem
import com.example.model.PlaybackQuality
import com.example.model.ShortItem
import com.example.model.StudioSceneStyle
import com.example.model.ThemeMode
import com.example.model.ThumbnailArtwork
import com.example.model.UserProfile
import com.example.model.VideoCategory
import com.example.model.VideoComment
import com.example.model.VideoItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.UUID

class BtTubeRepository {

    private val _channels = MutableStateFlow<List<Channel>>(emptyList())
    val channels: StateFlow<List<Channel>> = _channels.asStateFlow()

    private val _videos = MutableStateFlow<List<VideoItem>>(emptyList())
    val videos: StateFlow<List<VideoItem>> = _videos.asStateFlow()

    private val _shorts = MutableStateFlow<List<ShortItem>>(emptyList())
    val shorts: StateFlow<List<ShortItem>> = _shorts.asStateFlow()

    private val _subscribedChannelIds = MutableStateFlow<Set<String>>(emptySet())
    val subscribedChannelIds: StateFlow<Set<String>> = _subscribedChannelIds.asStateFlow()

    private val _likedVideoIds = MutableStateFlow<Set<String>>(emptySet())
    val likedVideoIds: StateFlow<Set<String>> = _likedVideoIds.asStateFlow()

    private val _dislikedVideoIds = MutableStateFlow<Set<String>>(emptySet())
    val dislikedVideoIds: StateFlow<Set<String>> = _dislikedVideoIds.asStateFlow()

    private val _watchLaterIds = MutableStateFlow<Set<String>>(emptySet())
    val watchLaterIds: StateFlow<Set<String>> = _watchLaterIds.asStateFlow()

    private val _hiddenVideoIds = MutableStateFlow<Set<String>>(emptySet())
    val hiddenVideoIds: StateFlow<Set<String>> = _hiddenVideoIds.asStateFlow()

    private val _watchHistoryIds = MutableStateFlow<List<String>>(emptyList())
    val watchHistoryIds: StateFlow<List<String>> = _watchHistoryIds.asStateFlow()

    private val _likedShortIds = MutableStateFlow<Set<String>>(emptySet())
    val likedShortIds: StateFlow<Set<String>> = _likedShortIds.asStateFlow()

    private val _recentSearches = MutableStateFlow<List<String>>(emptyList())
    val recentSearches: StateFlow<List<String>> = _recentSearches.asStateFlow()

    private val _notifications = MutableStateFlow<List<NotificationItem>>(emptyList())
    val notifications: StateFlow<List<NotificationItem>> = _notifications.asStateFlow()

    private val _userProfile = MutableStateFlow(UserProfile())
    val userProfile: StateFlow<UserProfile> = _userProfile.asStateFlow()

    private val _settings = MutableStateFlow(AppSettings())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    fun getChannel(channelId: String): Channel {
        return _channels.value.find { it.id == channelId }
            ?: Channel(
                id = "ch_user",
                name = _userProfile.value.name,
                handle = _userProfile.value.handle,
                baseSubscriberCount = 1,
                isVerified = false,
                avatarColorHex = 0xFFD91424,
                initials = _userProfile.value.avatarInitials,
                category = VideoCategory.VIDEOS,
                tagline = "Device Media"
            )
    }

    fun getVideoById(videoId: String): VideoItem? {
        return _videos.value.find { it.id == videoId }
    }

    fun recordVideoWatch(videoId: String) {
        _watchHistoryIds.update { current ->
            listOf(videoId) + current.filterNot { it == videoId }
        }
    }

    fun toggleSubscription(channelId: String) {
        _subscribedChannelIds.update { current ->
            if (channelId in current) current - channelId else current + channelId
        }
    }

    fun toggleVideoLike(videoId: String) {
        _likedVideoIds.update { current ->
            if (videoId in current) current - videoId else current + videoId
        }
        _dislikedVideoIds.update { it - videoId }
    }

    fun toggleVideoDislike(videoId: String) {
        _dislikedVideoIds.update { current ->
            if (videoId in current) current - videoId else current + videoId
        }
        _likedVideoIds.update { it - videoId }
    }

    fun toggleWatchLater(videoId: String) {
        _watchLaterIds.update { current ->
            if (videoId in current) current - videoId else current + videoId
        }
    }

    fun hideVideo(videoId: String) {
        _hiddenVideoIds.update { it + videoId }
    }

    fun restoreHiddenVideos() {
        _hiddenVideoIds.value = emptySet()
    }

    fun toggleShortLike(shortId: String) {
        _likedShortIds.update { current ->
            if (shortId in current) current - shortId else current + shortId
        }
    }

    fun addVideoComment(videoId: String, text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return
        val profile = _userProfile.value
        val newComment = VideoComment(
            id = "vc_${UUID.randomUUID()}",
            authorName = profile.name,
            authorInitials = profile.avatarInitials,
            authorColorHex = 0xFFD91424,
            text = trimmed,
            timestamp = "Just now",
            likes = 0
        )
        _videos.update { list ->
            list.map { video ->
                if (video.id == videoId) {
                    video.copy(comments = listOf(newComment) + video.comments)
                } else {
                    video
                }
            }
        }
    }

    fun addShortComment(shortId: String, text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return
        val profile = _userProfile.value
        val newComment = VideoComment(
            id = "sc_${UUID.randomUUID()}",
            authorName = profile.name,
            authorInitials = profile.avatarInitials,
            authorColorHex = 0xFFD91424,
            text = trimmed,
            timestamp = "Just now",
            likes = 0
        )
        _shorts.update { list ->
            list.map { short ->
                if (short.id == shortId) {
                    short.copy(
                        baseCommentsCount = short.baseCommentsCount + 1,
                        comments = listOf(newComment) + short.comments
                    )
                } else {
                    short
                }
            }
        }
    }

    fun addRecentSearch(query: String) {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return
        _recentSearches.update { current ->
            (listOf(trimmed) + current.filterNot { it.equals(trimmed, ignoreCase = true) }).take(8)
        }
    }

    fun removeRecentSearch(query: String) {
        _recentSearches.update { current -> current.filterNot { it == query } }
    }

    fun clearRecentSearches() {
        _recentSearches.value = emptyList()
    }

    fun clearWatchHistory() {
        _watchHistoryIds.value = emptyList()
    }

    fun markNotificationRead(notificationId: String) {
        _notifications.update { list ->
            list.map { if (it.id == notificationId) it.copy(isRead = true) else it }
        }
    }

    fun markAllNotificationsRead() {
        _notifications.update { list -> list.map { it.copy(isRead = true) } }
    }

    fun applyGmailUser(
        uid: String,
        email: String?,
        displayName: String?,
        photoUrl: String?
    ) {
        val cleanEmail = (email?.trim()?.takeIf { it.isNotEmpty() } ?: "gmail_user@gmail.com")
        val resolvedName = displayName?.trim()?.takeIf { it.isNotEmpty() }
            ?: cleanEmail.substringBefore("@").replaceFirstChar { it.uppercase() }
        val initials = resolvedName.split(" ")
            .filter { it.isNotBlank() }
            .take(2)
            .joinToString("") { it.first().uppercase() }
            .ifEmpty { "BT" }
        val handle = "@" + resolvedName.lowercase().replace(Regex("[^a-z0-9]"), "")
            .ifEmpty { "btmember" }
        _userProfile.value = UserProfile(
            isSignedIn = true,
            userId = uid,
            name = resolvedName,
            handle = handle,
            email = cleanEmail,
            photoUrl = photoUrl,
            avatarInitials = initials,
            bio = "Signed in with Gmail ($cleanEmail) • Cloud settings synced"
        )
    }

    fun applyRemoteAccount(account: FirestoreUserAccount) {
        val resolvedQuality = enumValues<PlaybackQuality>()
            .firstOrNull { it.name == account.defaultQuality }
            ?: _settings.value.defaultQuality
        _settings.update {
            it.copy(
                streamOnMobileData = account.streamOnMobileData,
                defaultQuality = resolvedQuality,
                autoplayNext = account.autoplayNext,
                notificationsEnabled = account.notificationsEnabled
            )
        }
        applyGmailUser(
            uid = account.userId,
            email = account.email,
            displayName = account.displayName,
            photoUrl = account.photoUrl
        )
    }

    fun signIn(email: String, displayName: String? = null) {
        applyGmailUser(
            uid = "local_user",
            email = email,
            displayName = displayName,
            photoUrl = null
        )
    }

    fun signOut() {
        _userProfile.value = UserProfile()
    }

    fun updateThemeMode(mode: ThemeMode) {
        _settings.update { it.copy(themeMode = mode) }
    }

    fun toggleAutoplayNext() {
        _settings.update { it.copy(autoplayNext = !it.autoplayNext) }
    }

    fun toggleNotificationsEnabled() {
        _settings.update { it.copy(notificationsEnabled = !it.notificationsEnabled) }
    }

    fun toggleStreamOnMobileData() {
        _settings.update { it.copy(streamOnMobileData = !it.streamOnMobileData) }
    }

    fun updateDefaultQuality(quality: PlaybackQuality) {
        _settings.update { it.copy(defaultQuality = quality) }
    }

    fun publishVideo(
        title: String,
        description: String,
        category: VideoCategory,
        sceneStyle: StudioSceneStyle,
        isShort: Boolean,
        videoUri: String? = null
    ): String {
        val profile = _userProfile.value
        val creatorChannelId = ensureUserChannel(profile, category)
        val badge = title.trim().split(" ").take(2).joinToString(" ").uppercase().ifEmpty { "NEW" }
        val artwork = if (videoUri != null) {
            ThumbnailArtwork.LocalVideoFrame(videoUri)
        } else {
            ThumbnailArtwork.VectorScene(sceneStyle, badge)
        }
        return if (isShort) {
            val newId = "short_user_${UUID.randomUUID()}"
            val newShort = ShortItem(
                id = newId,
                title = title.trim(),
                caption = description.trim().ifEmpty { "Created in BT Tube Studio #BTTube" },
                channelId = creatorChannelId,
                baseLikes = 1,
                baseCommentsCount = 0,
                baseSharesCount = 0,
                audioTrack = "${profile.name} — Original Audio",
                durationSeconds = 30,
                artwork = artwork,
                isUserCreated = true,
                videoUri = videoUri
            )
            _shorts.update { listOf(newShort) + it }
            newId
        } else {
            val newId = "vid_user_${UUID.randomUUID()}"
            val newVideo = VideoItem(
                id = newId,
                title = title.trim(),
                description = description.trim().ifEmpty { "Published directly from BT Tube Studio." },
                channelId = creatorChannelId,
                category = category,
                viewsCount = 1,
                uploadDate = "Just now",
                durationSeconds = 245,
                is4K = true,
                baseLikes = 1,
                artwork = artwork,
                tags = listOf(category.label, "CreatorStudio", "BTTube"),
                isUserCreated = true,
                videoUri = videoUri,
                isAudioOnly = (category == VideoCategory.AUDIO)
            )
            _videos.update { listOf(newVideo) + it }
            newId
        }
    }

    fun importDeviceVideos(imported: List<ImportedDeviceVideo>): Int {
        if (imported.isEmpty()) return 0
        val existingUris = _videos.value.mapNotNull { it.videoUri }.toSet()
        val fresh = imported.filter { it.uriString !in existingUris }
        if (fresh.isEmpty()) return 0

        val hasVideos = fresh.any { it.mediaType == DeviceMediaType.VIDEO }
        val hasAudio = fresh.any { it.mediaType == DeviceMediaType.AUDIO }
        val deviceVideoChannelId = if (hasVideos) ensureDeviceChannel() else "ch_device_videos"
        val deviceAudioChannelId = if (hasAudio) ensureDeviceAudioChannel() else "ch_device_audio"

        val newItems = fresh.map { item ->
            val isAudio = item.mediaType == DeviceMediaType.AUDIO
            val resolvedCategory = when {
                isAudio -> VideoCategory.AUDIO
                item.durationSeconds <= 60 -> VideoCategory.SHORTS
                else -> VideoCategory.VIDEOS
            }
            val artwork = if (isAudio) {
                ThumbnailArtwork.AudioTrackArt(
                    albumArtUri = item.albumArtUri,
                    artist = item.artistOrFolder.ifBlank { "Device Audio" }
                )
            } else {
                ThumbnailArtwork.LocalVideoFrame(item.uriString)
            }
            val qLabel = if (isAudio) "Audio" else item.qualityLabel.ifBlank { "SD" }
            val extraTags = if (isAudio) {
                listOf("Audio", "Music", "Device")
            } else {
                buildList {
                    add("Video")
                    add("Device")
                    if (qLabel.contains("SD", ignoreCase = true)) add("SD")
                    if (qLabel.isNotBlank()) add(qLabel)
                }
            }
            VideoItem(
                id = if (isAudio) "aud_device_${UUID.randomUUID()}" else "vid_device_${UUID.randomUUID()}",
                title = item.title,
                description = if (isAudio) {
                    "Device audio song${if (item.artistOrFolder.isNotBlank()) " • ${item.artistOrFolder}" else ""}"
                } else {
                    "Device video ($qLabel) • Play as Video or Background Audio"
                },
                channelId = if (isAudio) deviceAudioChannelId else deviceVideoChannelId,
                category = resolvedCategory,
                viewsCount = 1,
                uploadDate = if (isAudio) "Device Audio" else "Device Video ($qLabel)",
                durationSeconds = item.durationSeconds,
                is4K = qLabel.contains("4K"),
                baseLikes = 0,
                artwork = artwork,
                tags = extraTags,
                isUserCreated = true,
                videoUri = item.uriString,
                isAudioOnly = isAudio,
                qualityLabel = qLabel
            )
        }

        val newShorts = fresh
            .filter { it.mediaType == DeviceMediaType.VIDEO && it.durationSeconds <= 60 }
            .map { item ->
                ShortItem(
                    id = "short_device_${UUID.randomUUID()}",
                    title = item.title,
                    caption = "Device Short Video",
                    channelId = deviceVideoChannelId,
                    baseLikes = 0,
                    baseCommentsCount = 0,
                    baseSharesCount = 0,
                    audioTrack = "Original Video Audio",
                    durationSeconds = item.durationSeconds.coerceIn(5, 60),
                    artwork = ThumbnailArtwork.LocalVideoFrame(item.uriString),
                    isUserCreated = true,
                    videoUri = item.uriString
                )
            }

        _videos.update { newItems + it }
        if (newShorts.isNotEmpty()) {
            _shorts.update { newShorts + it }
        }

        val videoCount = fresh.count { it.mediaType == DeviceMediaType.VIDEO }
        val audioCount = fresh.count { it.mediaType == DeviceMediaType.AUDIO }
        val firstAdded = newItems.firstOrNull()
        if (firstAdded != null) {
            val summaryNotif = NotificationItem(
                id = "notif_${UUID.randomUUID()}",
                title = "Loaded $videoCount videos & $audioCount audio songs",
                subtitle = "Tap to play from your device library",
                timeAgo = "Just now",
                videoId = firstAdded.id,
                channelId = firstAdded.channelId
            )
            _notifications.update { listOf(summaryNotif) + it }
        }
        return newItems.size
    }

    private fun ensureDeviceChannel(): String {
        val deviceChannelId = "ch_device_videos"
        if (_channels.value.none { it.id == deviceChannelId }) {
            _channels.update {
                listOf(
                    Channel(
                        id = deviceChannelId,
                        name = "My Phone Videos",
                        handle = "@device_videos",
                        baseSubscriberCount = 1,
                        isVerified = true,
                        avatarColorHex = 0xFFD91424,
                        initials = "MV",
                        category = VideoCategory.VIDEOS,
                        tagline = "Videos from your Android device"
                    )
                ) + it
            }
            _subscribedChannelIds.update { it + deviceChannelId }
        }
        return deviceChannelId
    }

    private fun ensureDeviceAudioChannel(): String {
        val deviceAudioId = "ch_device_audio"
        if (_channels.value.none { it.id == deviceAudioId }) {
            _channels.update {
                listOf(
                    Channel(
                        id = deviceAudioId,
                        name = "My Phone Audio",
                        handle = "@device_audio",
                        baseSubscriberCount = 1,
                        isVerified = true,
                        avatarColorHex = 0xFF991B1B,
                        initials = "MA",
                        category = VideoCategory.AUDIO,
                        tagline = "Songs & audio files on your Android device"
                    )
                ) + it
            }
            _subscribedChannelIds.update { it + deviceAudioId }
        }
        return deviceAudioId
    }

    private fun ensureUserChannel(profile: UserProfile, category: VideoCategory): String {
        val userChannelId = "ch_me"
        val existing = _channels.value.any { it.id == userChannelId }
        val name = if (profile.isSignedIn) profile.name else "My Library"
        val handle = if (profile.isSignedIn) profile.handle else "@mylibrary"
        val initials = if (profile.isSignedIn) profile.avatarInitials else "ML"
        if (!existing) {
            _channels.update {
                listOf(
                    Channel(
                        id = userChannelId,
                        name = name,
                        handle = handle,
                        baseSubscriberCount = 1,
                        isVerified = false,
                        avatarColorHex = 0xFFD91424,
                        initials = initials,
                        category = category,
                        tagline = "Device Library"
                    )
                ) + it
            }
            _subscribedChannelIds.update { it + userChannelId }
        }
        return userChannelId
    }
}
