package com.maysk.cloner.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

enum class ThemePreset(val title: String, val subtitle: String) {
    PURPLE("Maysk Purple", "Фиолетово-белая"),
    AURORA("Aurora", "Тёмная с сиянием"),
    AMOLED("AMOLED", "Чёрная"),
    LIGHT("Light", "Светлая"),
}

/** Общие настройки приложения. */
data class AppSettings(
    val theme: ThemePreset = ThemePreset.PURPLE,
    val accent: Long = 0xFF8B5CF6,
    val cardAlpha: Float = 0.1f,
    val corner: Int = 22,
    val fontScale: Float = 1f,
    val appLock: Boolean = false,
    val appLockPinSalt: String = "",
    val appLockPinHash: String = "",
    val hideClones: Boolean = false,
    val confirmDelete: Boolean = true,
    val protectBackups: Boolean = true,
    val autoUpdateCheck: Boolean = true,
    val customUpdateRepo: String = "",
)

/** Профиль клона: ссылка на установленное приложение + оформление ярлыка. */
data class CloneProfile(
    val cloneId: String,
    val packageName: String,
    val appLabel: String,
    val cloneName: String,
    val addToHome: Boolean = true,
)

/** Salted, deliberately expensive PIN verifier. The PIN itself is never persisted. */
private object PinCredential {
    private const val ITERATIONS = 150_000
    private const val KEY_BITS = 256
    private val random = SecureRandom()

    fun create(pin: String): Pair<String, String> {
        val salt = ByteArray(16).also(random::nextBytes)
        return encode(salt) to encode(derive(pin, salt))
    }

    fun verify(pin: String, saltText: String, hashText: String): Boolean = runCatching {
        val salt = decode(saltText)
        val expected = decode(hashText)
        MessageDigest.isEqual(expected, derive(pin, salt))
    }.getOrDefault(false)

    private fun derive(pin: String, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(pin.toCharArray(), salt, ITERATIONS, KEY_BITS)
        return try {
            SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
    }

    private fun encode(bytes: ByteArray) = Base64.getEncoder().encodeToString(bytes)
    private fun decode(value: String) = Base64.getDecoder().decode(value)
}

class SettingsStore(context: Context) {
    private val file = File(context.filesDir, "cloner_settings.json")
    private val clonesFile = File(context.filesDir, "cloner_profiles.json")

    private val _settings = MutableStateFlow(load())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()
    val current: AppSettings get() = _settings.value

    private val _clones = MutableStateFlow(loadClones())
    val clones: StateFlow<List<CloneProfile>> = _clones.asStateFlow()

    init {
        migrateLegacyPin()
    }

    fun update(transform: (AppSettings) -> AppSettings) {
        _settings.value = transform(_settings.value)
        persist()
    }

    fun setPin(pin: String) {
        val (salt, hash) = PinCredential.create(pin)
        update { it.copy(appLock = true, appLockPinSalt = salt, appLockPinHash = hash) }
    }

    fun clearPin() {
        update { it.copy(appLock = false, appLockPinSalt = "", appLockPinHash = "") }
    }

    fun verifyPin(pin: String): Boolean = PinCredential.verify(
        pin,
        _settings.value.appLockPinSalt,
        _settings.value.appLockPinHash,
    )

    fun addClone(c: CloneProfile) {
        _clones.value = _clones.value.filterNot { it.cloneId == c.cloneId } + c
        persistClones()
    }

    fun updateClone(id: String, transform: (CloneProfile) -> CloneProfile) {
        _clones.value = _clones.value.map { if (it.cloneId == id) transform(it) else it }
        persistClones()
    }

    fun removeClone(id: String) {
        _clones.value = _clones.value.filterNot { it.cloneId == id }
        persistClones()
    }

    fun clone(id: String): CloneProfile? = _clones.value.firstOrNull { it.cloneId == id }

    /** Полный экспорт настроек и профилей в JSON (для резервной копии). */
    fun exportJson(): String = JSONObject().apply {
        put("version", 3)
        put("settings", toJson(_settings.value).apply {
            put("appLock", false)
            remove("appLockPinSalt")
            remove("appLockPinHash")
        })
        put("clones", JSONArray().also { arr -> _clones.value.forEach { arr.put(cloneToJson(it)) } })
    }.toString()

