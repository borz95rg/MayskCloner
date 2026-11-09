package com.maysk.cloner.data

import android.content.Context
import com.maysk.cloner.engine.GoogleMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

enum class ThemePreset(val title: String, val subtitle: String) {
    PURPLE("Maysk Purple", "Фиолетово-белая"),
    AURORA("Aurora", "Тёмная с сиянием"),
    AMOLED("AMOLED", "Чёрная"),
    LIGHT("Light", "Светлая"),
}

enum class LabelStyle(val title: String) { DEFAULT("По умолчанию"), CLASSIC("Классический"), MODERN3D("3D стиль"), NEON("Неоновый") }

/** Общие настройки приложения. */
data class AppSettings(
    val theme: ThemePreset = ThemePreset.PURPLE,
    val accent: Long = 0xFF8B5CF6,
    val cardAlpha: Float = 0.1f,
    val corner: Int = 22,
    val fontScale: Float = 1f,
    // Безопасность
    val appLock: Boolean = false,
    val appLockPin: String = "",
    val hideClones: Boolean = false,
    val confirmDelete: Boolean = true,
    val protectBackups: Boolean = true,
    val separateData: Boolean = true,
    // Обновления
    val autoUpdateCheck: Boolean = true,
    val customUpdateRepo: String = "",
)

/**
 * Настройки Google и авторизации для одного профиля (клона). Структура из ТЗ, раздел 6.
 * Хранится отдельно от данных исходного приложения.
 */
data class CloneGoogleSettings(
    val cloneId: String,
    val packageName: String,
    val appLabel: String,
    val cloneName: String,
    val googleMode: GoogleMode = GoogleMode.STANDARD,
    val autoGoogleSignIn: Boolean = true,
    val microG: Boolean = true,
    val playGames: Boolean = true,
    val externalBrowser: Boolean = true,
    val googlePlayRedirect: Boolean = true,
    val preserveGameData: Boolean = true,
    val preserveLogin: Boolean = true,
    // Оформление
    val labelStyle: LabelStyle = LabelStyle.DEFAULT,
    val addToHome: Boolean = true,
    val iconUri: String = "",
)

class SettingsStore(context: Context) {
    private val file = File(context.filesDir, "cloner_settings.json")
    private val clonesFile = File(context.filesDir, "cloner_profiles.json")

    private val _settings = MutableStateFlow(load())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()
    val current: AppSettings get() = _settings.value

    private val _clones = MutableStateFlow(loadClones())
    val clones: StateFlow<List<CloneGoogleSettings>> = _clones.asStateFlow()

    fun update(transform: (AppSettings) -> AppSettings) {
        _settings.value = transform(_settings.value)
        persist()
    }

    fun addClone(c: CloneGoogleSettings) {
        _clones.value = _clones.value.filterNot { it.cloneId == c.cloneId } + c
        persistClones()
    }

    fun updateClone(id: String, transform: (CloneGoogleSettings) -> CloneGoogleSettings) {
        _clones.value = _clones.value.map { if (it.cloneId == id) transform(it) else it }
        persistClones()
    }

    fun removeClone(id: String) {
        _clones.value = _clones.value.filterNot { it.cloneId == id }
        persistClones()
    }

    fun clone(id: String): CloneGoogleSettings? = _clones.value.firstOrNull { it.cloneId == id }

    /** Полный экспорт настроек и профилей в JSON (для резервной копии). */
    fun exportJson(): String = JSONObject().apply {
        put("version", 1)
        put("settings", toJson(_settings.value))
        put("clones", JSONArray().also { arr -> _clones.value.forEach { arr.put(cloneToJson(it)) } })
    }.toString()

    /** Импорт из JSON. Возвращает число восстановленных профилей, или -1 при ошибке. */
    fun importJson(text: String): Int = runCatching {
        val o = JSONObject(text)
        o.optJSONObject("settings")?.let { _settings.value = fromJson(it); persist() }
        val arr = o.optJSONArray("clones") ?: JSONArray()
        val list = (0 until arr.length()).map { cloneFromJson(arr.getJSONObject(it)) }
        _clones.value = list
        persistClones()
        list.size
    }.getOrDefault(-1)

    // --------------------------------------------------------------- JSON

    private fun persist() = runCatching { file.writeText(toJson(_settings.value).toString()) }
    private fun persistClones() = runCatching {
        val arr = JSONArray()
        _clones.value.forEach { arr.put(cloneToJson(it)) }
        clonesFile.writeText(arr.toString())
    }

