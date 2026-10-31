package com.radianceph.pos

import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.security.keystore.StrongBoxUnavailableException
import android.util.Base64
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.PrivateKey
import java.security.Signature
import java.security.spec.ECGenParameterSpec

/**
 * The phone's own key (radiance-pos core/Phones, docs/android/DESIGN.md section 2): made inside the phone's hardware
 * (a StrongBox when it has one, else the TEE), never leaving it, with key attestation whose challenge is the link's
 * one-time code, so the shop can check it was made by this app on a phone that isn't rooted. Every request after
 * linking is signed with it.
 */
object Keys {
    private const val STORE = "AndroidKeyStore"

    /** The alias of the key for a shop (one shop, one key; linking again makes a new one) */
    fun alias(shop: String): String = "rpos-" + shop.replace(Regex("[^A-Za-z0-9_-]"), "")

    /**
     * A new key for [alias], attested with [challenge]; the old one (if any) is gone.
     * @return its certificate chain, the key first, each as base64 DER
     */
    fun create(alias: String, challenge: ByteArray): List<String> {
        val store = KeyStore.getInstance(STORE).apply { load(null) }
        if (store.containsAlias(alias)) store.deleteEntry(alias)
        fun spec(strongBox: Boolean) = KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_SIGN)
            .setAlgorithmParameterSpec(ECGenParameterSpec("secp256r1"))
            .setDigests(KeyProperties.DIGEST_SHA256)
            .setAttestationChallenge(challenge)
            .apply { if (strongBox && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) setIsStrongBoxBacked(true) }
            .build()
        val generator = KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_EC, STORE)
        try {
            generator.initialize(spec(strongBox = true))
            generator.generateKeyPair()
        } catch (e: StrongBoxUnavailableException) {
            generator.initialize(spec(strongBox = false))
            generator.generateKeyPair()
        } catch (e: Exception) {
            // some phones say StrongBox is there and then refuse it: the TEE then
            if (store.containsAlias(alias)) store.deleteEntry(alias)
            generator.initialize(spec(strongBox = false))
            generator.generateKeyPair()
        }
        return chain(alias)
    }

    fun chain(alias: String): List<String> {
        val store = KeyStore.getInstance(STORE).apply { load(null) }
        return store.getCertificateChain(alias).orEmpty().map { Base64.encodeToString(it.encoded, Base64.NO_WRAP) }
    }

    /** The key's public half, X.509 SubjectPublicKeyInfo, base64 */
    fun publicKey(alias: String): String {
        val store = KeyStore.getInstance(STORE).apply { load(null) }
        return Base64.encodeToString(store.getCertificate(alias).publicKey.encoded, Base64.NO_WRAP)
    }

    fun has(alias: String): Boolean = KeyStore.getInstance(STORE).apply { load(null) }.containsAlias(alias)

    fun forget(alias: String) {
        val store = KeyStore.getInstance(STORE).apply { load(null) }
        if (store.containsAlias(alias)) store.deleteEntry(alias)
    }

    /** ECDSA-SHA256 of [message], DER, base64 */
    fun sign(alias: String, message: ByteArray): String {
        val store = KeyStore.getInstance(STORE).apply { load(null) }
        val key = store.getKey(alias, null) as PrivateKey
        val signature = Signature.getInstance("SHA256withECDSA").apply {
            initSign(key)
            update(message)
        }
        return Base64.encodeToString(signature.sign(), Base64.NO_WRAP)
    }
}
