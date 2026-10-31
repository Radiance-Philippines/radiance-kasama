package com.radianceph.pos

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** The link QR read (the shop's address, its name, the one-time code) and the message the phone signs */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class LinksTest {
    @Test fun aLinkQrGivesTheShopAndTheCode() {
        val (base, shop, code) = Links.parse("https://posv3.radianceph.com/pgt/api/v1/link?code=AbCdEfGhIjKlMnOpQrStUv-_")!!
        assertEquals("https://posv3.radianceph.com/pgt/", base)
        assertEquals("pgt", shop)
        assertEquals("AbCdEfGhIjKlMnOpQrStUv-_", code)
    }

    @Test fun anythingElseIsNotALink() {
        assertNull(Links.parse("https://radianceph.com/refer/?c=ABCDEFG"))
        assertNull(Links.parse("http://posv3.radianceph.com/pgt/api/v1/link?code=AbCdEfGhIjKlMnOpQrStUv-_"))
        assertNull(Links.parse("https://posv3.radianceph.com/pgt/api/v1/link?code=short"))
        assertNull(Links.parse("4800016644115"))
    }

    @Test fun theSignedMessageIsTheShopsOwn() {
        // radiance-pos core/Phones::verify: "METHOD\nURI\nTIME\nsha256(body)"; sha256("") is e3b0c442…
        assertEquals("GET\n/pgt/api/v1/me\n1793430000\ne3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855", Api.message("get", "/pgt/api/v1/me", 1793430000, ""))
        assertEquals("44136fa355b3678a1146ad16f7e8649e94fb4fc21fe77e8310c060f61caaff8a", Api.message("POST", "/x", 1, "{}").substringAfterLast("\n"))
    }
}
