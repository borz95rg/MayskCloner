package com.maysk.cloner.engine

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Что именно блокировать для одного приложения. */
data class AppRule(
    val packageName: String,
    val uid: Int,
    /** Суффиксы доменов, которые нужно блокировать для этого приложения. */
    val blockedDomains: Set<String>,
)

/**
 * Общее состояние файрвола между UI и VpnService. VpnService читает [snapshot] на каждый
 * DNS-запрос, поэтому изменения настроек применяются без перезапуска службы.
 */
object FirewallState {
    @Volatile
    var snapshot: Map<Int, AppRule> = emptyMap()
        private set

    /** Объединённый список доменов (для Android < 10, где нельзя определить приложение по пакету). */
    @Volatile
    var unionDomains: Set<String> = emptySet()
        private set

    private val _running = MutableStateFlow(false)
    val running: StateFlow<Boolean> = _running.asStateFlow()

    /** Пакеты, которые должны маршрутизироваться через файрвол (режим «Без Google-запроса»). */
    @Volatile
    var routedPackages: List<String> = emptyList()
        private set

    fun update(rules: List<AppRule>) {
        snapshot = rules.associateBy { it.uid }
        unionDomains = rules.flatMap { it.blockedDomains }.toSet()
        routedPackages = rules.map { it.packageName }
    }

    fun setRunning(v: Boolean) { _running.value = v }

    /** Нужно ли блокировать домен для данного uid (или по объединённому списку). */
    fun isBlocked(uid: Int, host: String): Boolean {
        val h = host.lowercase().trimEnd('.')
        if (BlockLists.NEVER_BLOCK.any { h == it || h.endsWith(".$it") }) return false
        val set = snapshot[uid]?.blockedDomains ?: if (uid < 0) unionDomains else return false
        return set.any { domainMatches(h, it) }
    }

    private fun domainMatches(host: String, pattern: String): Boolean {
        val p = pattern.substringBefore('/').lowercase()
        return host == p || host.endsWith(".$p")
    }
}
