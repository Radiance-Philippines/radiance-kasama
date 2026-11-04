package com.radianceph.pos.update

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Under Robolectric for Android's org.json (a release's answer reads with it) */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class UpdatesTest {
    @Test fun versionsCompareAsNumbers() {
        assertTrue(Updates.newer("0.1.10", "0.1.9"))
        assertTrue(Updates.newer("0.2.0", "0.1.9"))
        assertFalse(Updates.newer("0.1.0", "0.1.0"))
        assertFalse(Updates.newer("0.1", "0.1.0"))
    }

    @Test fun thePopUpWaitsADayAfterRemindMeLaterAndSkipsTheSkippedVersion() {
        val now = 1_000_000_000L
        assertTrue(Updates.asks("0.2.0", null, 0, now))
        assertFalse(Updates.asks("0.2.0", null, now + Updates.LATER_MS, now + Updates.LATER_MS - 1))
        assertTrue(Updates.asks("0.2.0", null, now + Updates.LATER_MS, now + Updates.LATER_MS))
        assertFalse(Updates.asks("0.2.0", "0.2.0", 0, now))
        assertTrue(Updates.asks("0.2.1", "0.2.0", 0, now))
    }

    @Test fun aReleaseReadsWithItsAppFileAndSha256() {
        val r = Updates.release(JSONObject("""{"tag_name":"v0.2.0","body":"- Updates","html_url":"https://github.com/x/y/releases/tag/v0.2.0",
            "assets":[{"name":"notes.txt","size":1,"browser_download_url":"https://e/n"},{"name":"RadianceKasama.apk","size":9,"browser_download_url":"https://e/a.apk","digest":"sha256:ab"}]}"""))
        assertEquals(listOf("0.2.0", "https://e/a.apk", "ab"), listOf(r.version, r.apk, r.sha256))
        assertEquals(9L, r.size)
    }
}
