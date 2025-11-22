package com.xavierclavel.services

import at.favre.lib.crypto.bcrypt.BCrypt
import com.xavierclavel.config.Configuration
import io.ktor.utils.io.core.toByteArray
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

class EncryptionService: KoinComponent {
    val configuration: Configuration by inject()
    val aesKey = SecretKeySpec(configuration.aes.key.toByteArray(), "AES")

    fun encryptPassword(password: String?): String? =
        password?.let { BCrypt.withDefaults().hashToString(12, password.toCharArray()) }


    fun isPasswordCorrect(expected: String, actual: String): Boolean =
        BCrypt.verifyer().verify(actual.toCharArray(), expected).verified


    fun encryptMail(email: String): String {
        val ivBytes = ByteArray(16)
        SecureRandom().nextBytes(ivBytes)
        val iv = IvParameterSpec(ivBytes)

        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        cipher.init(Cipher.ENCRYPT_MODE, aesKey, iv)
        val encrypted = cipher.doFinal(email.toByteArray())

        return Base64.getEncoder().encodeToString(ivBytes + encrypted)
    }


    fun decryptMail(encryptedEmail: String): String {
        val decoded = Base64.getDecoder().decode(encryptedEmail)
        val ivBytes = decoded.copyOfRange(0, 16)
        val encryptedBytes = decoded.copyOfRange(16, decoded.size)

        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        cipher.init(Cipher.DECRYPT_MODE, aesKey, IvParameterSpec(ivBytes))
        return String(cipher.doFinal(encryptedBytes))
    }

    fun hash(value: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(value.trim().lowercase().toByteArray(Charsets.UTF_8))
        return Base64.getEncoder().encodeToString(hashBytes)
    }
}