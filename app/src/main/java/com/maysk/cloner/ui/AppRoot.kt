package com.maysk.cloner.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.maysk.cloner.AppContainer
import com.maysk.cloner.update.UpdateStatus
import com.maysk.cloner.ui.screens.AboutScreen
import com.maysk.cloner.ui.screens.AppearanceScreen
import com.maysk.cloner.ui.screens.BackupScreen
import com.maysk.cloner.ui.screens.CloneCardScreen
import com.maysk.cloner.ui.screens.ClonesScreen
import com.maysk.cloner.ui.screens.DashboardScreen
import com.maysk.cloner.ui.screens.PickAppScreen
import com.maysk.cloner.ui.screens.SecurityScreen
import com.maysk.cloner.ui.screens.SettingsScreen
import com.maysk.design.AppBackground
import com.maysk.design.LocalMaysk

@Composable
fun AppRoot(container: AppContainer) {
    val settings by container.settings.settings.collectAsState()
    val dark = isSystemInDarkTheme()
    val colors = remember(settings, dark) { buildColors(settings, dark) }
    val nav = remember { Nav() }

    val actions = remember {
        UiActions(
            container = container,
            nav = nav,
            toast = { msg -> android.widget.Toast.makeText(container.appContext, msg, android.widget.Toast.LENGTH_SHORT).show() },
        )
    }

    ClonerTheme(settings, colors) {
        AppBackground {
            CompositionLocalProvider(LocalActions provides actions) {
                Column(Modifier.fillMaxSize()) {
                    Box(Modifier.weight(1f)) { RouteContent(nav.current) }
                    if (nav.stack.isEmpty()) BottomNav(nav)
                }
                UpdateDialogs(container)
            }
        }
    }
}

@Composable
private fun RouteContent(r: Route) {
    when (r) {
        Route.Dashboard -> DashboardScreen()
        Route.Clones -> ClonesScreen()
        Route.Backup -> BackupScreen()
        Route.Settings -> SettingsScreen()
        Route.PickApp -> PickAppScreen()
        Route.Security -> SecurityScreen()
        Route.About -> AboutScreen()
        is Route.CloneCard -> CloneCardScreen(r.id)
        is Route.Appearance -> AppearanceScreen(r.id)
    }
}

@Composable
private fun BottomNav(nav: Nav) {
    val c = LocalMaysk.current
    val items = listOf(
        Triple("Главная", Icons.Rounded.Home, 0),
        Triple("Клоны", Icons.Rounded.GridView, 1),
        Triple("Резерв", Icons.Rounded.Backup, 2),
        Triple("Настройки", Icons.Rounded.Settings, 3),
    )
    Row(
        Modifier.fillMaxWidth()
            .background(if (c.isLight) Color.White.copy(alpha = 0.9f) else Color.Black.copy(alpha = 0.5f))
            .navigationBarsPadding()
            .height(64.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        items.forEach { (label, icon: ImageVector, i) ->
            val sel = nav.tab == i
            Column(
                Modifier.weight(1f).clickable { nav.selectTab(i) }.padding(vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(icon, label, tint = if (sel) c.accent else c.textDim, modifier = Modifier.size(24.dp))
                Spacer(Modifier.size(2.dp))
                Text(label, color = if (sel) c.accent else c.textDim, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = if (sel) FontWeight.SemiBold else FontWeight.Normal)
            }
        }
    }
}

@Composable
private fun UpdateDialogs(container: AppContainer) {
    val status by container.updates.status.collectAsState()
    when (val s = status) {
        is UpdateStatus.Available -> AlertDialog(
            onDismissRequest = { container.updates.dismiss() },
            title = { Text("Доступно обновление ${s.info.versionName}") },
            text = {
                Column {
                    Text("Установлена версия ${container.updates.currentVersion}.")
                    if (s.info.notes.isNotBlank()) { Spacer(Modifier.height(8.dp)); Text("Что нового:", fontWeight = FontWeight.SemiBold); Text(s.info.notes, maxLines = 10, overflow = TextOverflow.Ellipsis) }
                }
            },
            confirmButton = { TextButton(onClick = { container.updates.download(s.info) }) { Text("Обновить") } },
            dismissButton = { TextButton(onClick = { container.updates.dismiss() }) { Text("Позже") } },
        )
        is UpdateStatus.Downloading -> AlertDialog(
            onDismissRequest = {},
            title = { Text("Загрузка обновления") },
            text = { Column { LinearProgressIndicator(progress = { s.progress }, modifier = Modifier.fillMaxWidth()); Spacer(Modifier.height(8.dp)); Text("${(s.progress * 100).toInt()}%") } },
            confirmButton = {},
        )
        is UpdateStatus.ReadyToInstall -> AlertDialog(
            onDismissRequest = { container.updates.dismiss() },
            title = { Text("Обновление загружено") },
            text = { Text(if (container.updates.canInstallPackages()) "Нажмите «Установить»." else "Разрешите установку из этого источника, затем вернитесь и нажмите «Установить».") },
            confirmButton = { TextButton(onClick = { container.updates.install(s.file) }) { Text("Установить") } },
            dismissButton = { TextButton(onClick = { container.updates.dismiss() }) { Text("Позже") } },
        )
        is UpdateStatus.Error -> AlertDialog(
            onDismissRequest = { container.updates.dismiss() },
            title = { Text("Обновление") },
            text = { Text(s.message) },
            confirmButton = { TextButton(onClick = { container.updates.dismiss() }) { Text("ОК") } },
        )
        else -> {}
    }
}
