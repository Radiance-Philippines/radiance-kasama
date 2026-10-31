package com.radianceph.pos.ui

import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.LinkOff
import androidx.compose.material.icons.rounded.PointOfSale
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.automirrored.rounded.Login
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.Sell
import androidx.compose.material.icons.rounded.Smartphone
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.radianceph.pos.Api
import com.radianceph.pos.ApiError
import com.radianceph.pos.Keys
import com.radianceph.pos.Link
import com.radianceph.pos.Links
import com.radianceph.pos.Words
import com.radianceph.pos.Words.t
import com.radianceph.pos.Where
import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException

internal enum class Page { HOME, SCAN_PC, PRICE, COUNT, CLOCK, PHONE }

/** What a refusal or a dropped connection says to the person */
private fun said(e: Throwable): String = when (e) {
    is ApiError -> e.message
    is IOException -> t("offline")
    else -> e.message ?: t("offline")
}

/** The app: link once, then Home and its pages */
@Composable
fun App() {
    val context = LocalContext.current
    var link by remember { mutableStateOf(Links.get(context)) }
    val current = link
    if (current == null) {
        LinkScreen(onLinked = { link = it })
        return
    }
    Words.use(current.language)
    val api = remember(current) { Api(current) }
    var me by remember(current) { mutableStateOf<JSONObject?>(null) }
    var error by remember(current) { mutableStateOf<String?>(null) }
    var page by remember { mutableStateOf(Page.HOME) }
    val scope = rememberCoroutineScope()
    val load = {
        error = null
        scope.launch {
            runCatching { withContext(Dispatchers.IO) { api.me() } }
                .onSuccess { me = it }
                .onFailure {
                    error = said(it)
                    // unlinked by the owner: back to linking
                    if (it is ApiError && it.error == "unlinked") { Links.forget(context); link = null }
                }
        }
    }
    LaunchedEffect(current) { load() }
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        when (page) {
            Page.HOME -> Home(current, me, error, onRetry = { load() }, onOpen = { page = it })
            Page.SCAN_PC -> ScanToPc(api, me, onBack = { page = Page.HOME })
            Page.PRICE -> PriceCheck(api, onBack = { page = Page.HOME })
            Page.COUNT -> CountStock(api, me, onBack = { page = Page.HOME })
            Page.CLOCK -> TimeClock(api, onBack = { page = Page.HOME })
            Page.PHONE -> ThisPhone(current, onBack = { page = Page.HOME }, onForget = { Links.forget(context); link = null })
        }
    }
}

@Composable
private fun Header(title: String, onBack: (() -> Unit)? = null) {
    Row(Modifier.fillMaxWidth().padding(start = 8.dp, end = 16.dp, top = 12.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        if (onBack != null) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = t("back")) }
        } else {
            Spacer(Modifier.width(8.dp))
        }
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
    }
}

/** A button with its icon first (the owner: every button carries an icon) */
@Composable
private fun Labeled(icon: ImageVector, text: String) {
    Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
    Spacer(Modifier.width(8.dp))
    Text(text)
}

// ---- linking ----

