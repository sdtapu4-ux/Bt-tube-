package com.example

import android.Manifest
import android.app.Application
import android.content.Context
import android.net.NetworkCapabilities
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToKey
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import com.example.audio.BackgroundAudioController
import com.example.audio.BackgroundAudioTrack
import com.example.data.BtTubeRepository
import com.example.data.DeviceMediaType
import com.example.data.ImportedDeviceVideo
import com.example.data.LocalVideoScanner
import com.example.data.MediaPermissionHelper
import com.example.model.AppSettings
import com.example.model.PlaybackQuality
import com.example.player.MediaTrackOption
import com.example.player.MediaTrackSelectionHelper
import com.example.ui.BtTubeApp
import com.example.ui.screens.GmailAuthScreen
import com.example.ui.theme.BtTubeTheme
import com.example.ui.viewmodel.BtTubeViewModel
import com.example.util.NetworkConnectivityHelper
import com.example.util.NetworkTransport
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowNetworkCapabilities

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w411dp-h915dp-mdpi")
class ExampleRobolectricTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @After
    fun tearDown() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        BackgroundAudioController.stop(app)
    }

    @Test
    fun `app name is BT Tube`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("BT Tube", appName)
    }

    @Test
    fun `media permission helper checks READ_MEDIA_VIDEO and READ_MEDIA_AUDIO on API 33 plus`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        assertEquals(
            Manifest.permission.READ_MEDIA_VIDEO,
            MediaPermissionHelper.requiredVideoPermission()
        )
        assertEquals(
            Manifest.permission.READ_MEDIA_AUDIO,
            MediaPermissionHelper.requiredAudioPermission()
        )
        val required = MediaPermissionHelper.requiredMediaPermissions().toSet()
        assertTrue(Manifest.permission.READ_MEDIA_VIDEO in required)
        assertTrue(Manifest.permission.READ_MEDIA_AUDIO in required)

        assertFalse(MediaPermissionHelper.hasVideoStoragePermission(app))
        assertFalse(MediaPermissionHelper.hasAudioStoragePermission(app))
        assertFalse(MediaPermissionHelper.hasAnyMediaPermission(app))
        assertFalse(MediaPermissionHelper.hasAllMediaPermissions(app))

        Shadows.shadowOf(app).grantPermissions(Manifest.permission.READ_MEDIA_VIDEO)
        assertTrue(MediaPermissionHelper.hasVideoStoragePermission(app))
        assertTrue(MediaPermissionHelper.hasAnyMediaPermission(app))
        assertFalse(MediaPermissionHelper.hasAllMediaPermissions(app))

        Shadows.shadowOf(app).grantPermissions(Manifest.permission.READ_MEDIA_AUDIO)
        assertTrue(MediaPermissionHelper.hasAudioStoragePermission(app))
        assertTrue(MediaPermissionHelper.hasAllMediaPermissions(app))
    }

    @Test
    @Config(sdk = [32], qualifiers = "w411dp-h915dp-mdpi")
    fun `media permission helper checks READ_EXTERNAL_STORAGE on API 32 and below`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        assertEquals(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            MediaPermissionHelper.requiredVideoPermission()
        )
        assertEquals(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            MediaPermissionHelper.requiredAudioPermission()
        )
        assertEquals(
            listOf(Manifest.permission.READ_EXTERNAL_STORAGE),
            MediaPermissionHelper.requiredMediaPermissions().toList()
        )
        assertFalse(MediaPermissionHelper.hasAnyMediaPermission(app))

        Shadows.shadowOf(app).grantPermissions(Manifest.permission.READ_EXTERNAL_STORAGE)
        assertTrue(MediaPermissionHelper.hasVideoStoragePermission(app))
        assertTrue(MediaPermissionHelper.hasAudioStoragePermission(app))
        assertTrue(MediaPermissionHelper.hasAllMediaPermissions(app))
    }

    @Test
    fun `all SD video qualities 144p 240p 360p 480p and mobile data are supported`() {
        assertTrue(AppSettings().streamOnMobileData)
        assertEquals("144p SD", LocalVideoScanner.classifyQualityLabel(256, 144))
        assertEquals("240p SD", LocalVideoScanner.classifyQualityLabel(426, 240))
        assertEquals("360p SD", LocalVideoScanner.classifyQualityLabel(640, 360))
        assertEquals("480p SD", LocalVideoScanner.classifyQualityLabel(854, 480))
        assertEquals("720p HD", LocalVideoScanner.classifyQualityLabel(1280, 720))
        assertEquals("1080p HD", LocalVideoScanner.classifyQualityLabel(1920, 1080))

        val sdEntries = PlaybackQuality.entries.filter { it.isSd }
        assertEquals(
            listOf(
                PlaybackQuality.SD_480,
                PlaybackQuality.SD_360,
                PlaybackQuality.SD_240,
                PlaybackQuality.SD_144
            ),
            sdEntries
        )

        val caps = ShadowNetworkCapabilities.newInstance()
        val shadowCaps = Shadows.shadowOf(caps)
        shadowCaps.addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
        shadowCaps.addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)

        val state = NetworkConnectivityHelper.mapCapabilitiesToState(caps, isMetered = true)
        assertTrue(state.isConnected)
        assertEquals(NetworkTransport.MOBILE_DATA, state.transport)
        assertTrue(state.canStreamOnMobileData)
    }

    @Test
    fun `repository starts with only device library empty until device media is imported`() {
        val repository = BtTubeRepository()
        assertTrue(repository.videos.value.isEmpty())
        assertTrue(repository.shorts.value.isEmpty())
        assertTrue(repository.channels.value.isEmpty())

        val addedCount = repository.importDeviceVideos(
            listOf(
                ImportedDeviceVideo(
                    uriString = "content://media/external/video/media/101",
                    title = "Concert SD Video Song",
                    durationSeconds = 45,
                    mediaType = DeviceMediaType.VIDEO,
                    artistOrFolder = "Camera",
                    widthPx = 640,
                    heightPx = 360,
                    qualityLabel = "360p SD"
                ),
                ImportedDeviceVideo(
                    uriString = "content://media/external/audio/media/202",
                    title = "Acoustic Track",
                    durationSeconds = 210,
                    mediaType = DeviceMediaType.AUDIO,
                    artistOrFolder = "Phone Music"
                )
            )
        )

        assertEquals(2, addedCount)
        assertEquals(2, repository.videos.value.size)
        assertEquals(1, repository.shorts.value.size)
        val videoEntry = repository.videos.value.first { it.title == "Concert SD Video Song" }
        val audioEntry = repository.videos.value.first { it.title == "Acoustic Track" }
        assertFalse(videoEntry.isAudioOnly)
        assertEquals("360p SD", videoEntry.qualityLabel)
        assertTrue(audioEntry.isAudioOnly)
    }

    @Test
    fun `background audio controller tracks active song queue and stops cleanly`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val track1 = BackgroundAudioTrack(
            id = "v1",
            title = "Video Song 1",
            subtitle = "My Phone Videos",
            uriString = "content://media/external/video/media/1",
            isVideoSource = true,
            durationSeconds = 180
        )
        val track2 = BackgroundAudioTrack(
            id = "a2",
            title = "Audio Song 2",
            subtitle = "My Phone Audio",
            uriString = "content://media/external/audio/media/2",
            isVideoSource = false,
            durationSeconds = 200
        )

        BackgroundAudioController.playTrackAsAudio(
            context = app,
            track = track1,
            queue = listOf(track1, track2),
            startPositionSeconds = 12f,
            playbackSpeed = 1.25f
        )

        val activeState = BackgroundAudioController.state.value
        assertNotNull(activeState.currentTrack)
        assertEquals("v1", activeState.currentTrack?.id)
        assertEquals(2, activeState.queue.size)
        assertTrue(activeState.isPlaying)
        assertEquals(12f, activeState.positionSeconds)
        assertEquals(1.25f, activeState.playbackSpeed)

        BackgroundAudioController.stop(app)
        assertNull(BackgroundAudioController.state.value.currentTrack)
        assertFalse(BackgroundAudioController.state.value.isPlaying)
    }

    @Test
    fun `gmail auth screen renders Sign in with Gmail button`() {
        composeTestRule.setContent {
            BtTubeTheme {
                GmailAuthScreen(
                    isSigningIn = false,
                    errorMessage = null,
                    onGoogleSignInClick = {}
                )
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("gmail_sign_in_button").assertIsDisplayed()
        composeTestRule.onNodeWithText("Sign in with Gmail").assertIsDisplayed()
    }

    @Test
    fun `multi-track video selector prefers real HEVC or higher-quality stream over dummy stub track`() {
        val tracks = listOf(
            MediaTrackOption(
                id = 0,
                label = "Video 1 • 360p • H.264",
                width = 640,
                height = 360,
                bitrate = 120_000,
                codec = "H.264"
            ),
            MediaTrackOption(
                id = 1,
                label = "Video 2 • 480p • HEVC",
                width = 854,
                height = 480,
                bitrate = 950_000,
                codec = "HEVC"
            ),
            MediaTrackOption(
                id = 2,
                label = "Video 3 • 240p • VP9",
                width = 426,
                height = 240,
                bitrate = 350_000,
                codec = "VP9"
            )
        )

        val chosenAuto = MediaTrackSelectionHelper.choosePreferredVideoTrackId(tracks, PlaybackQuality.AUTO)
        assertEquals(1, chosenAuto)

        val chosen240 = MediaTrackSelectionHelper.choosePreferredVideoTrackId(tracks, PlaybackQuality.SD_240)
        assertEquals(2, chosen240)

        val sameResStubVsReal = listOf(
            MediaTrackOption(id = 0, label = "Video 1 • 360p • H.264", width = 640, height = 360, bitrate = 150_000, codec = "H.264"),
            MediaTrackOption(id = 1, label = "Video 2 • 360p • HEVC", width = 640, height = 360, bitrate = 800_000, codec = "HEVC")
        )
        assertEquals(1, MediaTrackSelectionHelper.choosePreferredVideoTrackId(sameResStubVsReal, PlaybackQuality.SD_360))
    }

    @Test
    fun `verify device-only flows, SD quality selector, audio mode toggle, mini player, and navigation tabs`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val repository = BtTubeRepository().apply {
            applyGmailUser(
                uid = "gmail_uid_1",
                email = "sdtapu95@gmail.com",
                displayName = "Tapu",
                photoUrl = null
            )
        }
        val viewModel = BtTubeViewModel(repository = repository)
        composeTestRule.setContent {
            BtTubeTheme {
                BtTubeApp(viewModel = viewModel)
            }
        }
        composeTestRule.waitForIdle()

        // 1. Home Screen checks (device-only initial empty library + import card + categories)
        composeTestRule.onNodeWithTag("home_bt_logo").assertIsDisplayed()
        composeTestRule.onNodeWithTag("home_search_button").assertIsDisplayed()
        composeTestRule.onNodeWithTag("home_notifications_button").assertIsDisplayed()
        composeTestRule.onNodeWithTag("home_import_phone_videos_card").assertIsDisplayed()
        composeTestRule.onNodeWithTag("scan_all_videos_button").assertIsDisplayed()
        composeTestRule.onNodeWithTag("import_select_videos_button").assertIsDisplayed()
        composeTestRule.onNodeWithTag("import_folder_button").assertIsDisplayed()
        composeTestRule.onNodeWithTag("category_chip_videos").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("category_chip_audio").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("category_chip_all").performClick()
        composeTestRule.waitForIdle()

        // 2. Navigate to Shorts & verify device-only empty state CTA
        composeTestRule.onNodeWithTag("nav_shorts").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("shorts_empty_state").assertIsDisplayed()
        composeTestRule.onNodeWithTag("shorts_scan_button").assertIsDisplayed()

        // 3. Navigate to Subscriptions & Profile, verify Gmail user, SD quality dropdown & Mobile Data
        composeTestRule.onNodeWithTag("nav_subscriptions").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("manage_subscriptions_button").assertIsDisplayed()

        composeTestRule.onNodeWithTag("nav_profile").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Tapu").assertIsDisplayed()
        composeTestRule.onNodeWithText("sdtapu95@gmail.com").assertIsDisplayed()
        composeTestRule.onNodeWithTag("profile_list")
            .performScrollToKey("settings_section")
        composeTestRule.onNodeWithTag("settings_mobile_data_switch")
            .performScrollTo()
            .assertIsDisplayed()
        composeTestRule.onNodeWithTag("settings_quality_row")
            .performScrollTo()
            .performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("quality_option_sd_360").assertIsDisplayed()
        composeTestRule.onNodeWithTag("quality_option_sd_144").performClick()
        composeTestRule.waitForIdle()
        assertEquals(PlaybackQuality.SD_144, viewModel.settings.value.defaultQuality)

        // 4. Verify Mini Audio Player Bar appears when background audio is active on tabs
        composeTestRule.runOnUiThread {
            BackgroundAudioController.playTrackAsAudio(
                context = app,
                track = BackgroundAudioTrack(
                    id = "bg_test",
                    title = "Background Song Test",
                    subtitle = "My Phone Videos",
                    uriString = "content://media/external/video/media/77",
                    isVideoSource = true,
                    durationSeconds = 120
                )
            )
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("mini_audio_player_bar").assertIsDisplayed()
        composeTestRule.onNodeWithTag("mini_player_close").performClick()
        composeTestRule.waitForIdle()

        // 5. Navigate to Create, publish a Video Song, and test Player SD quality & Audio mode toggle
        composeTestRule.onNodeWithTag("nav_create").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("create_title_input")
            .performScrollTo()
            .performTextInput("Acoustic Video Song")
        composeTestRule.mainClock.autoAdvance = false
        composeTestRule.onNodeWithTag("publish_content_button")
            .performScrollTo()
            .performClick()
        composeTestRule.mainClock.advanceTimeBy(500)
        composeTestRule.onNodeWithTag("player_play_pause_button").assertIsDisplayed()
        composeTestRule.onNodeWithTag("player_mute_button").assertIsDisplayed()
        composeTestRule.onNodeWithTag("player_quality_button").assertIsDisplayed()
        composeTestRule.onNodeWithTag("player_audio_mode_button").assertIsDisplayed()
        composeTestRule.onNodeWithTag("player_seek_bar").assertIsDisplayed()
        composeTestRule.onNodeWithTag("playback_speed_button").assertIsDisplayed()
        composeTestRule.onNodeWithTag("player_fullscreen_button").assertIsDisplayed()

        // Switch quality in-player to 480p SD
        composeTestRule.onNodeWithTag("player_quality_button").performClick()
        composeTestRule.mainClock.advanceTimeBy(150)
        composeTestRule.onNodeWithTag("player_quality_sd_480").performClick()
        composeTestRule.mainClock.advanceTimeBy(150)
        assertEquals(PlaybackQuality.SD_480, viewModel.settings.value.defaultQuality)

        // Switch video into Audio-only stage
        composeTestRule.onNodeWithTag("player_audio_mode_button").performClick()
        composeTestRule.mainClock.advanceTimeBy(250)
        composeTestRule.onNodeWithTag("audio_mode_stage").assertIsDisplayed()
        composeTestRule.onNodeWithTag("switch_to_video_button").assertIsDisplayed()

        // Switch back from Audio stage to Video mode
        composeTestRule.onNodeWithTag("switch_to_video_button").performClick()
        composeTestRule.mainClock.advanceTimeBy(250)
        composeTestRule.onNodeWithTag("player_mute_button").assertIsDisplayed()

        composeTestRule.onNodeWithTag("player_like_button").performClick()
        composeTestRule.mainClock.advanceTimeBy(100)
        composeTestRule.onNodeWithTag("player_back_button").performClick()
        composeTestRule.mainClock.advanceTimeBy(600)

        // 6. Search for the newly added Video Song
        composeTestRule.onNodeWithTag("nav_home").performClick()
        composeTestRule.mainClock.advanceTimeBy(300)
        composeTestRule.onNodeWithTag("home_search_button").performClick()
        composeTestRule.mainClock.advanceTimeBy(350)
        composeTestRule.onNodeWithTag("search_input").performTextInput("Acoustic")
        composeTestRule.mainClock.advanceTimeBy(150)
        composeTestRule.onNodeWithTag("search_submit_button").performClick()
        composeTestRule.mainClock.advanceTimeBy(300)
        composeTestRule.onNodeWithText("Acoustic Video Song").assertIsDisplayed()
        composeTestRule.mainClock.autoAdvance = true
    }
}
