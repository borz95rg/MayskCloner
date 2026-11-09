package com.maysk.cloner.data

import android.content.Context
import android.net.Uri
import android.util.Base64
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.security.SecureRandom
import javax.crypto.AEADBadTagException
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/** Export/import results distinguish a missing password from a damaged backup. */
sealed interface BackupImportResult {
    data object PasswordRequired : BackupImportResult
    data object WrongPasswordOrDamaged : BackupImportResult
    data object InvalidFile : BackupImportResult
    data class Imported(val profiles: Int) : BackupImportResult
}

/** Export and import of local settings and profile metadata. */
object Backup {
    private const val FORMAT = "maysk-cloner-encrypted-backup"
    private const val VERSION = 1
    private const val ITERATIONS = 200_000
    private const val MAX_BACKUP_BYTES = 5 * 1024 * 1024
    private val random = SecureRandom()

    fun export(context: Context, store: SettingsStore, uri: Uri, password: String? = null): Boolean = runCatching {
        val plain = store.exportJson().toByteArray(Charsets.UTF_8)
        val payload = if (password == null) plain else encrypt(plain, password)
        val output = context.contentResolver.openOutputStream(uri) ?: return false
        output.use { stream ->
            stream.write(payload)
            stream.flush()
        }
        true
    }.getOrDefault(false)

    fun import(
        context: Context,
        store: SettingsStore,
        uri: Uri,
        password: String? = null,
    ): BackupImportResult = runCatching {
        val input = context.contentResolver.openInputStream(uri) ?: return BackupImportResult.InvalidFile
        val bytes = input.use { stream ->
            val buffer = ByteArray(16 * 1024)
            val output = ByteArrayOutputStream()
            while (true) {
                val count = stream.read(buffer)
                if (count < 0) break
                if (output.size() + count > MAX_BACKUP_BYTES) return BackupImportResult.InvalidFile
                output.write(buffer, 0, count)
            }
            output.toByteArray()
        }
        var text = bytes.toString(Charsets.UTF_8)
        val envelope = runCatching { JSONObject(text) }.getOrNull()
        if (envelope?.optString("format") == FORMAT) {
            if (password == null) return BackupImportResult.PasswordRequired
            text = decrypt(envelope, password)?.toString(Charsets.UTF_8)
                ?: return BackupImportResult.WrongPasswordOrDamaged
        }
        val count = store.importJson(text)
        if (count < 0) BackupImportResult.InvalidFile else BackupImportResult.Imported(count)
    }.getOrDefault(BackupImportResult.InvalidFile)

    private fun encrypt(plain: ByteArray, password: String): ByteArray {
        require(password.length >= 8) { "password too short" }
        val salt = ByteArray(16).also(random::nextBytes)
        val nonce = ByteArray(12).also(random::nextBytes)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, deriveKey(password, salt), GCMParameterSpec(128, nonce))
        val encrypted = cipher.doFinal(plain)
        return JSONObject()
            .put("format", FORMAT)
            .put("version", VERSION)
            .put("kdf", "PBKDF2WithHmacSHA256")
            .put("iterations", ITERATIONS)
            .put("salt", encode(salt))
            .put("nonce", encode(nonce))
            .put("data", encode(encrypted))
            .toString()
            .toByteArray(Charsets.UTF_8)
    }

    private fun decrypt(envelope: JSONObject, password: String): ByteArray? = runCatching {
        require(envelope.optInt("version", -1) == VERSION)
        require(envelope.optString("kdf") == "PBKDF2WithHmacSHA256")
        val iterations = envelope.optInt("iterations", -1)
        require(iterations in 100_000..300_000)
        val salt = decode(envelope.getString("salt"))
        val nonce = decode(envelope.getString("nonce"))
        val encrypted = decode(envelope.getString("data"))
        require(salt.size == 16 && nonce.size == 12 && encrypted.size <= MAX_BACKUP_BYTES)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, deriveKey(password, salt, iterations), GCMParameterSpec(128, nonce))
        cipher.doFinal(encrypted)
    }.getOrNull()

    private fun deriveKey(password: String, salt: ByteArray, iterations: Int = ITERATIONS): SecretKeySpec {
        val spec = PBEKeySpec(password.toCharArray(), salt, iterations, 256)
        return try {
            SecretKeySpec(SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded, "AES")
        } finally {
            spec.clearPassword()
        }
    }

    private fun encode(bytes: ByteArray): String = Base64.encodeToString(bytes, Base64.NO_WRAP)
    private fun decode(value: String): ByteArray = Base64.decode(value, Base64.DEFAULT)
}
