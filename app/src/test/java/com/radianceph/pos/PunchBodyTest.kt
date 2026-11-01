package com.radianceph.pos

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** A punch sends the location only when the phone has one; without it the shop's Wi-Fi may prove it (radiance-pos#203) */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class PunchBodyTest {
    @Test fun withALocationItIsSent() {
        val body = punchBody("rposclock:1:demo:1:5:abc", Fix(13.9411, 121.1631, 12.0, 3.0, false))
        assertEquals(13.9411, body.getDouble("lat"), 0.0)
        assertEquals(121.1631, body.getDouble("lng"), 0.0)
        assertEquals(12.0, body.getDouble("accuracy"), 0.0)
        assertEquals(3.0, body.getDouble("age"), 0.0)
        assertFalse(body.getBoolean("mock"))
    }

    @Test fun withoutOneOnlyTheQrGoes() {
        val body = punchBody("rposclock:1:demo:1:5:abc", null)
        assertEquals("rposclock:1:demo:1:5:abc", body.getString("qr"))
        assertFalse(body.getBoolean("mock"))
        for (key in listOf("lat", "lng", "accuracy", "age")) assertFalse(key, body.has(key))
    }

    @Test fun aFakeLocationIsStillSaid() {
        assertTrue(punchBody("q", Fix(0.0, 0.0, 5.0, 1.0, true)).getBoolean("mock"))
    }
}