    private fun load(): AppSettings = runCatching {
        if (!file.exists()) return AppSettings()
        fromJson(JSONObject(file.readText()))
    }.getOrDefault(AppSettings())

    private fun loadClones(): List<CloneGoogleSettings> = runCatching {
        if (!clonesFile.exists()) return emptyList()
        val arr = JSONArray(clonesFile.readText())
        (0 until arr.length()).map { cloneFromJson(arr.getJSONObject(it)) }
    }.getOrDefault(emptyList())

    private fun toJson(s: AppSettings) = JSONObject().apply {
        put("theme", s.theme.name); put("accent", s.accent); put("cardAlpha", s.cardAlpha.toDouble())
        put("corner", s.corner); put("fontScale", s.fontScale.toDouble())
        put("appLock", s.appLock); put("appLockPin", s.appLockPin); put("hideClones", s.hideClones)
        put("confirmDelete", s.confirmDelete); put("protectBackups", s.protectBackups); put("separateData", s.separateData)
        put("autoUpdateCheck", s.autoUpdateCheck); put("customUpdateRepo", s.customUpdateRepo)
    }

    private fun fromJson(o: JSONObject): AppSettings {
        val d = AppSettings()
        return AppSettings(
            theme = enumOr(o.optString("theme"), d.theme),
            accent = o.optLong("accent", d.accent),
            cardAlpha = o.optDouble("cardAlpha", d.cardAlpha.toDouble()).toFloat(),
            corner = o.optInt("corner", d.corner),
            fontScale = o.optDouble("fontScale", d.fontScale.toDouble()).toFloat(),
            appLock = o.optBoolean("appLock", d.appLock),
            appLockPin = o.optString("appLockPin", d.appLockPin),
            hideClones = o.optBoolean("hideClones", d.hideClones),
            confirmDelete = o.optBoolean("confirmDelete", d.confirmDelete),
            protectBackups = o.optBoolean("protectBackups", d.protectBackups),
            separateData = o.optBoolean("separateData", d.separateData),
            autoUpdateCheck = o.optBoolean("autoUpdateCheck", d.autoUpdateCheck),
            customUpdateRepo = o.optString("customUpdateRepo", d.customUpdateRepo),
        )
    }

    private fun cloneToJson(c: CloneGoogleSettings) = JSONObject().apply {
        put("cloneId", c.cloneId); put("packageName", c.packageName); put("appLabel", c.appLabel); put("cloneName", c.cloneName)
        put("googleMode", c.googleMode.name); put("autoGoogleSignIn", c.autoGoogleSignIn); put("microG", c.microG)
        put("playGames", c.playGames); put("externalBrowser", c.externalBrowser); put("googlePlayRedirect", c.googlePlayRedirect)
        put("preserveGameData", c.preserveGameData); put("preserveLogin", c.preserveLogin)
        put("labelStyle", c.labelStyle.name); put("addToHome", c.addToHome); put("iconUri", c.iconUri)
    }

    private fun cloneFromJson(o: JSONObject): CloneGoogleSettings {
        val d = CloneGoogleSettings("", "", "", "")
        return CloneGoogleSettings(
            cloneId = o.optString("cloneId"),
            packageName = o.optString("packageName"),
            appLabel = o.optString("appLabel"),
            cloneName = o.optString("cloneName"),
            googleMode = enumOr(o.optString("googleMode"), d.googleMode),
            autoGoogleSignIn = o.optBoolean("autoGoogleSignIn", d.autoGoogleSignIn),
            microG = o.optBoolean("microG", d.microG),
            playGames = o.optBoolean("playGames", d.playGames),
            externalBrowser = o.optBoolean("externalBrowser", d.externalBrowser),
            googlePlayRedirect = o.optBoolean("googlePlayRedirect", d.googlePlayRedirect),
            preserveGameData = o.optBoolean("preserveGameData", d.preserveGameData),
            preserveLogin = o.optBoolean("preserveLogin", d.preserveLogin),
            labelStyle = enumOr(o.optString("labelStyle"), d.labelStyle),
            addToHome = o.optBoolean("addToHome", d.addToHome),
            iconUri = o.optString("iconUri", d.iconUri),
        )
    }

    private inline fun <reified T : Enum<T>> enumOr(name: String?, default: T): T =
        runCatching { enumValueOf<T>(name ?: "") }.getOrDefault(default)
}
