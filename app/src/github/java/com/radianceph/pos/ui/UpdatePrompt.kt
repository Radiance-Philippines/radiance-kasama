package com.radianceph.pos.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.NotificationsOff
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Snooze
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.radianceph.pos.Words.t
import com.radianceph.pos.update.Updates

/**
 * The update pop-up (radiance-pos-android#1; owner, 2026-10-31: every app opens a modal when an
 * update is out, with "Remind me later"). It comes up by itself when a newer version is out, or
 * when one the person chose to download is ready: Update now (downloads) → Restart to update;
 * Remind me later (a day); Skip this version (until the next one).
 */
@Composable
fun UpdatePrompt() {
    val context = LocalContext.current
    val state by Updates.state.collectAsState()
    var open by remember { mutableStateOf(false) }
    // "Update now" pressed here: the pop-up follows it through to "Restart to update", even if hidden meanwhile
    var following by remember { mutableStateOf(false) }
    LaunchedEffect(state) {
        when (val s = state) {
            is Updates.State.Available -> if (open || Updates.asks(context, s.release)) open = true
            is Updates.State.Ready -> if (open || following || Updates.asks(context, s.release)) open = true
            is Updates.State.Downloading, is Updates.State.Installing, is Updates.State.Allow, is Updates.State.Failed -> Unit
            else -> open = false
        }
    }
    if (!open) return
    val release = when (val s = state) {
        is Updates.State.Available -> s.release
        is Updates.State.Ready -> s.release
        is Updates.State.Downloading -> s.release
        is Updates.State.Installing -> s.release
        is Updates.State.Allow -> s.release
        is Updates.State.Failed -> s.release
        else -> null
    }
    val later = { if (release != null) Updates.remindLater(context); open = false; following = false }
    Dialog(onDismissRequest = later, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        UpdateCard(
            state = state,
            metered = remember { Updates.metered(context) },
            onUpdate = { following = true; Updates.update(context, it) },
            onRestart = { Updates.restart(context, it) },
            onLater = later,
            onSkip = { Updates.skip(context, it); open = false; following = false },
            onHide = { open = false },
            onCancel = { Updates.cancel(); following = false },
            onAllow = { runCatching { context.startActivity(Updates.allowIntent(context)) } },
            onRetry = { r -> r?.let { Updates.update(context, it) } ?: Updates.check(context) },
        )
    }
}

