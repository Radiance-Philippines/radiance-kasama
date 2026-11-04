package com.radianceph.pos

import android.content.Context
import androidx.compose.runtime.Composable
import com.radianceph.pos.ui.UpdatePrompt
import com.radianceph.pos.update.Updates

/**
 * How this build updates (the github flavor, from the download page): by itself, from the
 * newest GitHub release, with the update pop-up (radiance-kasama#1). The play flavor's
 * AppUpdates does nothing: Google Play updates it.
 */
object AppUpdates {
    /** At start: a newer version out? (a little later, at most every 6 hours) */
    fun atStart(context: Context) = Updates.atStart(context)

    /** Back in the app: go on after allowing installs, or look again when due. */
    fun resumed(context: Context) = Updates.resumed(context)

    /** The update pop-up, when there's something to say. */
    @Composable
    fun Prompt() = UpdatePrompt()
}
