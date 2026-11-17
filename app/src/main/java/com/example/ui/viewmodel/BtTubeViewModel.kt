package com.example.ui.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.BtTubeRepository
import com.example.data.LocalVideoScanner
import com.example.model.AppSettings
import com.example.model.Channel
import com.example.model.NotificationItem
import com.example.model.PlaybackQuality
import com.example.model.SearchFilter
import com.example.model.ShortItem
import com.example.model.StudioSceneStyle
import com.example.model.ThemeMode
import com.example.model.UiState
import com.example.model.UserProfile
import com.example.model.VideoCategory
import com.example.model.VideoItem
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HomeFeedData(
    val selectedCategory: VideoCategory = VideoCategory.ALL,
    val trendingVideos: List<VideoItem> = emptyList(),
    val recommendedVideos: List<VideoItem> = emptyList(),
    val channelsById: Map<String, Channel> = emptyMap(),
    val subscribedChannelIds: Set<String> = emptySet(),
    val watchLaterIds: Set<String> = emptySet(),
    val unreadNotificationsCount: Int = 0,
    val deviceVideosCount: Int = 0
)

data class PlayerUiState(
    val video: VideoItem? = null,
    val channel: Channel? = null,
    val relatedVideos: List<VideoItem> = emptyList(),
    val channelsById: Map<String, Channel> = emptyMap(),
    val isSubscribed: Boolean = false,
    val isLiked: Boolean = false,
    val isDisliked: Boolean = false,
    val isWatchLater: Boolean = false,
    val isPlaying: Boolean = true,
    val isBuffering: Boolean = false,
    val isFullscreen: Boolean = false,
    val currentPositionSeconds: Float = 0f,
    val playbackSpeed: Float = 1.0f,
    val selectedQuality: PlaybackQuality = PlaybackQuality.HD_1080
)

data class SearchUiState(
    val query: String = "",
    val filter: SearchFilter = SearchFilter.ALL,
    val recentSearches: List<String> = emptyList(),
    val suggestions: List<String> = emptyList(),
    val matchingVideos: List<VideoItem> = emptyList(),
    val matchingShorts: List<ShortItem> = emptyList(),
    val matchingChannels: List<Channel> = emptyList(),
    val channelsById: Map<String, Channel> = emptyMap(),
    val subscribedChannelIds: Set<String> = emptySet(),
    val hasSubmittedQuery: Boolean = false
)

data class SubscriptionsUiState(
    val subscribedChannels: List<Channel> = emptyList(),
    val allChannels: List<Channel> = emptyList(),
    val selectedChannelId: String? = null,
    val feedVideos: List<VideoItem> = emptyList(),
    val channelsById: Map<String, Channel> = emptyMap(),
    val watchLaterIds: Set<String> = emptySet(),
    val unreadNotificationsCount: Int = 0
)

data class ProfileUiState(
    val userProfile: UserProfile = UserProfile(),
    val settings: AppSettings = AppSettings(),
    val watchHistoryVideos: List<VideoItem> = emptyList(),
    val likedVideos: List<VideoItem> = emptyList(),
    val watchLaterVideos: List<VideoItem> = emptyList(),
    val uploadedVideos: List<VideoItem> = emptyList(),
    val deviceVideos: List<VideoItem> = emptyList(),
    val channelsById: Map<String, Channel> = emptyMap()
)

