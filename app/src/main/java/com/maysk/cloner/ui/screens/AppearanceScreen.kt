package com.maysk.cloner.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.maysk.cloner.data.CloneProfile
import com.maysk.cloner.data.CloneManager
import com.maysk.cloner.ui.AppIcon
import com.maysk.cloner.ui.LocalActions
import com.maysk.cloner.ui.SwitchRow
import com.maysk.design.GlassCard
import com.maysk.design.GradientButton
import com.maysk.design.LocalMaysk
import com.maysk.design.TopBar
import androidx.compose.ui.platform.LocalContext

@Composable
fun AppearanceScreen(cloneId: String) {
    val a = LocalActions.current
    val c = LocalMaysk.current
    val context = LocalContext.current
    val clones by a.container.settings.clones.collectAsState()
    val clone = clones.firstOrNull { it.cloneId == cloneId } ?: run { a.nav.pop(); return }
    var name by remember { mutableStateOf(clone.cloneName) }

    fun edit(transform: (CloneProfile) -> CloneProfile) = a.container.settings.updateClone(cloneId, transform)

    Column(Modifier.fillMaxSize()) {
        TopBar("Профиль и ярлык", onBack = { a.nav.pop() })
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
            GlassCard(Modifier.fillMaxWidth()) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    AppIcon(clone.packageName, 56.dp, clone.appLabel)
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text("Приложение в профиле", color = c.text, style = MaterialTheme.typography.titleMedium)
                        Text("Системная копия зависит от функций телефона", color = c.textDim, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            Spacer(Modifier.size(18.dp))
            Text("Название профиля", color = c.text, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.size(8.dp))
            OutlinedTextField(
                value = name, onValueChange = { name = it }, singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.size(18.dp))
            SwitchRow(Icons.Rounded.Home, "Создать ярлык при сохранении", "Ярлык запускает исходное приложение", checked = clone.addToHome) { v -> edit { it.copy(addToHome = v) } }
            Spacer(Modifier.size(20.dp))
            GradientButton("Сохранить", Modifier.fillMaxWidth()) {
                val savedName = name.trim().ifBlank { clone.appLabel }
                edit { it.copy(cloneName = savedName) }
                val shortcutCreated = !clone.addToHome || CloneManager.createShortcut(context, clone.packageName, savedName, clone.cloneId)
                val message = when {
                    !clone.addToHome -> "Профиль сохранён"
                    shortcutCreated -> "Профиль сохранён. Подтвердите создание ярлыка"
                    else -> "Профиль сохранён, но ярлык не создан"
                }
                a.toast(message)
                a.nav.pop()
            }
            Spacer(Modifier.size(24.dp))
        }
    }
}