@Composable
fun LinkScreen(onLinked: (Link) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var scanning by remember { mutableStateOf(false) }
    var working by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var done by remember { mutableStateOf<Pair<Link, String>?>(null) }
    Words.use(java.util.Locale.getDefault().language.let { if (it == "tl" || it == "fil") "tl-PH" else "en-US" })
    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(t("link_title"), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        val finished = done
        if (finished != null) {
            Text(finished.second, style = MaterialTheme.typography.bodyLarge)
            Button(onClick = { onLinked(finished.first) }, modifier = Modifier.fillMaxWidth().height(52.dp)) { Labeled(Icons.Rounded.CheckCircle, "OK") }
            return@Column
        }
        Text(t("link_lead"), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        if (working) {
            Row(verticalAlignment = Alignment.CenterVertically) { CircularProgressIndicator(Modifier.size(24.dp)); Spacer(Modifier.width(12.dp)); Text(t("link_working")) }
        } else if (scanning) {
            Scanner(Codes.QR, modifier = Modifier.fillMaxWidth().height(360.dp), onFound = { text ->
                val parsed = Links.parse(text)
                if (parsed == null) {
                    error = t("not_a_link")
                    return@Scanner
                }
                scanning = false
                working = true
                error = null
                val (base, shop, code) = parsed
                scope.launch {
                    runCatching {
                        withContext(Dispatchers.IO) {
                            val alias = Keys.alias(shop)
                            val chain = Keys.create(alias, code.toByteArray(Charsets.UTF_8))
                            val version = context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: ""
                            Api.link(base, code, chain, Keys.publicKey(alias), Build.MANUFACTURER + " " + Build.MODEL, Build.VERSION.RELEASE, version)
                        }
                    }.onSuccess { r ->
                        val person = r.getJSONObject("person")
                        val shopInfo = r.getJSONObject("shop")
                        val l = Link(base, shop, r.getInt("phone_id"), person.optString("name"), shopInfo.optString("name"), r.optString("attested"), shopInfo.optString("language", "en-US"))
                        Links.save(context, l)
                        Words.use(l.language)
                        done = l to (if (l.attested.isNotEmpty()) t("linked_proven") else t("linked_scan_only", r.optString("attestation")))
                    }.onFailure { error = said(it) }
                    working = false
                }
            })
            OutlinedButton(onClick = { scanning = false }, modifier = Modifier.fillMaxWidth()) { Labeled(Icons.Rounded.Close, t("cancel")) }
        } else {
            Button(onClick = { scanning = true; error = null }, modifier = Modifier.fillMaxWidth().height(56.dp)) { Labeled(Icons.Rounded.QrCodeScanner, t("link_scan")) }
        }
    }
}

// ---- Home ----

@Composable
internal fun Home(link: Link, me: JSONObject?, error: String?, onRetry: () -> Unit, onOpen: (Page) -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text(t("hi", me?.optJSONObject("person")?.optString("first_name")?.ifEmpty { null } ?: link.person), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(me?.optJSONObject("shop")?.optString("name") ?: link.shopName, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (error != null) {
            Text(error, color = MaterialTheme.colorScheme.error)
            OutlinedButton(onClick = onRetry) { Labeled(Icons.Rounded.Refresh, t("retry")) }
        }
        if (me == null && error == null) {
            Row(verticalAlignment = Alignment.CenterVertically) { CircularProgressIndicator(Modifier.size(22.dp)); Spacer(Modifier.width(12.dp)); Text(t("loading")) }
        }
        val can = me?.optJSONObject("can")
        if (can?.optBoolean("scan_register") == true || can?.optBoolean("scan_receiving") == true) {
            Tile(Icons.Rounded.PointOfSale, t("scan_to_pc"), t("scan_to_pc_help")) { onOpen(Page.SCAN_PC) }
        }
        if (can?.optBoolean("price_check") == true) {
            Tile(Icons.Rounded.Sell, t("price_check"), t("price_check_help")) { onOpen(Page.PRICE) }
        }
        if (can?.optBoolean("count") == true) {
            Tile(Icons.Rounded.Inventory2, t("count"), t("count_help")) { onOpen(Page.COUNT) }
        }
        if (me != null) {
            Tile(Icons.Rounded.Schedule, t("clock"), if (link.attested.isNotEmpty()) t("clock_help") else t("clock_not_proven")) { onOpen(Page.CLOCK) }
        }
        Tile(Icons.Rounded.Smartphone, t("settings"), if (link.attested.isNotEmpty()) t("proven") else t("scan_only")) { onOpen(Page.PHONE) }
    }
}

@Composable
private fun Tile(icon: ImageVector, title: String, help: String, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(10.dp).size(28.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column {
                Text(title, fontWeight = FontWeight.SemiBold, fontSize = 17.sp)
                Text(help, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
            }
        }
    }
}

// ---- scan for the PC ----

private data class Sent(val code: String, val ok: Boolean, val text: String)

@Composable
private fun ScanToPc(api: Api, me: JSONObject?, onBack: () -> Unit) {
    val can = me?.optJSONObject("can")
    val targets = listOfNotNull("register".takeIf { can?.optBoolean("scan_register") == true }, "receiving".takeIf { can?.optBoolean("scan_receiving") == true })
    var to by remember { mutableStateOf(targets.firstOrNull() ?: "register") }
    val sent = remember { mutableStateListOf<Sent>() }
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    Column(Modifier.fillMaxSize()) {
        Header(t("scan_to_pc"), onBack)
        if (targets.size > 1) {
            Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                targets.forEach { target -> FilterChip(selected = to == target, onClick = { to = target }, label = { Text(t(target)) }) }
            }
        }
        Scanner(Codes.ITEMS, modifier = Modifier.fillMaxWidth().height(300.dp).padding(16.dp), onFound = { code ->
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            scope.launch {
                val r = runCatching { withContext(Dispatchers.IO) { api.scan(code, to) } }
                sent.add(0, Sent(code, r.isSuccess, if (r.isSuccess) t("sent") else said(r.exceptionOrNull()!!)))
            }
        })
        Text(t("open_pc"), color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 16.dp))
        LazyColumn(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items(sent) { s ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(if (s.ok) Icons.Rounded.CheckCircle else Icons.Rounded.ErrorOutline, contentDescription = null, tint = if (s.ok) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.error)
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text(s.code, fontWeight = FontWeight.SemiBold)
                        Text(s.text, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

// ---- price check ----

@Composable
private fun PriceCheck(api: Api, onBack: () -> Unit) {
    var item by remember { mutableStateOf<JSONObject?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    Column(Modifier.fillMaxSize()) {
        Header(t("price_check"), onBack)
        val shown = item
        if (shown == null && error == null) {
            Scanner(Codes.ITEMS, paused = busy, modifier = Modifier.fillMaxWidth().height(320.dp).padding(16.dp), onFound = { code ->
                busy = true
                scope.launch {
                    runCatching { withContext(Dispatchers.IO) { api.item(code) } }
                        .onSuccess { item = it.getJSONObject("item") }
                        .onFailure { error = said(it) }
                    busy = false
                }
            })
            Text(t("aim"), color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 16.dp))
        } else {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (shown != null) PriceResult(shown)
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                Button(onClick = { item = null; error = null }, modifier = Modifier.fillMaxWidth().height(52.dp)) { Labeled(Icons.Rounded.QrCodeScanner, t("scan_another")) }
            }
        }
    }
}

/** An item as price check shows it: name, barcode, price, the stock in the person's branches */
@Composable
internal fun PriceResult(shown: JSONObject) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(shown.optString("name"), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(shown.optString("barcode"), color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(t("price") + ": " + shown.optString("price_text"), style = MaterialTheme.typography.titleLarge)
        val stock = shown.optJSONArray("stock")
        if (stock != null && stock.length() > 0) {
            Text(t("stock"), fontWeight = FontWeight.SemiBold)
            for (i in 0 until stock.length()) {
                val s = stock.getJSONObject(i)
                Text(s.optString("name") + ": " + s.optString("text"))
            }
        }
    }
}

// ---- counting ----

private class Counted(val itemId: Int, val name: String, initial: Double) {
    var counted by mutableStateOf(initial)
}

@Composable
private fun CountStock(api: Api, me: JSONObject?, onBack: () -> Unit) {
    val branches = me?.optJSONArray("count_branches")
    val choices = (0 until (branches?.length() ?: 0)).map { branches!!.getJSONObject(it) }.map { it.getInt("location_id") to it.optString("name") }
    var branch by remember { mutableStateOf(choices.firstOrNull()) }
    val lines = remember { mutableStateListOf<Counted>() }
    var message by remember { mutableStateOf<String?>(null) }
    var asking by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    Column(Modifier.fillMaxSize()) {
        Header(t("count"), onBack)
        if (choices.size > 1) {
            Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                choices.forEach { c -> FilterChip(selected = branch == c, onClick = { branch = c }, label = { Text(c.second) }) }
            }
        }
        Scanner(Codes.ITEMS, paused = busy, modifier = Modifier.fillMaxWidth().height(220.dp).padding(16.dp), onFound = { code ->
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            scope.launch {
                runCatching { withContext(Dispatchers.IO) { api.item(code) } }
                    .onSuccess { r ->
                        val it = r.getJSONObject("item")
                        if (!it.optBoolean("counted")) return@onSuccess
                        val id = it.getInt("item_id")
                        val have = lines.firstOrNull { l -> l.itemId == id }
                        if (have != null) have.counted += 1.0 else lines.add(0, Counted(id, it.optString("name"), 1.0))
                        message = null
                    }
                    .onFailure { message = said(it) }
            }
        })
        message?.let { Text(it, modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.onSurfaceVariant) }
        if (lines.isEmpty()) {
            Text(t("nothing_counted"), modifier = Modifier.padding(16.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        LazyColumn(Modifier.weight(1f).fillMaxWidth().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items(lines, key = { it.itemId }) { line ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(line.name, modifier = Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                    IconButton(onClick = { if (line.counted >= 1) line.counted -= 1.0 }) { Icon(Icons.Rounded.Remove, contentDescription = "-") }
                    OutlinedTextField(
                        value = if (line.counted % 1.0 == 0.0) line.counted.toLong().toString() else line.counted.toString(),
                        onValueChange = { v -> v.toDoubleOrNull()?.let { if (it >= 0) line.counted = it } },
                        modifier = Modifier.width(84.dp), singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    )
                    IconButton(onClick = { line.counted += 1.0 }) { Icon(Icons.Rounded.Add, contentDescription = "+") }
                }
            }
        }
        Box(Modifier.padding(16.dp)) {
            Button(onClick = { asking = true }, enabled = lines.isNotEmpty() && branch != null && !busy, modifier = Modifier.fillMaxWidth().height(52.dp)) { Labeled(Icons.Rounded.Save, t("send_count")) }
        }
    }
    val chosen = branch
    if (asking && chosen != null) {
        AlertDialog(
            onDismissRequest = { asking = false },
            text = { Text(t("confirm_count", lines.size, chosen.second)) },
            confirmButton = {
                TextButton(onClick = {
                    asking = false
                    busy = true
                    scope.launch {
                        runCatching { withContext(Dispatchers.IO) { api.count(chosen.first, lines.map { it.itemId to it.counted }, "") } }
                            .onSuccess { r ->
                                val done = r.getJSONArray("lines")
                                val names = lines.associate { it.itemId to it.name }
                                message = t("count_saved") + "\n" + (0 until done.length()).map { done.getJSONObject(it) }.filter { it.optBoolean("ok") }
                                    .joinToString("\n") { names[it.getInt("item_id")].orEmpty() + ": " + t("before_after", it.opt("before").toString(), it.opt("after").toString()) }
                                lines.clear()
                            }
                            .onFailure { message = said(it) }
                        busy = false
                    }
                }) { Labeled(Icons.Rounded.Save, t("yes")) }
            },
            dismissButton = { TextButton(onClick = { asking = false }) { Labeled(Icons.Rounded.Close, t("cancel")) } },
        )
    }
}

// ---- the time clock (radiance-pos#173) ----

@Composable
private fun TimeClock(api: Api, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    var state by remember { mutableStateOf<JSONObject?>(null) }
    var message by remember { mutableStateOf<String?>(null) }
    var scanning by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf<String?>(null) }
    val allowed = { ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED }
    var permitted by remember { mutableStateOf(allowed()) }
    val ask = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permitted = allowed(); if (!permitted) message = t("location_denied") }
    val load = {
        scope.launch {
            runCatching { withContext(Dispatchers.IO) { api.clock() } }.onSuccess { state = it }.onFailure { message = said(it) }
        }
    }
    LaunchedEffect(Unit) { load(); if (!permitted) ask.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Header(t("clock"), onBack)
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            val s = state
            if (s == null) {
                Row(verticalAlignment = Alignment.CenterVertically) { CircularProgressIndicator(Modifier.size(22.dp)); Spacer(Modifier.width(12.dp)); Text(t("loading_clock")) }
            } else if (!s.optBoolean("proven")) {
                Text(t("clock_not_proven"), color = MaterialTheme.colorScheme.error)
            } else if (scanning) {
                Scanner(Codes.QR, paused = busy != null, modifier = Modifier.fillMaxWidth().height(320.dp), onFound = { qr ->
                    if (!qr.startsWith("rposclock:")) return@Scanner
                    busy = t("finding")
                    scope.launch {
                        val fix = Where.fix(context)
                        if (fix == null) {
                            message = t("location_off")
                        } else {
                            runCatching { withContext(Dispatchers.IO) { api.punch(qr, fix) } }
                                .onSuccess { r -> message = r.optString("message"); haptic.performHapticFeedback(HapticFeedbackType.LongPress) }
                                .onFailure { message = said(it) }
                        }
                        busy = null
                        scanning = false
                        load()
                    }
                })
                Text(t("scan_clock"), color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedButton(onClick = { scanning = false }, modifier = Modifier.fillMaxWidth()) { Labeled(Icons.Rounded.Close, t("cancel")) }
            } else {
                val next = s.optString("next", "in")
                Button(
                    onClick = { message = null; if (permitted) scanning = true else ask.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)) },
                    modifier = Modifier.fillMaxWidth().height(64.dp),
                ) { Labeled(if (next == "in") Icons.AutoMirrored.Rounded.Login else Icons.AutoMirrored.Rounded.Logout, t(if (next == "in") "time_in" else "time_out")) }
            }
            busy?.let { Row(verticalAlignment = Alignment.CenterVertically) { CircularProgressIndicator(Modifier.size(22.dp)); Spacer(Modifier.width(12.dp)); Text(it) } }
            message?.let { Text(it, style = MaterialTheme.typography.titleMedium) }
            val today = s?.optJSONArray("today")
            if (today != null && today.length() > 0) {
                Text(t("today"), fontWeight = FontWeight.SemiBold)
                for (i in 0 until today.length()) {
                    val p = today.getJSONObject(i)
                    Text(t(if (p.optString("kind") == "in") "time_in" else "time_out") + " · " + p.optString("time") + " · " + p.optString("branch"))
                }
            }
            if (s?.optBoolean("can_set_spot") == true && !scanning) {
                Spacer(Modifier.height(8.dp))
                Text(t("set_spot_help"), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                val branches = s.optJSONArray("branches")
                for (i in 0 until (branches?.length() ?: 0)) {
                    val b = branches!!.getJSONObject(i)
                    OutlinedButton(enabled = busy == null, onClick = {
                        busy = t("finding")
                        scope.launch {
                            val fix = if (permitted) Where.fix(context) else null
                            if (fix == null) message = t("location_off") else runCatching { withContext(Dispatchers.IO) { api.clockSpot(b.getInt("location_id"), fix) } }
                                .onSuccess { message = t("spot_saved", b.optString("name")) }.onFailure { message = said(it) }
                            busy = null
                            load()
                        }
                    }, modifier = Modifier.fillMaxWidth()) { Labeled(Icons.Rounded.MyLocation, t("set_spot") + " · " + b.optString("name")) }
                }
            }
        }
    }
}

// ---- this phone ----

@Composable
internal fun ThisPhone(link: Link, onBack: () -> Unit, onForget: () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        Header(t("settings"), onBack)
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(t("linked_to", link.shopName, link.person))
            Text(if (link.attested.isNotEmpty()) t("proven") else t("scan_only"), fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(12.dp))
            OutlinedButton(onClick = onForget, modifier = Modifier.fillMaxWidth()) { Labeled(Icons.Rounded.LinkOff, t("unlink_here")) }
            Text(t("unlink_here_help"), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
        }
    }
}
