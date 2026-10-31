package com.radianceph.pos

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlin.concurrent.thread

/**
 * The shop's alerts (radiance-pos#175): a voided sale, a short or over cash count, the day closed, a message or @mention
 * to the person, a new notice, a time-clock try refused. The shop never holds the Firebase key: the phone gives the
 * shop its token (api/v1/push), the shop lists what to send, and admin.radianceph.com sends it. With the app closed
 * Android shows an alert by itself (channel "alerts"); with the app open, onMessageReceived shows it. Tapping one opens
 * the shop's page it is about (only an address of the linked shop).
 */
class Push : FirebaseMessagingService() {
    override fun onNewToken(token: String) {
        Alerts.register(applicationContext, token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val n = message.notification ?: return
        Alerts.show(applicationContext, n.title ?: "", n.body ?: "", message.data["url"], n.tag ?: message.data["kind"])
    }
}

object Alerts {
    const val CHANNEL = "alerts"
    const val URL = "url"
    private const val PREFS = "alerts"

    /** The alerts' channel, made once (Android 8+ shows nothing without it); its name in the shop's language */
    fun channel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL, Words.t("alerts_channel"), NotificationManager.IMPORTANCE_HIGH).apply {
                description = Words.t("alerts_channel_help")
            },
        )
    }

    /** Whether alerts can show on this phone (Android 13+ asks; the person may turn them off in Settings) */
    fun allowed(context: Context): Boolean = NotificationManagerCompat.from(context).areNotificationsEnabled()

    /**
     * Give the linked shop this phone's token (a new one, or the first time; again after a new link): in the background,
     * quietly (no connection now: the next start tries again)
     */
    fun register(context: Context, token: String? = null, force: Boolean = false) {
        val link = Links.get(context) ?: return
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val send = { t: String ->
            val key = link.shop + "#" + link.phoneId
            if (force || prefs.getString("sent", null) != "$key:$t") {
                thread(name = "rpos-push-token") {
                    runCatching { Api(link).push(t) }.onSuccess { prefs.edit().putString("sent", "$key:$t").apply() }
                }
            }
        }
        if (token != null) {
            send(token)
        } else {
            FirebaseMessaging.getInstance().token.addOnSuccessListener { send(it) }
        }
    }

    /** A test alert to this phone, through the shop and the admin's sender, as a real one comes (This phone) */
    fun test(context: Context, done: (Result<Boolean>) -> Unit) {
        val main = Handler(Looper.getMainLooper())
        val link = Links.get(context) ?: return done(Result.failure(IllegalStateException("not linked")))
        FirebaseMessaging.getInstance().token
            .addOnSuccessListener { token ->
                thread(name = "rpos-push-test") {
                    val r = runCatching { Api(link).push(token, test = true).optBoolean("test") }
                    main.post { done(r) }
                }
            }
            .addOnFailureListener { e -> main.post { done(Result.failure(e)) } }
    }

    /** An alert shown while the app is open (Android shows the ones that come while it is closed) */
    fun show(context: Context, title: String, body: String, url: String?, tag: String?) {
        if (!allowed(context)) return
        channel(context)
        val open = Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        if (url != null) open.putExtra(URL, url)
        val tap = PendingIntent.getActivity(context, (tag ?: title).hashCode(), open, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val n = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_notify)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(tap)
            .build()
        runCatching { NotificationManagerCompat.from(context).notify(tag ?: title, 1, n) }
    }

    /** The page an alert is about, only when it is the linked shop's own address (never anything else from a message) */
    fun page(context: Context, intent: Intent?): String? {
        val url = intent?.getStringExtra(URL) ?: return null
        val link = Links.get(context) ?: return null
        return if (url.startsWith(link.base) && url.startsWith("https://")) url else null
    }
}
