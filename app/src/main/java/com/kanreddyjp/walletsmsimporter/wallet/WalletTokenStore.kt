package com.kanreddyjp.walletsmsimporter.wallet

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.nio.charset.StandardCharsets
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class WalletTokenStore(
    context: Context
) {

    private val preferences = context.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    private val keyStore = java.security.KeyStore.getInstance(
        ANDROID_KEYSTORE
    ).apply {
        load(null)
    }

    fun saveToken(token: String) {
        val cipher = Cipher.getInstance(TRANSFORMATION)

        cipher.init(
            Cipher.ENCRYPT_MODE,
            getOrCreateKey()
        )

        val encryptedBytes = cipher.doFinal(
            token.toByteArray(StandardCharsets.UTF_8)
        )

        val encryptedToken = Base64.encodeToString(
            encryptedBytes,
            Base64.NO_WRAP
        )

        val iv = Base64.encodeToString(
            cipher.iv,
            Base64.NO_WRAP
        )

        preferences.edit()
            .putString(KEY_TOKEN, encryptedToken)
            .putString(KEY_IV, iv)
            .apply()
    }

    fun getToken(): String? {
        val encryptedToken = preferences.getString(
            KEY_TOKEN,
            null
        ) ?: return null

        val ivString = preferences.getString(
            KEY_IV,
            null
        ) ?: return null

        val encryptedBytes = Base64.decode(
            encryptedToken,
            Base64.NO_WRAP
        )

        val iv = Base64.decode(
            ivString,
            Base64.NO_WRAP
        )

        val cipher = Cipher.getInstance(TRANSFORMATION)

        cipher.init(
            Cipher.DECRYPT_MODE,
            getOrCreateKey(),
            GCMParameterSpec(GCM_TAG_LENGTH, iv)
        )

        return String(
            cipher.doFinal(encryptedBytes),
            StandardCharsets.UTF_8
        )
    }

    fun hasToken(): Boolean {
        return preferences.contains(KEY_TOKEN)
    }

    fun clearToken() {
        preferences.edit()
            .remove(KEY_TOKEN)
            .remove(KEY_IV)
            .apply()
    }

    private fun getOrCreateKey(): SecretKey {
        if (keyStore.containsAlias(KEY_ALIAS)) {
            return (keyStore.getEntry(
                KEY_ALIAS,
                null
            ) as java.security.KeyStore.SecretKeyEntry).secretKey
        }

        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            ANDROID_KEYSTORE
        )

        val keySpec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or
                KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(
                KeyProperties.ENCRYPTION_PADDING_NONE
            )
            .setUserAuthenticationRequired(false)
            .build()

        keyGenerator.init(keySpec)

        return keyGenerator.generateKey()
    }

    companion object {
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"

        private const val KEY_ALIAS =
            "wallet_sms_importer_wallet_token"

        private const val PREFS_NAME =
            "wallet_sms_importer_secure"

        private const val KEY_TOKEN =
            "wallet_api_token"

        private const val KEY_IV =
            "wallet_api_token_iv"

        private const val TRANSFORMATION =
            "AES/GCM/NoPadding"

        private const val GCM_TAG_LENGTH = 128
    }
}