package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.example.ui.ScannerScreen
import com.example.ui.theme.GpsDetectorTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `read app name string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("GPS Scanner", appName)
    }

    @Test
    fun `scanner screen switches tabs and opens pro subscription sheet`() {
        composeTestRule.setContent {
            GpsDetectorTheme {
                ScannerScreen()
            }
        }

        composeTestRule.onNodeWithTag("scan_toggle_button").assertIsDisplayed()
        composeTestRule.onNodeWithTag("verdict_title_text").assertIsDisplayed()

        composeTestRule.onNodeWithTag("tab_device_gps").performClick()
        composeTestRule.onNodeWithTag("scan_toggle_button").assertIsDisplayed()

        composeTestRule.onNodeWithTag("pro_upgrade_button").performClick()
        composeTestRule.onNodeWithTag("plan_monthly_card").assertIsDisplayed()
        composeTestRule.onNodeWithTag("plan_annual_card").assertIsDisplayed()
    }
}