    /** Импорт из JSON. Возвращает число восстановленных профилей, или -1 при ошибке. */
    fun importJson(text: String): Int = runCatching {
        require(text.length <= 5 * 1024 * 1024) { "backup too large" }
        val o = JSONObject(text)
        val version = o.optInt("version", -1)
        require(version in 1..3) { "unsupported backup version" }
        val importedSettings = o.optJSONObject("settings")?.let(::fromJson)
        val arr = o.optJSONArray("clones") ?: JSONArray()
        require(arr.length() <= 1000) { "too many profiles" }
        val list = (0 until arr.length()).map { cloneFromJson(arr.getJSONObject(it)) }
        require(list.all { it.packageName.matches(Regex("[A-Za-z0-9_]+(\\.[A-Za-z0-9_]+)+")) && it.cloneId.isNotBlank() }) { "invalid profile" }
        importedSettings?.let {
            _settings.value = it.copy(
                appLock = _settings.value.appLock,
                appLockPinSalt = _settings.value.appLockPinSalt,
                appLockPinHash = _settings.value.appLockPinHash,
            )
            persist()
        }
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

    private fun migrateLegacyPin() {
        runCatching {
            if (!file.exists()) return
            val json = JSONObject(file.readText())
            if (!json.has("appLockPin")) return
            val pin = json.optString("appLockPin")
            if (pin.isNotEmpty()) {
                val (salt, hash) = PinCredential.create(pin)
                _settings.value = _settings.value.copy(
                    appLock = json.optBoolean("appLock", false),
                    appLockPinSalt = salt,
                    appLockPinHash = hash,
                )
            }
            persist()
        }
    }

    private fun loadClones(): List<CloneProfile> = runCatching {
        if (!clonesFile.exists()) return emptyList()
        val arr = JSONArray(clonesFile.readText())
        (0 until arr.length()).map { cloneFromJson(arr.getJSONObject(it)) }
    }.getOrDefault(emptyList())

    private fun toJson(s: AppSettings) = JSONObject().apply {
        put("theme", s.theme.name); put("accent", s.accent); put("cardAlpha", s.cardAlpha.toDouble())
        put("corner", s.corner); put("fontScale", s.fontScale.toDouble())
        put("appLock", s.appLock); put("appLockPinSalt", s.appLockPinSalt); put("appLockPinHash", s.appLockPinHash)
        put("hideClones", s.hideClones)
        put("confirmDelete", s.confirmDelete); put("protectBackups", s.protectBackups)
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
            appLock = o.optBoolean("appLock", d.appLock) && (
                (o.optString("appLockPinSalt").isNotBlank() && o.optString("appLockPinHash").isNotBlank()) ||
                    o.optString("appLockPin").isNotBlank()
                ),
            appLockPinSalt = o.optString("appLockPinSalt", ""),
            appLockPinHash = o.optString("appLockPinHash", ""),
            hideClones = o.optBoolean("hideClones", d.hideClones),
            confirmDelete = o.optBoolean("confirmDelete", d.confirmDelete),
            protectBackups = o.optBoolean("protectBackups", d.protectBackups),
            autoUpdateCheck = o.optBoolean("autoUpdateCheck", d.autoUpdateCheck),
            customUpdateRepo = o.optString("customUpdateRepo", d.customUpdateRepo),
        )
    }

    private fun cloneToJson(c: CloneProfile) = JSONObject().apply {
        put("cloneId", c.cloneId); put("packageName", c.packageName); put("appLabel", c.appLabel); put("cloneName", c.cloneName)
        put("addToHome", c.addToHome)
    }

    private fun cloneFromJson(o: JSONObject): CloneProfile {
        val d = CloneProfile("", "", "", "")
        return CloneProfile(
            cloneId = o.optString("cloneId"),
            packageName = o.optString("packageName"),
            appLabel = o.optString("appLabel"),
            cloneName = o.optString("cloneName"),
            addToHome = o.optBoolean("addToHome", d.addToHome),
        )
    }

    private inline fun <reified T : Enum<T>> enumOr(name: String?, default: T): T =
        runCatching { enumValueOf<T>(name ?: "") }.getOrDefault(default)
}
