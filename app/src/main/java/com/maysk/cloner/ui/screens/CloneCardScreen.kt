package com.maysk.cloner.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.AddToHomeScreen
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.maysk.cloner.data.Apps
import com.maysk.cloner.data.CloneManager
import com.maysk.cloner.ui.AppIcon
import com.maysk.cloner.ui.LocalActions
import com.maysk.cloner.ui.NavRow
import com.maysk.cloner.ui.Route
import com.maysk.design.ConfirmDialog
import com.maysk.design.GlassCard
import com.maysk.design.LocalMaysk
import com.maysk.design.TopBar

@Composable
fun CloneCardScreen(cloneId: String) {
    val a = LocalActions.current
    val c = LocalMaysk.current
    val context = LocalContext.current
    val clones by a.container.settings.clones.collectAsState()
    val clone = clones.firstOrNull { it.cloneId == cloneId } ?: run { a.nav.pop(); return }
    var confirmDelete by remember { mutableStateOf(false) }
    val installed = Apps.isInstalled(context, clone.packageName)

    Column(Modifier.fillMaxSize()) {
        TopBar(clone.cloneName.ifBlank { clone.appLabel }, onBack = { a.nav.pop() })
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {

            GlassCard(Modifier.fillMaxWidth()) {
                Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                    AppIcon(clone.packageName, 60.dp, clone.appLabel)
                    Spacer(Modifier.width(16.dp))
                    Column(Modifier.weight(1f)) {
                        Text(clone.appLabel, color = c.text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(clone.packageName, color = c.textDim, style = MaterialTheme.typography.bodySmall)
                        Spacer(Modifier.size(6.dp))
                        Text("Профиль приложения", color = c.textDim, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            Spacer(Modifier.height(10.dp))
            GlassCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    InfoLine("Профиль", clone.cloneName.ifBlank { clone.appLabel })
                    InfoLine("Пакет", clone.packageName)
                    InfoLine("Статус", if (installed) "Установлено" else "Приложение не найдено")
                }
            }

            Spacer(Modifier.height(16.dp))
            NavRow(Icons.Rounded.PlayArrow, "Запустить") {
                if (!CloneManager.launch(context, clone.packageName)) a.toast("Приложение не установлено")
            }
            Spacer(Modifier.height(10.dp))
            NavRow(Icons.Rounded.ContentCopy, "Открыть системное клонирование") {
                when (CloneManager.openSystemClone(context, clone.packageName)) {
                    CloneManager.OpenResult.OPENED_CLONE_SETTINGS -> a.toast("Найдите «${clone.appLabel}» в списке и включите клон")
                    CloneManager.OpenResult.OPENED_APP_INFO -> a.toast("Откройте пункт клонирования в «О приложении»")
                    CloneManager.OpenResult.FAILED -> a.toast("На этом телефоне встроенное клонирование не найдено")
                }
            }
            Spacer(Modifier.height(10.dp))
            NavRow(Icons.Rounded.AddToHomeScreen, "Ярлык на рабочий стол") {
                val ok = CloneManager.createShortcut(context, clone.packageName, clone.cloneName.ifBlank { clone.appLabel })
                a.toast(if (ok) "Подтвердите добавление ярлыка" else "Не удалось создать ярлык")
            }
            Spacer(Modifier.height(10.dp))
            NavRow(Icons.Rounded.Palette, "Оформление") { a.nav.push(Route.Appearance(cloneId)) }
            Spacer(Modifier.height(10.dp))
            NavRow(Icons.Rounded.Backup, "Резервная копия") { a.nav.selectTab(2) }
            Spacer(Modifier.height(10.dp))
            NavRow(Icons.Rounded.Delete, "Удалить профиль", tint = Color(0xFFEF4444)) { confirmDelete = true }
            Spacer(Modifier.height(24.dp))
        }
    }

    if (confirmDelete) ConfirmDialog(
        "Удалить профиль?",
        "Будут удалены только настройки этого клона «${clone.cloneName.ifBlank { clone.appLabel }}». Само приложение, его клон и данные не затрагиваются.",
        onDismiss = { confirmDelete = false },
        onConfirm = {
            confirmDelete = false
            a.container.settings.removeClone(cloneId)
            a.nav.pop()
        },
    )
}

@Composable
private fun InfoLine(key: String, value: String) {
    val c = LocalMaysk.current
    Row(Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
        Text(key, color = c.textDim, modifier = Modifier.width(96.dp), style = MaterialTheme.typography.bodySmall)
        Text(value, color = c.text, style = MaterialTheme.typography.bodySmall)
    }
}
