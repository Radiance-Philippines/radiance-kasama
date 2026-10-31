package com.radianceph.pos.ui

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.radianceph.pos.Words
import com.radianceph.pos.update.Updates
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/** The update pop-up at each step, drawn on a phone-sized screen (SCREENS_DIR, else the app's screens folder) */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w393dp-h852dp-xxhdpi")
class UpdateScreensTest {
    @get:Rule val rule = createComposeRule()

    private val release = Updates.Release("0.2.0", "- Updates pop up: Update now, Remind me later, Skip this version\n- Push alerts", "", 31_000_000, "")

    private fun card(name: String, tl: Boolean, state: Updates.State, metered: Boolean = false) {
        Words.use(if (tl) "tl-PH" else "en-US")
        rule.setContent {
            PosTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f)), contentAlignment = Alignment.Center) {
                        UpdateCard(state, metered, {}, {}, {}, {}, {}, {}, {}, {})
                    }
                }
            }
        }
        rule.waitForIdle()
        val dir = File(System.getenv("SCREENS_DIR") ?: "/srv/build/android-pos/screens")
        if (!dir.isDirectory && !dir.mkdirs()) return
        val bmp: Bitmap = rule.onRoot().captureToImage().asAndroidBitmap()
        File(dir, "$name.png").outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    @Test fun outEn() = card("update-out-en", tl = false, Updates.State.Available(release), metered = true)
    @Test fun outTl() = card("update-out-tl", tl = true, Updates.State.Available(release))
    @Test fun downloadingTl() = card("update-downloading-tl", tl = true, Updates.State.Downloading(release, 0.4f))
    @Test fun readyTl() = card("update-ready-tl", tl = true, Updates.State.Ready(release))
    @Test fun allowEn() = card("update-allow-en", tl = false, Updates.State.Allow(release))
}