/** The pop-up's card for each step (drawn alone in the screenshots). */
@Composable
fun UpdateCard(
    state: Updates.State,
    metered: Boolean,
    onUpdate: (Updates.Release) -> Unit,
    onRestart: (Updates.Release) -> Unit,
    onLater: () -> Unit,
    onSkip: (Updates.Release) -> Unit,
    onHide: () -> Unit,
    onCancel: () -> Unit,
    onAllow: () -> Unit,
    onRetry: (Updates.Release?) -> Unit,
) {
    Surface(shape = RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.surface, modifier = Modifier.padding(horizontal = 24.dp).widthIn(max = 440.dp).fillMaxWidth()) {
        Column(Modifier.verticalScroll(rememberScrollState()).padding(24.dp).animateContentSize()) {
            when (state) {
                is Updates.State.Available -> {
                    val r = state.release
                    Head(Icons.Rounded.SystemUpdate, t("upd_out_title", r.version), t("upd_out_lead"))
                    Notes(r.notes)
                    Spacer(Modifier.height(20.dp))
                    Main(Icons.Rounded.SystemUpdate, if (r.size > 0) t("upd_now", mb(r.size)) else t("upd_now_plain")) { onUpdate(r) }
                    if (metered) Small(t("upd_metered"))
                    Spacer(Modifier.height(8.dp))
                    Second(Icons.Rounded.Snooze, t("upd_later"), onLater)
                    Quiet(Icons.Rounded.NotificationsOff, t("upd_skip")) { onSkip(r) }
                }
                is Updates.State.Downloading -> {
                    val r = state.release
                    Head(Icons.Rounded.SystemUpdate, t("upd_downloading", r.version), t("upd_downloading_lead", mb((r.size * state.done).toLong()), mb(r.size)))
                    Spacer(Modifier.height(16.dp))
                    LinearProgressIndicator(progress = { state.done }, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(16.dp))
                    Second(Icons.Rounded.KeyboardArrowDown, t("upd_hide"), onHide)
                    Quiet(Icons.Rounded.Close, t("upd_cancel"), onCancel)
                }
                is Updates.State.Ready -> {
                    val r = state.release
                    Head(Icons.Rounded.RestartAlt, t("upd_ready_title", r.version), state.note ?: t("upd_ready_lead"))
                    Spacer(Modifier.height(20.dp))
                    Main(Icons.Rounded.RestartAlt, t("upd_restart")) { onRestart(r) }
                    Spacer(Modifier.height(8.dp))
                    Second(Icons.Rounded.Snooze, t("upd_later"), onLater)
                    Quiet(Icons.Rounded.NotificationsOff, t("upd_skip")) { onSkip(r) }
                }
                is Updates.State.Installing -> {
                    Head(Icons.Rounded.RestartAlt, t("upd_installing", state.release.version), t("upd_installing_lead"))
                    Spacer(Modifier.height(16.dp))
                    LinearProgressIndicator(Modifier.fillMaxWidth())
                    Spacer(Modifier.height(16.dp))
                    Second(Icons.Rounded.Refresh, t("upd_ask_again")) { onRestart(state.release) }
                    Quiet(Icons.Rounded.KeyboardArrowDown, t("upd_hide"), onHide)
                }
                is Updates.State.Allow -> {
                    Head(Icons.Rounded.Settings, t("upd_allow_title"), t("upd_allow_lead"))
                    Spacer(Modifier.height(20.dp))
                    Main(Icons.Rounded.Settings, t("upd_open_setting"), onAllow)
                    Spacer(Modifier.height(8.dp))
                    Second(Icons.Rounded.Snooze, t("upd_later"), onLater)
                }
                is Updates.State.Failed -> {
                    Head(Icons.Rounded.SystemUpdate, state.message, t("upd_failed_lead"))
                    Spacer(Modifier.height(20.dp))
                    Main(Icons.Rounded.Refresh, t("retry")) { onRetry(state.release) }
                    Spacer(Modifier.height(8.dp))
                    Second(Icons.Rounded.Snooze, t("upd_later"), onLater)
                }
                else -> Unit
            }
        }
    }
}

private fun mb(bytes: Long) = if (bytes <= 0) "" else "${(bytes + 500_000) / 1_000_000} MB"

/** The step's icon in a soft circle, its title and what it means. */
@Composable
private fun Head(icon: ImageVector, title: String, text: String) {
    Box(Modifier.size(52.dp).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f), CircleShape), contentAlignment = Alignment.Center) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
    }
    Spacer(Modifier.height(16.dp))
    Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(6.dp))
    Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

/** What's new in it: the release notes' first few "- " lines. */
@Composable
private fun Notes(notes: String) {
    val lines = notes.lines().map { it.trim() }.filter { it.startsWith("- ") }.map { it.removePrefix("- ").replace("**", "") }.take(4)
    if (lines.isEmpty()) return
    Spacer(Modifier.height(10.dp))
    lines.forEach { line ->
        Row(Modifier.padding(vertical = 3.dp)) {
            Text("•", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(10.dp))
            Text(line, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

/** A button's inside: its icon, then its words (the owner: every button carries an icon). */
@Composable
private fun Labeled(icon: ImageVector, text: String) {
    Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
    Spacer(Modifier.width(8.dp))
    Text(text)
}

@Composable
private fun Main(icon: ImageVector, text: String, onClick: () -> Unit) =
    Button(onClick = onClick, modifier = Modifier.fillMaxWidth().height(52.dp)) { Labeled(icon, text) }

@Composable
private fun Second(icon: ImageVector, text: String, onClick: () -> Unit) =
    FilledTonalButton(
        onClick = onClick,
        colors = ButtonDefaults.filledTonalButtonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurface),
        modifier = Modifier.fillMaxWidth().height(48.dp),
    ) { Labeled(icon, text) }

@Composable
private fun Quiet(icon: ImageVector, text: String, onClick: () -> Unit) =
    Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.Center) {
        TextButton(onClick = onClick) { Labeled(icon, text) }
    }

@Composable
private fun Small(text: String) =
    Text(text, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
