package com.radianceph.pos.ui

import android.graphics.Bitmap
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import com.radianceph.pos.ApiError
import com.radianceph.pos.Link
import com.radianceph.pos.Words
import com.radianceph.pos.Words.t
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.io.IOException

/** The screens drawn on a phone-sized screen, to look at on the build server (no phone needed) */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w393dp-h852dp-xxhdpi")
class ScreensTest {
    @get:Rule val rule = createComposeRule()

    private val link = Link("https://posv3.radianceph.com/demo/", "demo", 7, "Ana Santos", "Botika ng Bayan", "tee", "tl-PH")

    /** clock: the shop's can.time_clock (radiance-pos#210); null = not sent (a shop on an older release) */
    private fun me(scan: Boolean, receiving: Boolean, price: Boolean, count: Boolean, clock: Boolean? = true) = JSONObject(
        """{"ok":true,"person":{"id":7,"name":"Ana Santos","first_name":"Ana"},"shop":{"name":"Botika ng Bayan","slug":"demo","language":"tl-PH"},
        "branches":[{"location_id":1,"name":"Lipa"}],"count_branches":[{"location_id":1,"name":"Lipa"}],
        "can":{"scan_register":$scan,"scan_receiving":$receiving,"price_check":$price,"count":$count${if (clock == null) "" else ",\"time_clock\":$clock"}},"phone":{"phone_id":7,"attested":"tee"}}""",
    )

    /** what the shop says with its 409 clock_off (radiance-pos#210, rpos_api_error_clock_off) */
    private val off = "The time clock is off in this shop. The owner or an admin turns it on in Settings > Time clock."

    /** Scanning, price check and counting stay as they are, whatever the time clock */
    private fun theRestStays() {
        rule.onNodeWithText(t("scan_to_pc")).assertExists()
        rule.onNodeWithText(t("price_check")).assertExists()
        rule.onNodeWithText(t("count")).assertExists()
    }

