package com.radianceph.pos.ui

import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Android's Back on a page goes to Home and the app stays open; on Home it leaves (owner, 2026-11-01) */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class BackTest {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()

    @Test fun backOnAPageGoesHomeAndTheAppStays() {
        var page by mutableStateOf(Page.PRICE)
        rule.setContent { BackToHome(page) { page = Page.HOME } }
        rule.runOnUiThread { rule.activity.onBackPressedDispatcher.onBackPressed() }
        rule.waitForIdle()
        assertEquals(Page.HOME, page)
        assertFalse(rule.activity.isFinishing)
    }

    @Test fun backOnHomeLeavesAsAndroidDoes() {
        var page by mutableStateOf(Page.HOME)
        rule.setContent { BackToHome(page) { page = Page.HOME } }
        rule.runOnUiThread { rule.activity.onBackPressedDispatcher.onBackPressed() }
        rule.waitForIdle()
        assertTrue(rule.activity.isFinishing)
    }
}
