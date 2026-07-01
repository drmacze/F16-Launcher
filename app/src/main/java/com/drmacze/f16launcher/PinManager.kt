package com.drmacze.f16launcher

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * PIN Manager — stores a 6-digit launcher PIN securely using Android Keystore.
 *
 * Flow:
 *  - setupPin(context, pin): creates AES key in Keystore, encrypts PIN, stores ciphertext
 *  - verifyPin(context, pin): decrypts ciphertext, compares to user input
 *  - hasPin(context): true if PIN is set
 *  - clearPin(context): removes PIN + key
 *
 * Security:
 *  - AES-256-GCM via Android Keystore (key never leaves secure hardware on supported devices)
 *  - 6-digit numeric PIN (caller enforces length)
 *  - Encrypted ciphertext stored in SharedPreferences (not the PIN itself)
 *
 * Note: Keystore key alias is per-app, so uninstalling the app clears the key.
 */
object PinManager {
    private const val PREF_NAME   = "dlavie_pin"
    private const val KEY_CIPHER  = "pin_cipher"
    private const val KEY_IV      = "pin_iv"
    private const val KEYSET_ALIAS = "dlavie_launcher_pin_key"
    private const val GCM_TAG_BITS = 128

    private fun prefs(ctx: Context) =
        ctx.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    fun hasPin(ctx: Context): Boolean =
        prefs(ctx).getString(KEY_CIPHER, null) != null

    /**
     * Set or replace the launcher PIN.
     * @param pin 6-digit string (caller must validate length & numeric)
     */
    fun setupPin(ctx: Context, pin: String): Boolean {
        if (pin.length != 6 || !pin.all { it.isDigit() }) return false
        return try {
            // Remove old key if any (for replace case)
            clearPin(ctx)
            val key = getOrCreateKey()
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, key)
            val iv = cipher.iv
            val ct = cipher.doFinal(pin.toByteArray(Charsets.UTF_8))
            prefs(ctx).edit()
                .putString(KEY_CIPHER, Base64.encodeToString(ct, Base64.NO_WRAP))
                .putString(KEY_IV, Base64.encodeToString(iv, Base64.NO_WRAP))
                .apply()
            true
        } catch (_: Throwable) { false }
    }

    /** Returns true if PIN matches the stored one. */
    fun verifyPin(ctx: Context, pin: String): Boolean {
        if (!hasPin(ctx)) return false
        if (pin.length != 6 || !pin.all { it.isDigit() }) return false
        return try {
            val cipherB64 = prefs(ctx).getString(KEY_CIPHER, null) ?: return false
            val ivB64     = prefs(ctx).getString(KEY_IV, null)     ?: return false
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            val key = getKey() ?: return false
            cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, Base64.decode(ivB64, Base64.NO_WRAP)))
            val plain = cipher.doFinal(Base64.decode(cipherB64, Base64.NO_WRAP)).toString(Charsets.UTF_8)
            plain == pin
        } catch (_: Throwable) { false }
    }

    /** Remove the PIN and the underlying Keystore key. */
    fun clearPin(ctx: Context) {
        prefs(ctx).edit().clear().apply()
        try {
            val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
            if (ks.containsAlias(KEYSET_ALIAS)) ks.deleteEntry(KEYSET_ALIAS)
        } catch (_: Throwable) { }
    }

    // ─── Internal helpers ─────────────────────────────────────────────────────

    private fun getOrCreateKey(): SecretKey {
        val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        if (ks.containsAlias(KEYSET_ALIAS)) {
            return (ks.getEntry(KEYSET_ALIAS, null) as KeyStore.SecretKeyEntry).secretKey
        }
        val gen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        gen.init(
            KeyGenParameterSpec.Builder(
                KEYSET_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setUserAuthenticationRequired(false)
                .build()
        )
        return gen.generateKey()
    }

    private fun getKey(): SecretKey? = try {
        val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (ks.getEntry(KEYSET_ALIAS, null) as KeyStore.SecretKeyEntry).secretKey
    } catch (_: Throwable) { null }
}
