package com.hypto.iam.server.utils

import com.google.gson.Gson
import com.hypto.iam.server.service.MasterKeyCache
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

data class EncryptedData(
    val data: String,
    val keyId: String,
)

object EncryptUtil : KoinComponent {
    private const val ALGORITHM = "AES/GCM/NoPadding"
    private const val KEY_ALGORITHM = "AES"
    private const val KEY_LENGTH = 16
    private val iv = IvParameterSpec(ByteArray(KEY_LENGTH))
    private val masterKeyCache: MasterKeyCache by inject()
    private val gson: Gson by inject()

    suspend fun encrypt(input: String): String {
        val cipher = Cipher.getInstance(ALGORITHM)
        val (key, keyId) = getSecretKeySpec()
        cipher.init(Cipher.ENCRYPT_MODE, key, iv)
        val cipherText = cipher.doFinal(input.toByteArray())
        return gson.toJson(
            EncryptedData(
                data = Base64.getEncoder().encodeToString(cipherText),
                keyId = keyId,
            ),
        )
    }

    suspend fun decrypt(cipherText: String): String {
        val cipher = Cipher.getInstance(ALGORITHM)
        val encryptedData = gson.fromJson(cipherText, EncryptedData::class.java)
        val (key, _) = getSecretKeySpec(encryptedData.keyId)
        cipher.init(Cipher.DECRYPT_MODE, key, iv)
        val plainText = cipher.doFinal(Base64.getDecoder().decode(encryptedData.data))
        return String(plainText)
    }

    private suspend fun getSecretKeySpec(id: String? = null): Pair<SecretKeySpec, String> {
        val masterKey =
            if (id == null) masterKeyCache.forSigning() else masterKeyCache.getKey(id)
        val key = SecretKeySpec(masterKey.privateKey.toString().take(KEY_LENGTH).toByteArray(), KEY_ALGORITHM)
        return Pair(key, masterKey.id)
    }
}
