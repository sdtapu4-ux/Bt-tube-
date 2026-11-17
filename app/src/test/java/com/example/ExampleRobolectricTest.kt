package com.example

import android.Manifest
import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
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
import com.example.data.MediaPermissionHelper
import com.example.model.AppSettings
import com.example.ui.BtTubeApp
import com.example.ui.theme.BtTubeTheme
import com.example.ui.viewmodel.BtTubeViewModel
import com.example.util.NetworkConnectivityHelper
import com.example.util.NetworkTransport
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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

    @Test
    fun `app name is BT Tube`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("BT Tube", appName)
    }

    @Test
    fun `media permission helper checks READ_MEDIA_VIDEO on API 33 plus`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        assertEquals(
            Manifest.permission.READ_MEDIA_VIDEO,
            MediaPermissionHelper.requiredVideoPermission()
        )
        assertFalse(MediaPermissionHelper.hasVideoStoragePermission(app))

        Shadows.shadowOf(app).grantPermissions(Manifest.permission.READ_MEDIA_VIDEO)
        assertTrue(MediaPermissionHelper.hasVideoStoragePermission(app))
    }

    @Test
    @Config(sdk = [32], qualifiers = "w411dp-h915dp-mdpi")
    fun `media permission helper checks READ_EXTERNAL_STORAGE on API 32 and below`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        assertEquals(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            MediaPermissionHelper.requiredVideoPermission()
        )
        assertFalse(MediaPermissionHelper.hasVideoStoragePermission(app))

        Shadows.shadowOf(app).grantPermissions(Manifest.permission.READ_EXTERNAL_STORAGE)
        assertTrue(MediaPermissionHelper.hasVideoStoragePermission(app))
    }

    @Test
    fun `mobile data cellular network is recognized and enabled by default`() {
        assertTrue(AppSettings().streamOnMobileData)

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
    fun `verify all five bottom navigation tabs and search and player flows`() {
        val viewModel = BtTubeViewModel()
        composeTestRule.setContent {
            BtTubeTheme {
                BtTubeApp(viewModel = viewModel)
            }
        }
        composeTestRule.waitForIdle()

        // 1. Home Screen checks
        composeTestRule.onNodeWithTag("home_bt_logo").assertIsDisplayed()
        composeTestRule.onNodeWithTag("home_search_button").assertIsDisplayed()
        composeTestRule.onNodeWithTag("home_notifications_button").assertIsDisplayed()
        composeTestRule.onNodeWithTag("home_import_phone_videos_card").assertIsDisplayed()
        composeTestRule.onNodeWithTag("scan_all_videos_button").assertIsDisplayed()
        composeTestRule.onNodeWithTag("import_select_videos_button").assertIsDisplayed()
        composeTestRule.onNodeWithTag("import_folder_button").assertIsDisplayed()
        composeTestRule.onNodeWithTag("trending_carousel").assertIsDisplayed()
        composeTestRule.onNodeWithTag("category_chip_tech").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("category_chip_all").performClick()
        composeTestRule.waitForIdle()

        // 2. Navigate to Shorts & verify mute control
        composeTestRule.onNodeWithTag("nav_shorts").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("shorts_vertical_pager").assertIsDisplayed()
        composeTestRule.onNodeWithTag("shorts_mute_button").assertIsDisplayed()

        // 3. Navigate to Create & verify studio form
        composeTestRule.onNodeWithTag("nav_create").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("create_title_input")
            .performScrollTo()
            .assertIsDisplayed()
        composeTestRule.onNodeWithTag("publish_content_button")
            .performScrollTo()
            .assertIsDisplayed()

        // 4. Navigate to Subscriptions
        composeTestRule.onNodeWithTag("nav_subscriptions").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("manage_subscriptions_button").assertIsDisplayed()

        // 5. Navigate to Profile, test Auth form & Mobile Data setting
        composeTestRule.onNodeWithTag("nav_profile").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("profile_sign_in_button").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("auth_email_input").performTextInput("alex@bttube.app")
        composeTestRule.onNodeWithTag("auth_password_input").performTextInput("secret123")
        composeTestRule.onNodeWithTag("auth_confirm_button").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Alex").assertIsDisplayed()
        composeTestRule.onNodeWithTag("profile_list")
            .performScrollToKey("settings_section")
        composeTestRule.onNodeWithTag("settings_mobile_data_switch")
            .performScrollTo()
            .assertIsDisplayed()

        // 6. Navigate Home -> Search
        composeTestRule.onNodeWithTag("nav_home").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("home_search_button").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("search_input").performTextInput("Iceland")
        composeTestRule.onNodeWithTag("search_submit_button").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("compact_video_vid_2").assertIsDisplayed()

        // 7. Open Video Player from Search Result & verify sound mute button
        composeTestRule.mainClock.autoAdvance = false
        composeTestRule.onNodeWithTag("compact_video_vid_2").performClick()
        composeTestRule.mainClock.advanceTimeBy(400)
        composeTestRule.onNodeWithTag("player_play_pause_button").assertIsDisplayed()
        composeTestRule.onNodeWithTag("player_mute_button").assertIsDisplayed()
        composeTestRule.onNodeWithTag("player_mute_button").performClick()
        composeTestRule.mainClock.advanceTimeBy(100)
        composeTestRule.onNodeWithTag("player_seek_bar").assertIsDisplayed()
        composeTestRule.onNodeWithTag("playback_speed_button").assertIsDisplayed()
        composeTestRule.onNodeWithTag("player_fullscreen_button").assertIsDisplayed()
        composeTestRule.onNodeWithTag("player_subscribe_button").assertIsDisplayed()
        composeTestRule.onNodeWithTag("player_like_button").performClick()
        composeTestRule.mainClock.advanceTimeBy(100)
        composeTestRule.onNodeWithTag("player_play_pause_button").performClick()
        composeTestRule.mainClock.autoAdvance = true
        composeTestRule.waitForIdle()
    }
}
