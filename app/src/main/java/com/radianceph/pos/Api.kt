package com.radianceph.pos

import android.content.Context
import android.net.Uri
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.TimeUnit

/** The link this phone has to one shop (kept in the app's own preferences, never in a backup) */
data class Link(val base: String, val shop: String, val phoneId: Int, val person: String, val shopName: String, val attested: String, val language: String)

object Links {
    private const val PREFS = "link"

    fun get(context: Context): Link? {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val base = p.getString("base", null) ?: return null
        val shop = p.getString("shop", null) ?: return null
        val id = p.getInt("phone_id", 0)
        if (id <= 0 || !Keys.has(Keys.alias(shop))) return null
        return Link(base, shop, id, p.getString("person", "") ?: "", p.getString("shop_name", "") ?: "", p.getString("attested", "") ?: "", p.getString("language", "en-US") ?: "en-US")
    }

    fun save(context: Context, link: Link) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString("base", link.base).putString("shop", link.shop).putInt("phone_id", link.phoneId)
            .putString("person", link.person).putString("shop_name", link.shopName).putString("attested", link.attested)
            .putString("language", link.language).apply()
    }

    fun forget(context: Context) {
        get(context)?.let { Keys.forget(Keys.alias(it.shop)) }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().clear().apply()
    }

    /**
     * The shop's address and the code in a link QR: "https://posv3.radianceph.com/<shop>/api/v1/link?code=<code>"
     * @return (base with its trailing slash, shop, code), or null for any other QR
     */
    fun parse(text: String): Triple<String, String, String>? {
        val at = text.indexOf("api/v1/link?")
        if (at <= 0 || !text.startsWith("https://")) return null
        val base = text.substring(0, at)
        val code = Uri.parse(text).getQueryParameter("code") ?: return null
        if (!Regex("^[A-Za-z0-9_-]{24}$").matches(code)) return null
        val shop = Uri.parse(base).pathSegments.lastOrNull() ?: return null
        return Triple(base, shop, code)
    }
}

/** A refusal from the shop, in its own words (the shop's language): error is its word, message what to tell the person */
class ApiError(val status: Int, val error: String, override val message: String) : Exception(message)

/**
 * The shop's phone API (radiance-pos modules/api, /api/v1/<what>). Every request but linking is signed with the phone's key:
 * X-Rpos-Phone, X-Rpos-Time and X-Rpos-Sig = ECDSA-SHA256 of "METHOD\nURI\nTIME\nsha256(body)". The shop refuses a
 * request more than 2 minutes off its clock; a phone whose clock is off learns the shop's from the answer's Date and
 * asks once more.
 */
class Api(private val link: Link) {
    private val alias = Keys.alias(link.shop)

    companion object {
        private val JSON = "application/json; charset=utf-8".toMediaType()
        val http: OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()

        /** seconds to add to this phone's clock to get the shop's (learned from a refusal for the clock) */
        @Volatile var skew: Long = 0

        /** Linking: the one-time code, the key's chain; no signature yet (there is no key on the shop's side) */
        fun link(base: String, code: String, chain: List<String>, publicKey: String, model: String, android: String, version: String): JSONObject {
            val body = JSONObject()
                .put("code", code).put("public_key", publicKey).put("chain", JSONArray(chain))
                .put("model", model).put("android", android).put("app_version", version)
                .toString()
            val request = Request.Builder().url(base + "api/v1/link").post(body.toRequestBody(JSON)).build()
            http.newCall(request).execute().use { r ->
                val json = runCatching { JSONObject(r.body.string()) }.getOrElse { JSONObject() }
                if (!r.isSuccessful || !json.optBoolean("ok")) throw ApiError(r.code, json.optString("error", "other"), json.optString("message", "The shop didn't answer as expected (" + r.code + ")."))
                return json
            }
        }

        private fun sha256(text: String): String = MessageDigest.getInstance("SHA-256").digest(text.toByteArray()).joinToString("") { "%02x".format(it) }

        /** What the phone signs, exactly as the shop checks it (radiance-pos core/Phones::verify) */
        fun message(method: String, uri: String, time: Long, body: String): String = method.uppercase() + "\n" + uri + "\n" + time + "\n" + sha256(body)
    }

