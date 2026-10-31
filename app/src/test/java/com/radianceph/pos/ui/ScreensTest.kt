package com.radianceph.pos.ui

import android.graphics.Bitmap
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.radianceph.pos.Link
import com.radianceph.pos.Words
import org.json.JSONObject
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/** The screens drawn on a phone-sized screen, to look at on the build server (no phone needed) */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w393dp-h852dp-xxhdpi")
class ScreensTest {
    @get:Rule val rule = createComposeRule()

    private val link = Link("https://posv3.radianceph.com/demo/", "demo", 7, "Ana Santos", "Botika ng Bayan", "tee", "tl-PH")

    private fun me(scan: Boolean, receiving: Boolean, price: Boolean, count: Boolean) = JSONObject(
        """{"ok":true,"person":{"id":7,"name":"Ana Santos","first_name":"Ana"},"shop":{"name":"Botika ng Bayan","slug":"demo","language":"tl-PH"},
        "branches":[{"location_id":1,"name":"Lipa"}],"count_branches":[{"location_id":1,"name":"Lipa"}],
        "can":{"scan_register":$scan,"scan_receiving":$receiving,"price_check":$price,"count":$count},"phone":{"phone_id":7,"attested":"tee"}}""",
    )

    private fun shot(name: String, tl: Boolean, content: @Composable () -> Unit) {
        Words.use(if (tl) "tl-PH" else "en-US")
        rule.setContent { PosTheme { Surface(color = MaterialTheme.colorScheme.background) { content() } } }
        rule.waitForIdle()
        val dir = File("/srv/build/android-pos/screens")
        if (!dir.isDirectory && !dir.mkdirs()) return
        val bmp: Bitmap = rule.onRoot().captureToImage().asAndroidBitmap()
        File(dir, "$name.png").outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    @Test fun link() = shot("link-en", tl = false) { LinkScreen(onLinked = {}) }
    @Test fun homeCashier() = shot("home-cashier-tl", tl = true) { Home(link, me(scan = true, receiving = false, price = true, count = false), null, onRetry = {}, onOpen = {}) }
    @Test fun homeManager() = shot("home-manager-en", tl = false) { Home(link.copy(language = "en-US"), me(scan = true, receiving = true, price = true, count = true), null, onRetry = {}, onOpen = {}) }
    @Test fun homeOffline() = shot("home-offline-en", tl = false) { Home(link.copy(language = "en-US"), null, "No connection to the shop. Check the internet and try again.", onRetry = {}, onOpen = {}) }
    @Test fun price() = shot("price-tl", tl = true) {
        PriceResult(JSONObject("""{"item_id":1,"name":"Paracetamol 500mg (Biogesic)","barcode":"4800016644115","price":2.5,"price_text":"₱2.50","stock":[{"location_id":1,"name":"Lipa","quantity":40,"text":"40"},{"location_id":2,"name":"Tanauan","quantity":12,"text":"12"}],"counted":true}"""))
    }
    @Test fun phone() = shot("phone-tl", tl = true) { ThisPhone(link, onBack = {}, onForget = {}) }
}
