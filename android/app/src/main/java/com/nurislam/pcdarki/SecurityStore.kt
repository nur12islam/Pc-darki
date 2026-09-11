package com.nurislam.pcdarki

import android.content.Context
import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/** Local PC-DARKI account storage. The PIN itself is never persisted. */
class SecurityStore(context: Context) {
    private val prefs = context.getSharedPreferences("pc_darki_security", Context.MODE_PRIVATE)

    val isConfigured: Boolean
        get() = prefs.contains("pin_hash") && prefs.contains("username")

    val username: String
        get() = prefs.getString("username", "User") ?: "User"

    fun createAccount(username: String, pin: String) {
        val cleanName = username.trim()
        require(cleanName.length in 1..32) { "Username must be 1–32 characters." }
        require(pin.length >= 4) { "PIN must be at least 4 characters." }

        val salt = ByteArray(16).also(SecureRandom()::nextBytes)
        val hash = derive(pin, salt)
        prefs.edit()
            .putString("username", cleanName)
            .putString("pin_salt", Base64.encodeToString(salt, Base64.NO_WRAP))
            .putString("pin_hash", Base64.encodeToString(hash, Base64.NO_WRAP))
            .apply()
    }

    fun verifyPin(pin: String): Boolean {
        val saltText = prefs.getString("pin_salt", null) ?: return false
        val expectedText = prefs.getString("pin_hash", null) ?: return false
        return try {
            val salt = Base64.decode(saltText, Base64.NO_WRAP)
            val expected = Base64.decode(expectedText, Base64.NO_WRAP)
            MessageDigest.isEqual(derive(pin, salt), expected)
        } catch (_: IllegalArgumentException) {
            false
        }
    }

    private fun derive(pin: String, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(pin.toCharArray(), salt, 120_000, 256)
        return try {
            SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
    }
}
