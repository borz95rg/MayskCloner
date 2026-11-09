package com.maysk.cloner.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.maysk.cloner.BuildConfig
import com.maysk.cloner.data.ThemePreset
import com.maysk.cloner.ui.LocalActions
import com.maysk.cloner.ui.NavRow
import com.maysk.cloner.ui.SwitchRow
import com.maysk.design.AccentPalette
import com.maysk.design.ChoiceChipRow
import com.maysk.design.LocalMaysk
import com.maysk.design.TextInputDialog
import com.maysk.design.TopBar

@Composable
fun SettingsScreen() {
    val a = LocalActions.current
    val c = LocalMaysk.current
    val s by a.container.settings.settings.collectAsState()
    var repoDialog by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {
        TopBar("Настройки")
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
            Text("Тема", color = c.text, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.size(10.dp))
            ChoiceChipRow(
                ThemePreset.entries.map { it.title },
                ThemePreset.entries.indexOf(s.theme),
                { a.container.settings.update { st -> st.copy(theme = ThemePreset.entries[it]) } },
            )
            Spacer(Modifier.size(16.dp))
            Text("Акцент", color = c.text, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.size(10.dp))
            Row(Modifier.fillMaxWidth().horizontalScroll()) {
                AccentPalette.forEach { color ->
                    val sel = s.accent == color
                    Box(
                        Modifier.size(38.dp).padding(end = 10.dp).clip(CircleShape).background(Color(color))
                            .border(if (sel) 3.dp else 0.dp, Color.White, CircleShape)
                            .clickable { a.container.settings.update { it.copy(accent = color) } },
                    )
                }
            }
            Spacer(Modifier.size(18.dp))

            NavRow(Icons.Rounded.Security, "Безопасность") { a.nav.push(com.maysk.cloner.ui.Route.Security) }
            Spacer(Modifier.size(10.dp))
            SwitchRow(Icons.Rounded.SystemUpdate, "Автопроверка обновлений", "Из GitHub Releases, не чаще раза в 6 часов", checked = s.autoUpdateCheck) { v -> a.container.settings.update { it.copy(autoUpdateCheck = v) } }
            Spacer(Modifier.size(10.dp))
            NavRow(Icons.Rounded.SystemUpdate, "Репозиторий обновлений") { repoDialog = true }
            Spacer(Modifier.size(10.dp))
            NavRow(Icons.Rounded.Info, "О приложении") { a.nav.push(com.maysk.cloner.ui.Route.About) }
            Spacer(Modifier.size(24.dp))
        }
    }

    if (repoDialog) TextInputDialog(
        "Репозиторий обновлений",
        initial = s.customUpdateRepo.ifBlank { BuildConfig.UPDATE_REPO },
        label = "владелец/репозиторий",
        message = "Откуда приложение берёт обновления (GitHub Releases).",
        onDismiss = { repoDialog = false },
        onConfirm = { v ->
            val cleaned = v.trim().removePrefix("https://github.com/").trimEnd('/')
            a.container.settings.update { it.copy(customUpdateRepo = cleaned) }
            repoDialog = false
        },
    )
}

@Composable
private fun Modifier.horizontalScroll() = this.then(androidx.compose.foundation.horizontalScroll(androidx.compose.foundation.rememberScrollState()))