class BtTubeViewModel(
    private val repository: BtTubeRepository = BtTubeRepository()
) : ViewModel() {

    private val _selectedCategory = MutableStateFlow(VideoCategory.ALL)
    private val _isHomeLoading = MutableStateFlow(false)
    private val _homeErrorMessage = MutableStateFlow<String?>(null)

    private val _searchQuery = MutableStateFlow("")
    private val _searchFilter = MutableStateFlow(SearchFilter.ALL)
    private val _hasSubmittedSearch = MutableStateFlow(false)

    private val _selectedSubscriptionChannelId = MutableStateFlow<String?>(null)
    private val _isImportingVideos = MutableStateFlow(false)
    val isImportingVideos: StateFlow<Boolean> = _isImportingVideos.asStateFlow()

    private val _activeVideoId = MutableStateFlow<String?>(null)
    private val _isPlaying = MutableStateFlow(true)
    private val _isBuffering = MutableStateFlow(false)
    private val _isFullscreen = MutableStateFlow(false)
    private val _currentPositionSeconds = MutableStateFlow(0f)
    private val _playbackSpeed = MutableStateFlow(1.0f)

    val settings: StateFlow<AppSettings> = repository.settings
    val notifications: StateFlow<List<NotificationItem>> = repository.notifications
    val shorts: StateFlow<List<ShortItem>> = repository.shorts
    val likedShortIds: StateFlow<Set<String>> = repository.likedShortIds
    val subscribedChannelIds: StateFlow<Set<String>> = repository.subscribedChannelIds
    val channels: StateFlow<List<Channel>> = repository.channels

    private val _channelsMap = repository.channels.combine(repository.userProfile) { list, _ ->
        list.associateBy { it.id }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    val homeState: StateFlow<UiState<HomeFeedData>> = combine(
        repository.videos,
        repository.channels,
        _selectedCategory,
        repository.subscribedChannelIds,
        repository.watchLaterIds,
        repository.hiddenVideoIds,
        repository.notifications,
        _isHomeLoading,
        _homeErrorMessage
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        val videos = values[0] as List<VideoItem>
        @Suppress("UNCHECKED_CAST")
        val channelList = values[1] as List<Channel>
        val category = values[2] as VideoCategory
        @Suppress("UNCHECKED_CAST")
        val subIds = values[3] as Set<String>
        @Suppress("UNCHECKED_CAST")
        val watchLater = values[4] as Set<String>
        @Suppress("UNCHECKED_CAST")
        val hiddenIds = values[5] as Set<String>
        @Suppress("UNCHECKED_CAST")
        val notifs = values[6] as List<NotificationItem>
        val isLoading = values[7] as Boolean
        val errorMsg = values[8] as String?

        when {
            isLoading -> UiState.Loading
            errorMsg != null -> UiState.Error(errorMsg)
            else -> {
                val visibleVideos = videos.filterNot { it.id in hiddenIds }
                val filteredByCategory = if (category == VideoCategory.ALL) {
                    visibleVideos
                } else {
                    visibleVideos.filter { it.category == category }
                }
                val trending = visibleVideos
                    .filter { it.isTrending }
                    .sortedBy { it.trendingRank ?: Int.MAX_VALUE }
                UiState.Success(
                    HomeFeedData(
                        selectedCategory = category,
                        trendingVideos = trending,
                        recommendedVideos = filteredByCategory,
                        channelsById = channelList.associateBy { it.id },
                        subscribedChannelIds = subIds,
                        watchLaterIds = watchLater,
                        unreadNotificationsCount = notifs.count { !it.isRead },
                        deviceVideosCount = visibleVideos.count { it.videoUri != null }
                    )
                )
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = UiState.Loading
    )

    val playerState: StateFlow<PlayerUiState> = combine(
        _activeVideoId,
        repository.videos,
        repository.channels,
        repository.subscribedChannelIds,
        repository.likedVideoIds,
        repository.dislikedVideoIds,
        repository.watchLaterIds,
        _isPlaying,
        _isBuffering,
        _isFullscreen,
        _currentPositionSeconds,
        _playbackSpeed,
        repository.settings
    ) { values ->
        val activeId = values[0] as String?
        @Suppress("UNCHECKED_CAST")
        val allVideos = values[1] as List<VideoItem>
        @Suppress("UNCHECKED_CAST")
        val allChannels = values[2] as List<Channel>
        @Suppress("UNCHECKED_CAST")
        val subIds = values[3] as Set<String>
        @Suppress("UNCHECKED_CAST")
        val likedIds = values[4] as Set<String>
        @Suppress("UNCHECKED_CAST")
        val dislikedIds = values[5] as Set<String>
        @Suppress("UNCHECKED_CAST")
        val watchLater = values[6] as Set<String>
        val playing = values[7] as Boolean
        val buffering = values[8] as Boolean
        val fullscreen = values[9] as Boolean
        val position = values[10] as Float
        val speed = values[11] as Float
        val appSettings = values[12] as AppSettings

        val channelMap = allChannels.associateBy { it.id }
        val video = allVideos.find { it.id == activeId } ?: allVideos.firstOrNull()
        val channel = video?.let { channelMap[it.channelId] }
        val related = if (video != null) {
            allVideos.filter { it.id != video.id }
                .sortedByDescending { it.category == video.category }
        } else {
            emptyList()
        }

        PlayerUiState(
            video = video,
            channel = channel,
            relatedVideos = related,
            channelsById = channelMap,
            isSubscribed = video?.channelId in subIds,
            isLiked = video?.id in likedIds,
            isDisliked = video?.id in dislikedIds,
            isWatchLater = video?.id in watchLater,
            isPlaying = playing,
            isBuffering = buffering,
            isFullscreen = fullscreen,
            currentPositionSeconds = position,
            playbackSpeed = speed,
            selectedQuality = appSettings.defaultQuality
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = PlayerUiState()
    )

    val searchState: StateFlow<SearchUiState> = combine(
        _searchQuery,
        _searchFilter,
        _hasSubmittedSearch,
        repository.recentSearches,
        repository.videos,
        repository.shorts,
        repository.channels,
        repository.subscribedChannelIds
    ) { values ->
        val query = values[0] as String
        val filter = values[1] as SearchFilter
        val submitted = values[2] as Boolean
        @Suppress("UNCHECKED_CAST")
        val recents = values[3] as List<String>
        @Suppress("UNCHECKED_CAST")
        val allVideos = values[4] as List<VideoItem>
        @Suppress("UNCHECKED_CAST")
        val allShorts = values[5] as List<ShortItem>
        @Suppress("UNCHECKED_CAST")
        val allChannels = values[6] as List<Channel>
        @Suppress("UNCHECKED_CAST")
        val subIds = values[7] as Set<String>

        val channelMap = allChannels.associateBy { it.id }
        val trimmed = query.trim()

        val suggestions = if (trimmed.isEmpty()) {
            listOf("Hyper-EV 4K", "Iceland Canyon", "Analog Synths", "Path Tracing", "Motion Systems", "Orbital Live")
        } else {
            buildSet {
                allVideos.forEach { video ->
                    if (video.title.contains(trimmed, ignoreCase = true)) add(video.title)
                    video.tags.filter { it.contains(trimmed, ignoreCase = true) }.forEach { add(it) }
                }
                allChannels.filter { it.name.contains(trimmed, ignoreCase = true) }.forEach { add(it.name) }
            }.take(6)
        }

        val matchedVideos = if (trimmed.isEmpty()) {
            emptyList()
        } else {
            allVideos.filter { v ->
                val ch = channelMap[v.channelId]
                v.title.contains(trimmed, ignoreCase = true) ||
                    v.description.contains(trimmed, ignoreCase = true) ||
                    v.category.label.contains(trimmed, ignoreCase = true) ||
                    v.tags.any { it.contains(trimmed, ignoreCase = true) } ||
                    (ch?.name?.contains(trimmed, ignoreCase = true) == true)
            }
        }

        val matchedShorts = if (trimmed.isEmpty()) {
            emptyList()
        } else {
            allShorts.filter { s ->
                val ch = channelMap[s.channelId]
                s.title.contains(trimmed, ignoreCase = true) ||
                    s.caption.contains(trimmed, ignoreCase = true) ||
                    (ch?.name?.contains(trimmed, ignoreCase = true) == true)
            }
        }

        val matchedChannels = if (trimmed.isEmpty()) {
            emptyList()
        } else {
            allChannels.filter { ch ->
                ch.name.contains(trimmed, ignoreCase = true) ||
                    ch.handle.contains(trimmed, ignoreCase = true) ||
                    ch.category.label.contains(trimmed, ignoreCase = true)
            }
        }

        SearchUiState(
            query = query,
            filter = filter,
            recentSearches = recents,
            suggestions = suggestions,
            matchingVideos = matchedVideos,
            matchingShorts = matchedShorts,
            matchingChannels = matchedChannels,
            channelsById = channelMap,
            subscribedChannelIds = subIds,
            hasSubmittedQuery = submitted
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SearchUiState()
    )

    val subscriptionsState: StateFlow<SubscriptionsUiState> = combine(
        repository.channels,
        repository.subscribedChannelIds,
        repository.videos,
        _selectedSubscriptionChannelId,
        repository.watchLaterIds,
        repository.notifications
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        val allChannels = values[0] as List<Channel>
        @Suppress("UNCHECKED_CAST")
        val subIds = values[1] as Set<String>
        @Suppress("UNCHECKED_CAST")
        val allVideos = values[2] as List<VideoItem>
        val selectedChId = values[3] as String?
        @Suppress("UNCHECKED_CAST")
        val watchLater = values[4] as Set<String>
        @Suppress("UNCHECKED_CAST")
        val notifs = values[5] as List<NotificationItem>

        val subChannels = allChannels.filter { it.id in subIds }
        val effectiveFilter = if (selectedChId != null && selectedChId in subIds) selectedChId else null
        val feed = allVideos.filter { video ->
            video.channelId in subIds && (effectiveFilter == null || video.channelId == effectiveFilter)
        }

        SubscriptionsUiState(
            subscribedChannels = subChannels,
            allChannels = allChannels,
            selectedChannelId = effectiveFilter,
            feedVideos = feed,
            channelsById = allChannels.associateBy { it.id },
            watchLaterIds = watchLater,
            unreadNotificationsCount = notifs.count { !it.isRead }
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SubscriptionsUiState()
    )

    val profileState: StateFlow<ProfileUiState> = combine(
        repository.userProfile,
        repository.settings,
        repository.watchHistoryIds,
        repository.likedVideoIds,
        repository.watchLaterIds,
        repository.videos,
        repository.channels
    ) { values ->
        val profile = values[0] as UserProfile
        val appSettings = values[1] as AppSettings
        @Suppress("UNCHECKED_CAST")
        val historyIds = values[2] as List<String>
        @Suppress("UNCHECKED_CAST")
        val likedIds = values[3] as Set<String>
        @Suppress("UNCHECKED_CAST")
        val watchLater = values[4] as Set<String>
        @Suppress("UNCHECKED_CAST")
        val allVideos = values[5] as List<VideoItem>
        @Suppress("UNCHECKED_CAST")
        val allChannels = values[6] as List<Channel>

        val videosById = allVideos.associateBy { it.id }

        ProfileUiState(
            userProfile = profile,
            settings = appSettings,
            watchHistoryVideos = historyIds.mapNotNull { videosById[it] },
            likedVideos = allVideos.filter { it.id in likedIds },
            watchLaterVideos = allVideos.filter { it.id in watchLater },
            uploadedVideos = allVideos.filter { it.isUserCreated && it.videoUri == null },
            deviceVideos = allVideos.filter { it.videoUri != null },
            channelsById = allChannels.associateBy { it.id }
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ProfileUiState()
    )

    fun selectCategory(category: VideoCategory) {
        _selectedCategory.value = category
    }

    fun refreshHomeFeed() {
        viewModelScope.launch {
            _homeErrorMessage.value = null
            _isHomeLoading.value = true
            delay(350)
            _isHomeLoading.value = false
        }
    }

    fun restoreHiddenVideos() {
        repository.restoreHiddenVideos()
    }

    fun openVideo(videoId: String) {
        _activeVideoId.value = videoId
        _currentPositionSeconds.value = 0f
        _isPlaying.value = true
        _isBuffering.value = false
        repository.recordVideoWatch(videoId)
    }

    fun togglePlayPause() {
        _isPlaying.update { !it }
    }

    fun setPlaying(playing: Boolean) {
        _isPlaying.value = playing
    }

    fun seekTo(seconds: Float) {
        val duration = playerState.value.video?.durationSeconds?.toFloat() ?: 1f
        _currentPositionSeconds.value = seconds.coerceIn(0f, duration)
    }

    fun seekBy(deltaSeconds: Float) {
        val duration = playerState.value.video?.durationSeconds?.toFloat() ?: 1f
        _currentPositionSeconds.update { (it + deltaSeconds).coerceIn(0f, duration) }
    }

    fun syncPlaybackPosition(seconds: Float) {
        _currentPositionSeconds.value = seconds.coerceAtLeast(0f)
    }

    fun advancePlayback(deltaSeconds: Float) {
        val video = playerState.value.video ?: return
        val maxDuration = video.durationSeconds.toFloat().coerceAtLeast(1f)
        val next = _currentPositionSeconds.value + deltaSeconds
        if (next >= maxDuration) {
            if (repository.settings.value.autoplayNext) {
                val nextVideo = playerState.value.relatedVideos.firstOrNull()
                if (nextVideo != null) {
                    openVideo(nextVideo.id)
                    return
                }
            }
            _currentPositionSeconds.value = maxDuration
            _isPlaying.value = false
        } else {
            _currentPositionSeconds.value = next
        }
    }

    fun setPlaybackSpeed(speed: Float) {
        _playbackSpeed.value = speed
    }

    fun setFullscreen(fullscreen: Boolean) {
        _isFullscreen.value = fullscreen
    }

    fun toggleFullscreen() {
        _isFullscreen.update { !it }
    }

    fun toggleSubscription(channelId: String) {
        repository.toggleSubscription(channelId)
    }

    fun toggleVideoLike(videoId: String) {
        repository.toggleVideoLike(videoId)
    }

    fun toggleVideoDislike(videoId: String) {
        repository.toggleVideoDislike(videoId)
    }

    fun toggleWatchLater(videoId: String) {
        repository.toggleWatchLater(videoId)
    }

    fun hideVideo(videoId: String) {
        repository.hideVideo(videoId)
    }

    fun addVideoComment(videoId: String, text: String) {
        repository.addVideoComment(videoId, text)
    }

    fun toggleShortLike(shortId: String) {
        repository.toggleShortLike(shortId)
    }

    fun addShortComment(shortId: String, text: String) {
        repository.addShortComment(shortId, text)
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        if (query.isBlank()) {
            _hasSubmittedSearch.value = false
        }
    }

    fun submitSearch(query: String = _searchQuery.value) {
        val clean = query.trim()
        if (clean.isNotEmpty()) {
            _searchQuery.value = clean
            _hasSubmittedSearch.value = true
            repository.addRecentSearch(clean)
        }
    }

    fun selectSearchFilter(filter: SearchFilter) {
        _searchFilter.value = filter
    }

    fun removeRecentSearch(query: String) {
        repository.removeRecentSearch(query)
    }

    fun clearRecentSearches() {
        repository.clearRecentSearches()
    }

    fun selectSubscriptionChannel(channelId: String?) {
        _selectedSubscriptionChannelId.value = channelId
    }

    fun markNotificationRead(notificationId: String) {
        repository.markNotificationRead(notificationId)
    }

    fun markAllNotificationsRead() {
        repository.markAllNotificationsRead()
    }

    fun signIn(email: String, displayName: String? = null) {
        repository.signIn(email, displayName)
    }

    fun signOut() {
        repository.signOut()
    }

    fun clearWatchHistory() {
        repository.clearWatchHistory()
    }

    fun updateThemeMode(mode: ThemeMode) {
        repository.updateThemeMode(mode)
    }

    fun toggleAutoplayNext() {
        repository.toggleAutoplayNext()
    }

    fun toggleNotificationsEnabled() {
        repository.toggleNotificationsEnabled()
    }

    fun toggleStreamOnMobileData() {
        repository.toggleStreamOnMobileData()
    }

    fun updateDefaultQuality(quality: PlaybackQuality) {
        repository.updateDefaultQuality(quality)
    }

    fun publishCreatorContent(
        title: String,
        description: String,
        category: VideoCategory,
        sceneStyle: StudioSceneStyle,
        isShort: Boolean,
        videoUri: String? = null
    ): String {
        return repository.publishVideo(
            title = title,
            description = description,
            category = category,
            sceneStyle = sceneStyle,
            isShort = isShort,
            videoUri = videoUri
        )
    }

    fun importPhoneVideos(
        context: Context,
        uris: List<Uri>,
        onComplete: (Int) -> Unit
    ) {
        if (uris.isEmpty()) return
        viewModelScope.launch {
            _isImportingVideos.value = true
            val extracted = LocalVideoScanner.extractMetadataFromUris(context, uris)
            val addedCount = repository.importDeviceVideos(extracted)
            _isImportingVideos.value = false
            onComplete(addedCount)
        }
    }

    fun importPhoneFolder(
        context: Context,
        treeUri: Uri,
        onComplete: (Int) -> Unit
    ) {
        viewModelScope.launch {
            _isImportingVideos.value = true
            val extracted = LocalVideoScanner.scanFolderTree(context, treeUri)
            val addedCount = repository.importDeviceVideos(extracted)
            _isImportingVideos.value = false
            onComplete(addedCount)
        }
    }

    fun scanAllMediaStoreVideos(
        context: Context,
        onComplete: (Int) -> Unit
    ) {
        viewModelScope.launch {
            _isImportingVideos.value = true
            val discovered = LocalVideoScanner.scanAllMediaStoreVideos(context)
            val addedCount = repository.importDeviceVideos(discovered)
            _isImportingVideos.value = false
            onComplete(addedCount)
        }
    }

    fun getChannelSync(channelId: String): Channel = repository.getChannel(channelId)
}
