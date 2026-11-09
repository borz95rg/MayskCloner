package com.maysk.cloner.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import com.maysk.cloner.AppContainer

sealed interface Route {
    data object Dashboard : Route
    data object Clones : Route
    data object Backup : Route
    data object Settings : Route
    data object PickApp : Route
    data object Security : Route
    data object About : Route
    data class CloneCard(val id: String) : Route
    data class Appearance(val id: String) : Route
}

class Nav {
    var tab by mutableStateOf(0)
        private set
    val stack = mutableStateListOf<Route>()

    fun selectTab(i: Int) {
        tab = i
        stack.clear()
    }

    fun push(r: Route) { stack.add(r) }
    fun pop() { if (stack.isNotEmpty()) stack.removeAt(stack.lastIndex) }
    val current: Route
        get() = stack.lastOrNull() ?: when (tab) {
            0 -> Route.Dashboard
            1 -> Route.Clones
            2 -> Route.Backup
            else -> Route.Settings
        }
}

class UiActions(
    val container: AppContainer,
    val nav: Nav,
    val toast: (String) -> Unit,
)

val LocalActions = staticCompositionLocalOf<UiActions> { error("UiActions not provided") }