    private fun shot(name: String, tl: Boolean, content: @Composable () -> Unit) {
        Words.use(if (tl) "tl-PH" else "en-US")
        rule.setContent { PosTheme { Surface(color = MaterialTheme.colorScheme.background) { content() } } }
        rule.waitForIdle()
        // SCREENS_DIR: another build user's own folder (the default one is root's on ribs02)
        val dir = File(System.getenv("SCREENS_DIR") ?: "/srv/build/android-pos/screens")
        if (!dir.isDirectory && !dir.mkdirs()) return
        val bmp: Bitmap = rule.onRoot().captureToImage().asAndroidBitmap()
        File(dir, "$name.png").outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    @Test fun link() = shot("link-en", tl = false) { LinkScreen(onLinked = {}) }
    @Test fun homeCashier() = shot("home-cashier-tl", tl = true) { Home(link, me(scan = true, receiving = false, price = true, count = false), null, onRetry = {}, onOpen = {}) }
    @Test fun homeManager() = shot("home-manager-en", tl = false) { Home(link.copy(language = "en-US"), me(scan = true, receiving = true, price = true, count = true), null, onRetry = {}, onOpen = {}) }
    @Test fun homeWaiting() = shot("home-waiting-tl", tl = true) { Home(link, null, null, onRetry = {}, onOpen = {}, waiting = "Hinihintay pa ng phone na ito ang approval ng may-ari o admin. Kapag na-approve na, i-tap ang Tingnan ulit.") }
    @Test fun homeWaitingEn() = shot("home-waiting-en", tl = false) { Home(link.copy(language = "en-US"), null, null, onRetry = {}, onOpen = {}, waiting = "") }
    @Test fun homeOffline() = shot("home-offline-en", tl = false) { Home(link.copy(language = "en-US"), null, "No connection to the shop. Check the internet and try again.", onRetry = {}, onOpen = {}) }
    @Test fun price() = shot("price-tl", tl = true) {
        PriceResult(JSONObject("""{"item_id":1,"name":"Paracetamol 500mg (Biogesic)","barcode":"4800016644115","price":2.5,"price_text":"₱2.50","stock":[{"location_id":1,"name":"Lipa","quantity":40,"text":"40"},{"location_id":2,"name":"Tanauan","quantity":12,"text":"12"}],"counted":true}"""))
    }
    @Test fun phone() = shot("phone-tl", tl = true) { ThisPhone(link, onBack = {}, onForget = {}) }

    // ---- the shop's time clock on or off (radiance-pos#210, radiance-pos-android#4) ----

    @Test fun clockOffHidesTheTimeClock() {
        shot("home-clock-off-en", tl = false) { Home(link.copy(language = "en-US"), me(scan = true, receiving = true, price = true, count = true, clock = false), null, onRetry = {}, onOpen = {}) }
        rule.onNodeWithText(t("clock")).assertDoesNotExist()
        theRestStays()
    }

    @Test fun clockOnShowsTheTimeClock() {
        shot("home-clock-on-tl", tl = true) { Home(link, me(scan = true, receiving = true, price = true, count = true, clock = true), null, onRetry = {}, onOpen = {}) }
        rule.onNodeWithText(t("clock")).assertExists()
        theRestStays()
    }

    @Test fun clockNotSaidShowsTheTimeClock() {
        // a shop on an older release doesn't send can.time_clock: on, as before
        shot("home-clock-unsaid-en", tl = false) { Home(link.copy(language = "en-US"), me(scan = true, receiving = true, price = true, count = true, clock = null), null, onRetry = {}, onOpen = {}) }
        rule.onNodeWithText(t("clock")).assertExists()
        theRestStays()
    }

    @Test fun clockOnReadsMe() {
        assertFalse(clockOn(me(scan = true, receiving = false, price = true, count = false, clock = false)))
        assertTrue(clockOn(me(scan = true, receiving = false, price = true, count = false, clock = true)))
        assertTrue(clockOn(me(scan = true, receiving = false, price = true, count = false, clock = null)))
        assertTrue(clockOn(JSONObject("""{"ok":true}""")))
    }

    @Test fun clockOffRefusalIsKnownAndSaidPlainly() {
        Words.use("en-US")
        assertEquals(off, clockOffSaid(ApiError(409, "clock_off", off)))
        // no words from the shop: the app's own, in the shop's language
        assertEquals(t("clock_off"), clockOffSaid(ApiError(409, "clock_off", "")))
        Words.use("tl-PH")
        assertTrue(clockOffSaid(ApiError(409, "clock_off", ""))!!.startsWith("Naka-off ang time clock"))
        // anything else stays on the page as before
        assertNull(clockOffSaid(ApiError(403, "geo", "Wala ka sa shop.")))
        assertNull(clockOffSaid(IOException("no route")))
        assertNull(clockOffSaid(null))
    }

    @Test fun clockOffMakesMeSayOffUntilTheShopAnswers() {
        val before = me(scan = true, receiving = false, price = true, count = false, clock = true)
        val after = withClockOff(before)!!
        assertFalse(clockOn(after))
        assertTrue(after.getJSONObject("can").getBoolean("scan_register"))
        assertTrue(clockOn(before)) // the shop's own me untouched
        assertFalse(clockOn(withClockOff(JSONObject("""{"ok":true}"""))))
        assertNull(withClockOff(null))
    }

    @Test fun clockOff409TakesThePersonHomeWithTheShopsWords() {
        // the time clock page got 409 clock_off: Home, the shop's words plainly, no Time clock, the rest as it was
        var closed = false
        val said = clockOffSaid(ApiError(409, "clock_off", off))!!
        shot("home-clock-off-said-en", tl = false) {
            Home(link.copy(language = "en-US"), withClockOff(me(scan = true, receiving = true, price = true, count = true, clock = true)), null, onRetry = {}, onOpen = {}, notice = said, onNotice = { closed = true })
        }
        rule.onNodeWithText(off).assertExists()
        rule.onNodeWithText(t("clock")).assertDoesNotExist()
        theRestStays()
        rule.onNodeWithText("OK").performClick()
        assertTrue(closed)
    }
}
