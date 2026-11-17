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

    private val _channels = MutableStateFlow(SampleCatalog.channels)
    val channels: StateFlow<List<Channel>> = _channels.asStateFlow()

    private val _videos = MutableStateFlow(SampleCatalog.videos)
    val videos: StateFlow<List<VideoItem>> = _videos.asStateFlow()

    private val _shorts = MutableStateFlow(SampleCatalog.shorts)
    val shorts: StateFlow<List<ShortItem>> = _shorts.asStateFlow()

    private val _subscribedChannelIds = MutableStateFlow(
        setOf("ch_apex_drive", "ch_nordic_lens", "ch_pulse_synth", "ch_cosmos_brief")
    )
    val subscribedChannelIds: StateFlow<Set<String>> = _subscribedChannelIds.asStateFlow()

    private val _likedVideoIds = MutableStateFlow(setOf("vid_2"))
    val likedVideoIds: StateFlow<Set<String>> = _likedVideoIds.asStateFlow()

    private val _dislikedVideoIds = MutableStateFlow(emptySet<String>())
    val dislikedVideoIds: StateFlow<Set<String>> = _dislikedVideoIds.asStateFlow()

    private val _watchLaterIds = MutableStateFlow(setOf("vid_4", "vid_5"))
    val watchLaterIds: StateFlow<Set<String>> = _watchLaterIds.asStateFlow()

    private val _hiddenVideoIds = MutableStateFlow(emptySet<String>())
    val hiddenVideoIds: StateFlow<Set<String>> = _hiddenVideoIds.asStateFlow()

    private val _watchHistoryIds = MutableStateFlow(listOf("vid_1", "vid_3"))
    val watchHistoryIds: StateFlow<List<String>> = _watchHistoryIds.asStateFlow()

    private val _likedShortIds = MutableStateFlow(setOf("short_2"))
    val likedShortIds: StateFlow<Set<String>> = _likedShortIds.asStateFlow()

    private val _recentSearches = MutableStateFlow(SampleCatalog.initialRecentSearches)
    val recentSearches: StateFlow<List<String>> = _recentSearches.asStateFlow()

    private val _notifications = MutableStateFlow(SampleCatalog.initialNotifications)
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
                baseSubscriberCount = 128,
                isVerified = false,
                avatarColorHex = 0xFFD91424,
                initials = _userProfile.value.avatarInitials,
                category = VideoCategory.TECH,
                tagline = "BT Tube Creator"
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

    fun signIn(email: String, displayName: String? = null) {
        val cleanEmail = email.trim().ifEmpty { "creator@bttube.app" }
        val resolvedName = displayName?.trim()?.takeIf { it.isNotEmpty() }
            ?: cleanEmail.substringBefore("@").replaceFirstChar { it.uppercase() }
        val initials = resolvedName.split(" ")
            .filter { it.isNotBlank() }
            .take(2)
            .joinToString("") { it.first().uppercase() }
            .ifEmpty { "BT" }
        val handle = "@" + resolvedName.lowercase().replace(Regex("[^a-z0-9]"), "")
            .ifEmpty { "btcreator" }
        _userProfile.value = UserProfile(
            isSignedIn = true,
            name = resolvedName,
            handle = handle,
            email = cleanEmail,
            avatarInitials = initials,
            bio = "BT Tube member • Streaming in crisp HD"
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

    fun updateDefaultQuality(quality: PlaybackQuality) {
        _settings.update { it.copy(defaultQuality = quality) }
    }

    fun publishVideo(
        title: String,
        description: String,
        category: VideoCategory,
        sceneStyle: StudioSceneStyle,
        isShort: Boolean
    ): String {
        val profile = _userProfile.value
        val creatorChannelId = ensureUserChannel(profile, category)
        val badge = title.trim().split(" ").take(2).joinToString(" ").uppercase().ifEmpty { "NEW" }
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
                artwork = ThumbnailArtwork.VectorScene(sceneStyle, badge),
                isUserCreated = true
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
                artwork = ThumbnailArtwork.VectorScene(sceneStyle, badge),
                tags = listOf(category.label, "CreatorStudio", "BTTube"),
                isUserCreated = true
            )
            _videos.update { listOf(newVideo) + it }
            newId
        }
    }

    private fun ensureUserChannel(profile: UserProfile, category: VideoCategory): String {
        val userChannelId = "ch_me"
        val existing = _channels.value.any { it.id == userChannelId }
        val name = if (profile.isSignedIn) profile.name else "Your Studio"
        val handle = if (profile.isSignedIn) profile.handle else "@yourstudio"
        val initials = if (profile.isSignedIn) profile.avatarInitials else "YS"
        if (!existing) {
            _channels.update {
                listOf(
                    Channel(
                        id = userChannelId,
                        name = name,
                        handle = handle,
                        baseSubscriberCount = 128,
                        isVerified = false,
                        avatarColorHex = 0xFFD91424,
                        initials = initials,
                        category = category,
                        tagline = "BT Tube Creator Studio"
                    )
                ) + it
            }
            _subscribedChannelIds.update { it + userChannelId }
        }
        return userChannelId
    }
}