    fun me(): JSONObject = call("GET", "api/v1/me")
    fun scan(code: String, to: String): JSONObject = call("POST", "api/v1/scan", JSONObject().put("code", code).put("to", to))
    fun item(code: String): JSONObject = call("GET", "api/v1/item?code=" + Uri.encode(code))
    fun clock(): JSONObject = call("GET", "api/v1/clock")
    /** A time in or out: the QR, and the phone's location when it has one (none: the shop's Wi-Fi may prove it, radiance-pos#203) */
    fun punch(qr: String, fix: Fix?): JSONObject = call("POST", "api/v1/punch", punchBody(qr, fix))
    fun clockSpot(locationId: Int, fix: Fix): JSONObject = call(
        "POST", "api/v1/clock_spot",
        JSONObject().put("location_id", locationId).put("lat", fix.lat).put("lng", fix.lng).put("accuracy", fix.accuracy).put("mock", fix.mock),
    )
    /** This phone's Firebase token for the shop's alerts ("" stops them); test: the shop sends this phone a test alert */
    fun push(token: String, test: Boolean = false): JSONObject = call("POST", "api/v1/push", JSONObject().put("token", token).apply { if (test) put("test", true) })
    fun count(locationId: Int, lines: List<Pair<Int, Double>>, note: String): JSONObject = call(
        "POST", "api/v1/count",
        JSONObject().put("location_id", locationId).put("note", note).put("lines", JSONArray(lines.map { JSONObject().put("item_id", it.first).put("counted", it.second) })),
    )

    private fun call(method: String, path: String, json: JSONObject? = null, again: Boolean = true): JSONObject {
        val body = if (method == "GET") "" else (json ?: JSONObject()).toString()
        val url = (link.base + path).toHttpUrlOrNull() ?: throw ApiError(0, "address", "This shop's address isn't right. Link the phone again.")
        val uri = url.encodedPath + (url.encodedQuery?.let { "?$it" } ?: "")
        val time = System.currentTimeMillis() / 1000 + skew
        val sig = Keys.sign(alias, message(method, uri, time, body).toByteArray())
        val builder = Request.Builder().url(url)
            .header("X-Rpos-Phone", link.phoneId.toString())
            .header("X-Rpos-Time", time.toString())
            .header("X-Rpos-Sig", sig)
        if (method == "GET") builder.get() else builder.method(method, body.toRequestBody(JSON))
        http.newCall(builder.build()).execute().use { r ->
            val json2 = runCatching { JSONObject(r.body.string()) }.getOrElse { JSONObject() }
            if (r.code == 401 && json2.optString("error") == "clock" && again) {
                // the shop's clock from its answer, then once more
                r.header("Date")?.let { date ->
                    runCatching { SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US).parse(date) }.getOrNull()?.let {
                        skew = it.time / 1000 - System.currentTimeMillis() / 1000
                        return call(method, path, json, again = false)
                    }
                }
            }
            if (!r.isSuccessful || !json2.optBoolean("ok")) throw ApiError(r.code, json2.optString("error", "other"), json2.optString("message", "The shop didn't answer as expected (" + r.code + ")."))
            return json2
        }
    }
}

/** What a punch sends: the QR, and the location only when the phone has one (radiance-pos#203) */
internal fun punchBody(qr: String, fix: Fix?): JSONObject {
    val body = JSONObject().put("qr", qr).put("mock", fix?.mock ?: false)
    if (fix != null) body.put("lat", fix.lat).put("lng", fix.lng).put("accuracy", fix.accuracy).put("age", fix.age)
    return body
}
