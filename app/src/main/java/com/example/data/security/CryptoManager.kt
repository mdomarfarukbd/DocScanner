package com.example.data.security

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * High-performance AES-256-GCM encryption manager backed by Android KeyStore.
 * All captured documents and processed images are encrypted on disk to protect user privacy.
 */
class CryptoManager(private val context: Context) {

    companion object {
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val KEY_ALIAS = "doc_scanner_master_key_v1"
        private const val AES_TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_IV_LENGTH = 12
        private const val GCM_TAG_LENGTH = 128
    }

    private val keyStore: KeyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply {
        load(null)
    }

    private fun getOrCreateSecretKey(): SecretKey {
        if (keyStore.containsAlias(KEY_ALIAS)) {
            val entry = keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry
            if (entry != null) {
                return entry.secretKey
            }
        }

        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            ANDROID_KEYSTORE
        )
        val spec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .setRandomizedEncryptionRequired(true)
            .build()

        keyGenerator.init(spec)
        return keyGenerator.generateKey()
    }

    fun encrypt(data: ByteArray): ByteArray {
        val cipher = Cipher.getInstance(AES_TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateSecretKey())
        val iv = cipher.iv
        val encrypted = cipher.doFinal(data)

        // Prepend IV (12 bytes) to ciphertext
        val output = ByteArray(iv.size + encrypted.size)
        System.arraycopy(iv, 0, output, 0, iv.size)
        System.arraycopy(encrypted, 0, output, iv.size, encrypted.size)
        return output
    }

    fun decrypt(encryptedData: ByteArray): ByteArray {
        if (encryptedData.size < GCM_IV_LENGTH) {
            throw IllegalArgumentException("Invalid encrypted data length")
        }
        val iv = ByteArray(GCM_IV_LENGTH)
        val cipherText = ByteArray(encryptedData.size - GCM_IV_LENGTH)
        System.arraycopy(encryptedData, 0, iv, 0, GCM_IV_LENGTH)
        System.arraycopy(encryptedData, GCM_IV_LENGTH, cipherText, 0, cipherText.size)

        val cipher = Cipher.getInstance(AES_TRANSFORMATION)
        val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
        cipher.init(Cipher.DECRYPT_MODE, getOrCreateSecretKey(), spec)
        return cipher.doFinal(cipherText)
    }

    fun saveEncryptedBitmap(bitmap: Bitmap, file: File, quality: Int = 90) {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, stream)
        val rawBytes = stream.toByteArray()
        val encrypted = encrypt(rawBytes)
        
        file.parentFile?.mkdirs()
        FileOutputStream(file).use { fos ->
            fos.write(encrypted)
            fos.flush()
        }
    }

    fun loadDecryptedBitmap(file: File): Bitmap? {
        if (!file.exists()) return null
        return try {
            val encryptedBytes = FileInputStream(file).use { it.readBytes() }
            val decryptedBytes = decrypt(encryptedBytes)
            BitmapFactory.decodeByteArray(decryptedBytes, 0, decryptedBytes.size)
        } catch (e: Exception) {
            null
        }
    }

    fun deleteEncryptedFile(file: File): Boolean {
        return if (file.exists()) file.delete() else true
    }
}
