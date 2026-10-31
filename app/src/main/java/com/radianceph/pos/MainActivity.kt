package com.radianceph.pos

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.radianceph.pos.ui.App
import com.radianceph.pos.ui.PosTheme

/** Radiance POS V3 for Android (radiance-pos#172): one screen at a time (ui/Screens.kt) */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        Links.get(this)?.let { Words.use(it.language) }
        Alerts.channel(this)
        AppUpdates.atStart(this) // a newer version out? (the github flavor updates itself; Play updates the play one)
        if (savedInstanceState == null) openAlert(intent)
        setContent {
            PosTheme {
                Surface(Modifier.fillMaxSize().safeDrawingPadding(), color = MaterialTheme.colorScheme.background) {
                    App()
                    AppUpdates.Prompt() // the update pop-up, when there's one
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        AppUpdates.resumed(this) // back from allowing installs, or a look when it's due
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        openAlert(intent)
    }

    /** An alert tapped (radiance-pos#175): the shop's page it is about, in the browser; the app stays as it was */
    private fun openAlert(intent: Intent?) {
        val page = Alerts.page(this, intent) ?: return
        intent?.removeExtra(Alerts.URL)
        runCatching { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(page))) }
    }
}
