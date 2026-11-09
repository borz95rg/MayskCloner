package com.maysk.cloner.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material.icons.rounded.Workspaces
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.maysk.cloner.ui.LocalActions
import com.maysk.cloner.ui.SwitchRow
import com.maysk.design.LocalMaysk
import com.maysk.design.TextInputDialog
import com.maysk.design.TopBar

@Composable
fun SecurityScreen() {
    val a = LocalActions.current
    val c = LocalMaysk.current
    val s by a.container.settings.settings.collectAsState()
    var pinDialog by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {
        TopBar("Безопасность", onBack = { a.nav.pop() })
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
            Text("Защита клона", color = c.text, style = MaterialTheme.typography.titleMedium)
            Text("Ваши данные хранятся только на устройстве.", color = c.textDim, style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.size(14.dp))

            SwitchRow(Icons.Rounded.Lock, "Пароль на вход в приложение", if (s.appLock) "PIN установлен" else "Выключено", checked = s.appLock) { v ->
                if (v) pinDialog = true else a.container.settings.update { it.copy(appLock = false, appLockPin = "") }
            }
            Spacer(Modifier.size(10.dp))
            SwitchRow(Icons.Rounded.VisibilityOff, "Скрыть список клонов", "На экране блокировки", checked = s.hideClones) { v -> a.container.settings.update { it.copy(hideClones = v) } }
            Spacer(Modifier.size(10.dp))
            SwitchRow(Icons.Rounded.DeleteForever, "Подтверждение удаления", null, checked = s.confirmDelete) { v -> a.container.settings.update { it.copy(confirmDelete = v) } }
            Spacer(Modifier.size(10.dp))
            SwitchRow(Icons.Rounded.Backup, "Защита резервных копий", null, checked = s.protectBackups) { v -> a.container.settings.update { it.copy(protectBackups = v) } }
            Spacer(Modifier.size(18.dp))
            Text("Дополнительно", color = c.text, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.size(10.dp))
            SwitchRow(Icons.Rounded.Workspaces, "Раздельные данные для каждого клона", "Настройки клона хранятся отдельно от исходного приложения", checked = s.separateData) { v -> a.container.settings.update { it.copy(separateData = v) } }
            Spacer(Modifier.size(24.dp))
        }
    }

    if (pinDialog) TextInputDialog(
        "PIN-код", label = "4–8 цифр", password = true,
        onDismiss = { pinDialog = false },
        onConfirm = { pin ->
            val p = pin.filter { it.isDigit() }
            if (p.length in 4..8) a.container.settings.update { it.copy(appLock = true, appLockPin = p) } else a.toast("Введите 4–8 цифр")
            pinDialog = false
        },
    )
}
