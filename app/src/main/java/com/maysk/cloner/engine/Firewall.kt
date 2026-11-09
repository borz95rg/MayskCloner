package com.maysk.cloner.engine

import android.content.Context
import android.content.Intent
import android.net.VpnService
import com.maysk.cloner.data.CloneGoogleSettings
import com.maysk.cloner.data.SettingsStore
import kotlinx.coroutines.flow.StateFlow

/**
 * Управление локальным файрволом: собирает правила из настроек профилей и запускает/останавливает
 * службу. Правила пересобираются при любом изменении настроек — служба перечитывает их на лету.
 */
class Firewall(private val context: Context, private val store: SettingsStore) {

    val running: StateFlow<Boolean> get() = FirewallState.running

    /** Нужно ли показать системный диалог разрешения VPN (один раз). */
    fun prepareIntent(): Intent? = VpnService.prepare(context)

    /** Пересобрать правила блокировки из текущих настроек всех профилей. */
    fun rebuildRules() {
        val pm = context.packageManager
        val rules = store.clones.value.mapNotNull { clone ->
            val domains = blockedDomainsFor(clone)
            if (clone.googleMode != GoogleMode.NO_AUTO_GOOGLE || domains.isEmpty()) return@mapNotNull null
            val uid = try { pm.getApplicationInfo(clone.packageName, 0).uid } catch (e: Exception) { return@mapNotNull null }
            AppRule(clone.packageName, uid, domains)
        }
        FirewallState.update(rules)
    }

    /** Есть ли хотя бы один профиль, требующий файрвола. */
    fun hasActiveRules(): Boolean = FirewallState.routedPackages.isNotEmpty()

    fun start() {
        rebuildRules()
        if (!hasActiveRules()) { stop(); return }
        val intent = Intent(context, FirewallVpnService::class.java).setAction(FirewallVpnService.ACTION_START)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) context.startForegroundService(intent)
        else context.startService(intent)
    }

    fun stop() {
        val intent = Intent(context, FirewallVpnService::class.java).setAction(FirewallVpnService.ACTION_STOP)
        runCatching { context.startService(intent) }
    }

    /** Применить изменения: если остались активные правила — (пере)запустить, иначе остановить. */
    fun apply() {
        rebuildRules()
        if (hasActiveRules()) start() else stop()
    }

    companion object {
        /** Какие домены блокировать для профиля, исходя из его переключателей. */
        fun blockedDomainsFor(c: CloneGoogleSettings): Set<String> {
            if (c.googleMode != GoogleMode.NO_AUTO_GOOGLE) return emptySet()
            val set = LinkedHashSet<String>()
            // Базовый набор режима «Без автоматического Google-запроса».
            set += BlockLists.CONNECTIVITY
            if (!c.autoGoogleSignIn) set += BlockLists.SIGN_IN
            if (!c.playGames) set += BlockLists.PLAY_GAMES
            if (!c.microG) set += BlockLists.MICROG
            if (!c.googlePlayRedirect) set += BlockLists.PLAY_STORE
            if (!c.externalBrowser) set += BlockLists.BROWSER_REDIRECT
            return set
        }
    }
}
