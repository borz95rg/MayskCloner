package com.maysk.cloner.update

import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import com.maysk.cloner.BuildConfig
import com.maysk.cloner.data.SettingsStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.security.MessageDigest
import java.net.HttpURLConnection
import java.net.URL

data class UpdateInfo(val versionName: String, val build: Int, val notes: String, val apkUrl: String, val sizeBytes: Long)

sealed interface UpdateStatus {
    data object Idle : UpdateStatus
    data object Checking : UpdateStatus
    data object UpToDate : UpdateStatus
    data class Available(val info: UpdateInfo) : UpdateStatus
    data class Downloading(val info: UpdateInfo, val progress: Float) : UpdateStatus
    data class ReadyToInstall(val info: UpdateInfo, val file: File) : UpdateStatus
    data class Error(val message: String) : UpdateStatus
}

/**
 * Обновления через GitHub Releases: GitHub Actions публикует APK с тегом v1.0.N,
 * приложение сравнивает N со своим versionCode, скачивает APK и открывает установщик.
 */
class UpdateManager(
    private val context: Context,
    private val settings: SettingsStore,
    private val scope: CoroutineScope,
) {
    private val _status = MutableStateFlow<UpdateStatus>(UpdateStatus.Idle)
    val status: StateFlow<UpdateStatus> = _status.asStateFlow()

    private val prefs = context.getSharedPreferences("maysk_update", Context.MODE_PRIVATE)

    val repo: String
        get() = settings.current.customUpdateRepo.trim().ifEmpty { BuildConfig.UPDATE_REPO }

    val currentBuild: Int get() = BuildConfig.VERSION_CODE
    val currentVersion: String get() = BuildConfig.VERSION_NAME

    /** Автопроверка не чаще раза в 6 часов. */
    fun autoCheck() {
        if (!settings.current.autoUpdateCheck || repo.isBlank()) return
        val last = prefs.getLong("last_check", 0)
        if (System.currentTimeMillis() - last < 6 * 3600_000L) {
            val cached = if (prefs.getString("latest_repo", null) == repo) {
                runCatching { prefs.getString("latest_release", null)?.let { JSONObject(it) } }.getOrNull()
            } else null
            val info = cached?.let { json -> runCatching {
                UpdateInfo(json.getString("versionName"), json.getInt("build"), json.optString("notes"), json.getString("apkUrl"), json.optLong("sizeBytes"))
            }.getOrNull() }
            if (info != null && info.build > currentBuild && !wasDismissedRecently(info.build)) {
                _status.value = UpdateStatus.Available(info)
            }
            return
        }
        check(silent = true)
    }

    fun check(silent: Boolean = false) {
        if (repo.isBlank()) {
            _status.value = UpdateStatus.Error("Не указан репозиторий GitHub для обновлений")
            return
        }
        if (_status.value is UpdateStatus.Downloading) return
        if (!silent) _status.value = UpdateStatus.Checking
        scope.launch {
            val result = withContext(Dispatchers.IO) { runCatching { fetchLatest() } }
            prefs.edit().putLong("last_check", System.currentTimeMillis()).apply()
            result.onSuccess { info ->
                if (info != null) prefs.edit()
                    .putString("latest_repo", repo)
                    .putString("latest_release", JSONObject().apply {
                        put("versionName", info.versionName); put("build", info.build); put("notes", info.notes)
                        put("apkUrl", info.apkUrl); put("sizeBytes", info.sizeBytes)
                    }.toString()).apply()
                else prefs.edit().remove("latest_repo").remove("latest_release").apply()
                _status.value = when {
                    info != null && info.build > currentBuild && !wasDismissedRecently(info.build) -> UpdateStatus.Available(info)
                    silent -> UpdateStatus.Idle
                    else -> UpdateStatus.UpToDate
                }
            }.onFailure {
                _status.value = if (silent) UpdateStatus.Idle else UpdateStatus.Error("Не удалось проверить обновления: ${it.message ?: "нет сети"}")
            }
        }
    }

    fun dismiss() {
        val current = _status.value
        if (current is UpdateStatus.Available) prefs.edit().putLong("dismissed_at_${current.info.build}", System.currentTimeMillis()).apply()
        _status.value = UpdateStatus.Idle
    }

    private fun wasDismissedRecently(build: Int): Boolean {
        val dismissedAt = prefs.getLong("dismissed_at_$build", 0L)
        return dismissedAt > 0L && System.currentTimeMillis() - dismissedAt < 24 * 3600_000L
    }

    private fun fetchLatest(): UpdateInfo? {
        val conn = URL("https://api.github.com/repos/$repo/releases/latest").openConnection() as HttpURLConnection
        conn.setRequestProperty("Accept", "application/vnd.github+json")
        conn.setRequestProperty("User-Agent", "MayskCloner/${currentVersion}")
        conn.connectTimeout = 15_000
        conn.readTimeout = 15_000
        try {
            if (conn.responseCode == 404) throw IllegalStateException("релизы не найдены (репозиторий приватный или ещё нет сборок)")
            if (conn.responseCode !in 200..299) throw IllegalStateException("GitHub ответил ${conn.responseCode}")
            val json = JSONObject(conn.inputStream.bufferedReader().use { it.readText() })
            val tag = json.optString("tag_name")
            val build = tag.substringAfterLast('.').filter { it.isDigit() }.toIntOrNull() ?: return null
            val assets = json.optJSONArray("assets") ?: return null
            for (i in 0 until assets.length()) {
                val a = assets.getJSONObject(i)
                val name = a.optString("name")
                if (name.endsWith(".apk")) {
                    return UpdateInfo(
                        versionName = tag.removePrefix("v"),
                        build = build,
                        notes = json.optString("body").take(1500),
                        apkUrl = a.getString("browser_download_url"),
                        sizeBytes = a.optLong("size"),
                    )
                }
            }
            return null
        } finally {
            conn.disconnect()
        }
    }

    fun download(info: UpdateInfo) {
        if (_status.value is UpdateStatus.Downloading) return
        _status.value = UpdateStatus.Downloading(info, 0f)
        scope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    val dir = File(context.cacheDir, "updates").apply { mkdirs() }
                    dir.listFiles()?.forEach { it.delete() }
                    val out = File(dir, "MayskCloner-${info.versionName}.apk")
                    fun open(u: URL): HttpURLConnection = (u.openConnection() as HttpURLConnection).apply {
                        instanceFollowRedirects = false
                        setRequestProperty("User-Agent", "MayskCloner/${currentVersion}")
                        connectTimeout = 20_000
                        readTimeout = 30_000
                    }
                    var url = URL(info.apkUrl)
                    var conn = open(url)
                    var redirects = 0
                    while (conn.responseCode in 300..399 && redirects < 5) {
                        val loc = conn.getHeaderField("Location") ?: break
                        conn.disconnect()
                        url = URL(url, loc)
                        conn = open(url)
                        redirects++
                    }
                    if (conn.responseCode !in 200..299) throw IllegalStateException("код ${conn.responseCode}")
                    val total = conn.contentLengthLong.takeIf { it > 0 } ?: info.sizeBytes
                    conn.inputStream.use { input ->
                        out.outputStream().use { output ->
                            val buf = ByteArray(64 * 1024)
                            var done = 0L
                            var lastEmit = 0L
                            while (true) {
                                val n = input.read(buf)
                                if (n < 0) break
                                output.write(buf, 0, n)
                                done += n
                                if (total > 0 && done - lastEmit > 256 * 1024) {
                                    lastEmit = done
                                    val p = (done.toFloat() / total).coerceIn(0f, 1f)
                                    withContext(Dispatchers.Main) { _status.value = UpdateStatus.Downloading(info, p) }
                                }
                            }
                        }
                    }
                    conn.disconnect()
                    if (out.length() == 0L || (info.sizeBytes > 0L && out.length() != info.sizeBytes)) {
                        out.delete()
                        throw IllegalStateException("Загружен неполный APK")
                    }
                    validateApk(out, info.build)
                    out
                }
            }
            result.onSuccess { file ->
                _status.value = UpdateStatus.ReadyToInstall(info, file)
            }.onFailure {
                _status.value = UpdateStatus.Error("Ошибка загрузки: ${it.message ?: "нет сети"}")
            }
        }
    }

    private fun validateApk(file: File, expectedBuild: Int) {
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            PackageManager.GET_SIGNING_CERTIFICATES
        } else {
            @Suppress("DEPRECATION") PackageManager.GET_SIGNATURES
        }
        val downloaded = context.packageManager.getPackageArchiveInfo(file.absolutePath, flags)
            ?: throw IllegalStateException("Файл не распознан как APK")
        if (downloaded.packageName != context.packageName) {
            throw IllegalStateException("APK предназначен для другого приложения")
        }
        val downloadedBuild = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) downloaded.longVersionCode else {
            @Suppress("DEPRECATION") downloaded.versionCode.toLong()
        }
        if (downloadedBuild != expectedBuild.toLong()) throw IllegalStateException("Номер версии APK не совпадает с релизом")

        val installed = context.packageManager.getPackageInfo(context.packageName, flags)
        if (signerDigests(downloaded).isEmpty() || signerDigests(downloaded) != signerDigests(installed)) {
            throw IllegalStateException("Подпись APK не совпадает с установленным приложением")
        }
    }

    private fun signerDigests(info: PackageInfo): Set<String> {
        val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) info.signingInfo?.apkContentsSigners else {
            @Suppress("DEPRECATION") info.signatures
        } ?: return emptySet()
        return signatures.map { signature ->
            MessageDigest.getInstance("SHA-256").digest(signature.toByteArray()).joinToString("") { byte -> "%02x".format(byte) }
        }.toSet()
    }

    fun canInstallPackages(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.O || context.packageManager.canRequestPackageInstalls()

    fun openInstallPermissionSettings() {
        val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}"))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(intent) }
    }

    fun install(file: File) {
        if (!canInstallPackages()) {
            openInstallPermissionSettings()
            return
        }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", file)
        val intent = Intent(Intent.ACTION_VIEW)
            .setDataAndType(uri, "application/vnd.android.package-archive")
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(intent) }
            .onFailure { _status.value = UpdateStatus.Error("Не удалось открыть установщик") }
    }
}
