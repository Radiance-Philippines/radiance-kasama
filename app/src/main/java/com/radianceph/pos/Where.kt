package com.radianceph.pos

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Looper
import android.os.SystemClock
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

/** Where the phone is at a punch: the fix, its accuracy (m), its age (s), and whether Android says it is a mock */
data class Fix(val lat: Double, val lng: Double, val accuracy: Double, val age: Double, val mock: Boolean)

/**
 * The phone's location for the time clock (radiance-pos#173): a fresh fix from GPS (or the network, indoors), asked
 * only at a punch and never kept. The platform's LocationManager, no Google Play services. A fake-location app is
 * caught: Android marks its fixes as mock (Location.isMock, API 31; isFromMockProvider before).
 */
object Where {
    @SuppressLint("MissingPermission")
    suspend fun fix(context: Context, timeoutMs: Long = 20000): Fix? {
        val manager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER).filter { runCatching { manager.isProviderEnabled(it) }.getOrDefault(false) }
        if (providers.isEmpty()) return null
        val location = withTimeoutOrNull(timeoutMs) {
            suspendCancellableCoroutine<Location?> { cont ->
                var best: Location? = null
                val listener = object : LocationListener {
                    override fun onLocationChanged(l: Location) {
                        if (best == null || l.accuracy < best!!.accuracy) best = l
                        // good enough for a shop: within 25 m, or the best after a few fixes
                        if (l.accuracy <= 25f && cont.isActive) {
                            manager.removeUpdates(this)
                            cont.resume(l)
                        }
                    }
                    override fun onProviderDisabled(provider: String) {}
                    override fun onProviderEnabled(provider: String) {}
                }
                providers.forEach { manager.requestLocationUpdates(it, 500L, 0f, listener, Looper.getMainLooper()) }
                cont.invokeOnCancellation { manager.removeUpdates(listener) }
                // after 12 s, the best one so far
                android.os.Handler(Looper.getMainLooper()).postDelayed({
                    if (cont.isActive && best != null) {
                        manager.removeUpdates(listener)
                        cont.resume(best)
                    }
                }, 12000)
            }
        } ?: return null
        val age = (SystemClock.elapsedRealtimeNanos() - location.elapsedRealtimeNanos) / 1e9
        @Suppress("DEPRECATION")
        val mock = if (Build.VERSION.SDK_INT >= 31) location.isMock else location.isFromMockProvider
        return Fix(location.latitude, location.longitude, location.accuracy.toDouble(), age, mock)
    }
}
