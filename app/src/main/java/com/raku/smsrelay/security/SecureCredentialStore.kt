package com.raku.smsrelay.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class SecureCredentialStore(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun hasAuthorizationCode(): Boolean = preferences.contains(KEY_AUTHORIZATION_CODE)

    fun read(): String {
        val encoded = preferences.getString(KEY_AUTHORIZATION_CODE, null) ?: return ""
        return runCatching {
            val bytes = Base64.decode(encoded, Base64.NO_WRAP)
            require(bytes.size > IV_LENGTH_BYTES)
            val iv = bytes.copyOfRange(0, IV_LENGTH_BYTES)
            val ciphertext = bytes.copyOfRange(IV_LENGTH_BYTES, bytes.size)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), GCMParameterSpec(TAG_LENGTH_BITS, iv))
            cipher.doFinal(ciphertext).toString(Charsets.UTF_8)
        }.getOrElse {
            clear()
            ""
        }
    }

    fun write(authorizationCode: String) {
        if (authorizationCode.isBlank()) {
            clear()
            return
        }
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        val encrypted = cipher.doFinal(authorizationCode.toByteArray(Charsets.UTF_8))
        val payload = cipher.iv + encrypted
        preferences.edit()
            .putString(KEY_AUTHORIZATION_CODE, Base64.encodeToString(payload, Base64.NO_WRAP))
            .apply()
    }

    fun clear() {
        preferences.edit().remove(KEY_AUTHORIZATION_CODE).apply()
    }

    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }

        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE_PROVIDER).run {
            init(
                KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setRandomizedEncryptionRequired(true)
                    .build(),
            )
            generateKey()
        }
    }

    private companion object {
        const val PREFERENCES_NAME = "secure-smtp-credentials"
        const val KEY_AUTHORIZATION_CODE = "smtp-authorization-code"
        const val KEY_ALIAS = "sms-relay-smtp-authorization-code-v1"
        const val KEYSTORE_PROVIDER = "AndroidKeyStore"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val IV_LENGTH_BYTES = 12
        const val TAG_LENGTH_BITS = 128
    }
}
