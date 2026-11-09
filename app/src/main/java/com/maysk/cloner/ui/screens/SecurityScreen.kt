package com.maysk.cloner.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.VisibilityOff
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
            Text("Защита приложения", color = c.text, style = MaterialTheme.typography.titleMedium)
            Text("PIN хранится как защищённый хеш на этом устройстве.", color = c.textDim, style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.size(14.dp))

            SwitchRow(Icons.Rounded.Lock, "Запрашивать PIN при возвращении", if (s.appLock) "PIN установлен" else "Выключено", checked = s.appLock) { enabled ->
                if (enabled) pinDialog = true else a.container.settings.clearPin()
            }
            Spacer(Modifier.size(10.dp))
            SwitchRow(Icons.Rounded.VisibilityOff, "Скрыть экран в недавних приложениях", "Android скроет снимок окна в списке открытых приложений", checked = s.hideClones) { enabled ->
                a.container.settings.update { it.copy(hideClones = enabled) }
            }
            Spacer(Modifier.size(10.dp))
            SwitchRow(Icons.Rounded.DeleteForever, "Подтверждать удаление профиля", null, checked = s.confirmDelete) { enabled ->
                a.container.settings.update { it.copy(confirmDelete = enabled) }
            }
            Spacer(Modifier.size(10.dp))
            SwitchRow(Icons.Rounded.Backup, "Шифровать резервные копии паролем", "При каждом экспорте запрашивается отдельный пароль", checked = s.protectBackups) { enabled ->
                a.container.settings.update { it.copy(protectBackups = enabled) }
            }
            Spacer(Modifier.size(18.dp))
            Text(
                "Профиль хранит только название и ссылку на установленное приложение. Системную копию и отдельные данные создаёт телефон, если его прошивка поддерживает клонирование.",
                color = c.textDim,
                style = MaterialTheme.typography.bodySmall,
            )
            Spacer(Modifier.size(24.dp))
        }
    }

    if (pinDialog) TextInputDialog(
        "Установить PIN-код",
        label = "4–8 цифр",
        password = true,
        onDismiss = { pinDialog = false },
        onConfirm = { pin ->
            if (pin.length in 4..8 && pin.all(Char::isDigit)) a.container.settings.setPin(pin)
            else a.toast("Введите от 4 до 8 цифр")
            pinDialog = false
        },
    )
}
