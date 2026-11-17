package com.example.model

import androidx.annotation.DrawableRes

enum class VideoCategory(val label: String) {
    ALL("All"),
    TECH("Tech"),
    MUSIC("Music"),
    GAMING("Gaming"),
    NATURE("Nature"),
    DESIGN("Design"),
    SCIENCE("Science"),
    PODCASTS("Podcasts")
}

enum class ThemeMode(val label: String) {
    SYSTEM("System"),
    LIGHT("Light"),
    DARK("Dark")
}

enum class PlaybackQuality(val label: String) {
    AUTO("Auto"),
    HD_1080("1080p HD"),
    HD_720("720p"),
    SD_480("480p")
}

enum class SearchFilter(val label: String) {
    ALL("All"),
    VIDEOS("Videos"),
    SHORTS("Shorts"),
    CHANNELS("Channels")
}

enum class StudioSceneStyle(val label: String, val startHex: Long, val endHex: Long, val accentHex: Long) {
    CRIMSON_STUDIO("Crimson Studio", 0xFF280509, 0xFFD91424, 0xFFFF8A93),
    OBSIDIAN_TECH("Obsidian Tech", 0xFF0E1320, 0xFF1E3A8A, 0xFF38BDF8),
    EMERALD_EXPEDITION("Emerald Wild", 0xFF052E24, 0xFF047857, 0xFF6EE7B7),
    AMBER_ACOUSTIC("Amber Sound", 0xFF291405, 0xFFB45309, 0xFFFCD34D),
    NEON_CYBER("Neon Horizon", 0xFF1A0B2E, 0xFFBE185D, 0xFF22D3EE)
}

sealed interface ThumbnailArtwork {
    data class PhotoDrawable(@param:DrawableRes val resId: Int) : ThumbnailArtwork
    data class VectorScene(val style: StudioSceneStyle, val badgeText: String) : ThumbnailArtwork
    data class LocalVideoFrame(val uriString: String) : ThumbnailArtwork
}

data class Channel(
    val id: String,
    val name: String,
    val handle: String,
    val baseSubscriberCount: Int,
    val isVerified: Boolean,
    val avatarColorHex: Long,
    val initials: String,
    val category: VideoCategory,
    val tagline: String
)

data class VideoComment(
    val id: String,
    val authorName: String,
    val authorInitials: String,
    val authorColorHex: Long,
    val text: String,
    val timestamp: String,
    val likes: Int,
    val isLikedByUser: Boolean = false
)

data class VideoItem(
    val id: String,
    val title: String,
    val description: String,
    val channelId: String,
    val category: VideoCategory,
    val viewsCount: Long,
    val uploadDate: String,
    val durationSeconds: Int,
    val isTrending: Boolean = false,
    val trendingRank: Int? = null,
    val isLive: Boolean = false,
    val is4K: Boolean = false,
    val baseLikes: Int,
    val artwork: ThumbnailArtwork,
    val tags: List<String> = emptyList(),
    val comments: List<VideoComment> = emptyList(),
    val isUserCreated: Boolean = false,
    val videoUri: String? = null
)

data class ShortItem(
    val id: String,
    val title: String,
    val caption: String,
    val channelId: String,
    val baseLikes: Int,
    val baseCommentsCount: Int,
    val baseSharesCount: Int,
    val audioTrack: String,
    val durationSeconds: Int,
    val artwork: ThumbnailArtwork,
    val comments: List<VideoComment> = emptyList(),
    val isUserCreated: Boolean = false,
    val videoUri: String? = null
)

data class NotificationItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val timeAgo: String,
    val videoId: String,
    val channelId: String,
    val isRead: Boolean = false
)

data class UserProfile(
    val isSignedIn: Boolean = false,
    val name: String = "Guest Viewer",
    val handle: String = "@guest_bt",
    val email: String = "",
    val avatarInitials: String = "BT",
    val bio: String = "Sign in to sync your watch history, liked videos, and creator studio."
)

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val autoplayNext: Boolean = true,
    val notificationsEnabled: Boolean = true,
    val streamOnMobileData: Boolean = true,
    val defaultQuality: PlaybackQuality = PlaybackQuality.HD_1080
)

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}
