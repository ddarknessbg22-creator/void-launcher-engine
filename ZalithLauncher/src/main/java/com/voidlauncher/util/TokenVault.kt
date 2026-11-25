package com.voidlauncher.util

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Microsoft tokens are encrypted with a hardware-backed Android Keystore AES-GCM key.
 * Only the IV and ciphertext touch SharedPreferences; no plaintext token is ever persisted.
 */
object TokenVault {
    private const val PREFS = "void_token_vault"
    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val IV_KEY_SUFFIX = ".iv"

    private lateinit var prefs: SharedPreferences

    fun init(context: Context) {
        prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    }

    fun store(alias: String, secret: String) {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key(alias))
        val iv = cipher.iv
        val cipherText = cipher.doFinal(secret.toByteArray(Charsets.UTF_8))
        prefs.edit()
            .putString(alias, android.util.Base64.encodeToString(cipherText, android.util.Base64.DEFAULT))
            .putString(alias + IV_KEY_SUFFIX, android.util.Base64.encodeToString(iv, android.util.Base64.DEFAULT))
            .apply()
    }

    fun read(alias: String): String? {
        val encoded = prefs.getString(alias, null) ?: return null
        val ivEncoded = prefs.getString(alias + IV_KEY_SUFFIX, null) ?: return null
        return try {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(
                Cipher.DECRYPT_MODE,
                key(alias),
                GCMParameterSpec(128, android.util.Base64.decode(ivEncoded, android.util.Base64.DEFAULT)),
            )
            val plain = cipher.doFinal(android.util.Base64.decode(encoded, android.util.Base64.DEFAULT))
            String(plain, Charsets.UTF_8)
        } catch (e: Exception) {
            null
        }
    }

    fun delete(alias: String) {
        prefs.edit().remove(alias).remove(alias + IV_KEY_SUFFIX).apply()
        try {
            KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }.deleteEntry(alias)
        } catch (_: Exception) {
        }
    }

    private fun key(alias: String): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (keyStore.getKey(alias, null) as? SecretKey)?.let { return it }
        val spec = KeyGenParameterSpec.Builder(
            alias,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .build()
        KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE).apply {
            init(spec)
            generateKey()
        }
        return keyStore.getKey(alias, null) as SecretKey
    }
}
