package com.maysk.cloner

import android.app.Application
import android.content.Context
import com.maysk.cloner.data.SettingsStore
import com.maysk.cloner.engine.Firewall
import com.maysk.cloner.update.UpdateManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/** Контейнер зависимостей приложения. */
class AppContainer(context: Context) {
    val appContext: Context = context.applicationContext
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    val settings = SettingsStore(context)
    val firewall = Firewall(context, settings)
    val updates = UpdateManager(context, settings, scope)

    init {
        seedKnownProfiles()
        // Восстанавливаем правила файрвола из сохранённых профилей.
        firewall.rebuildRules()
    }

    /**
     * Профиль совместимости для «Наследие Древних» (com.fenix.newlegacy) из ТЗ: режим
     * «Без автоматического Google-запроса». Создаётся один раз и только если игра установлена.
     */
    private fun seedKnownProfiles() {
        val pkg = "com.fenix.newlegacy"
        if (settings.clones.value.any { it.packageName == pkg }) return
        if (!com.maysk.cloner.data.Apps.isInstalled(appContext, pkg)) return
        val label = runCatching {
            appContext.packageManager.getApplicationLabel(appContext.packageManager.getApplicationInfo(pkg, 0)).toString()
        }.getOrDefault("Наследие Древних")
        settings.addClone(
            com.maysk.cloner.data.CloneGoogleSettings(
                cloneId = "clone_newlegacy",
                packageName = pkg,
                appLabel = label,
                cloneName = "$label Клон 1",
                googleMode = com.maysk.cloner.engine.GoogleMode.NO_AUTO_GOOGLE,
                preserveLogin = true,
                preserveGameData = true,
            ),
        )
    }
}

class ClonerApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
