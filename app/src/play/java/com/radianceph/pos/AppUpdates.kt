package com.radianceph.pos

import android.content.Context
import androidx.compose.runtime.Composable

/**
 * How this build updates (the play flavor): Google Play does it, so nothing here (Play forbids
 * apps updating themselves). The github flavor's AppUpdates updates by itself.
 */
object AppUpdates {
    fun atStart(context: Context) = Unit

    fun resumed(context: Context) = Unit

    @Composable
    fun Prompt() = Unit
}
