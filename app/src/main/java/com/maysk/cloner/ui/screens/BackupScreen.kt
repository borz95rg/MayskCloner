package com.maysk.cloner.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudUpload
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.SaveAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.maysk.cloner.data.Backup
import com.maysk.cloner.data.BackupImportResult
import com.maysk.cloner.ui.LocalActions
import com.maysk.cloner.ui.NavRow
import com.maysk.design.GlassCard
import com.maysk.design.GradientButton
import com.maysk.design.LocalMaysk
import com.maysk.design.TextInputDialog
import com.maysk.design.TopBar

@Composable
fun BackupScreen() {
    val a = LocalActions.current
    val c = LocalMaysk.current
    val context = LocalContext.current
    val settings by a.container.settings.settings.collectAsState()
    var exportPasswordStep by remember { mutableIntStateOf(0) }
    var exportPasswordFirst by remember { mutableStateOf("") }
    var pendingExportPassword by remember { mutableStateOf<String?>(null) }
    var pendingImportUri by remember { mutableStateOf<Uri?>(null) }
    var importPasswordDialog by remember { mutableStateOf(false) }
    var importAttempt by remember { mutableIntStateOf(0) }

    val exporter = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) {
            val ok = Backup.export(context, a.container.settings, uri, pendingExportPassword)
            a.toast(if (ok) "Резервная копия сохранена" else "Не удалось записать файл")
        }
        pendingExportPassword = null
        exportPasswordFirst = ""
    }

    fun importBackup(uri: Uri, password: String? = null) {
        when (val result = Backup.import(context, a.container.settings, uri, password)) {
            BackupImportResult.PasswordRequired -> {
                pendingImportUri = uri
                importPasswordDialog = true
            }
            is BackupImportResult.Imported -> {
                pendingImportUri = null
                importPasswordDialog = false
                a.toast("Восстановлено профилей: ${result.profiles}")
            }
            BackupImportResult.WrongPasswordOrDamaged -> {
                importPasswordDialog = true
                importAttempt++
                a.toast("Пароль неверный или копия повреждена")
            }
            BackupImportResult.InvalidFile -> {
                pendingImportUri = null
                importPasswordDialog = false
                a.toast("Файл резервной копии повреждён или имеет неподдерживаемую версию")
            }
        }
    }

    val importer = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) importBackup(uri)
    }

    Column(Modifier.fillMaxSize()) {
        TopBar("Резервная копия")
        Column(Modifier.fillMaxSize().padding(16.dp)) {
            GlassCard(Modifier.fillMaxWidth()) {
                Column(Modifier.fillMaxWidth().padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(Modifier.size(72.dp).clip(CircleShape).background(c.accent.copy(alpha = 0.18f)), contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.CloudUpload, null, tint = c.accent, modifier = Modifier.size(38.dp))
                    }
                    Spacer(Modifier.size(14.dp))
                    Text("Резервная копия профилей", color = c.text, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
                    Spacer(Modifier.size(6.dp))
                    Text(
                        if (settings.protectBackups) "Экспорт шифруется паролем. PIN-код приложения в копию не попадает."
                        else "Сохраняются названия профилей и ссылки на приложения. PIN-код приложения в копию не попадает.",
                        color = c.textDim,
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                    )
                }
            }
            Spacer(Modifier.size(16.dp))
            GradientButton("Создать резервную копию", Modifier.fillMaxWidth(), icon = Icons.Rounded.SaveAlt) {
                if (settings.protectBackups) exportPasswordStep = 1
                else exporter.launch("maysk_cloner_backup.json")
            }
            Spacer(Modifier.size(12.dp))
            NavRow(Icons.Rounded.Restore, "Восстановить из копии") { importer.launch(arrayOf("application/json", "text/*", "*/*")) }
        }
    }

    if (exportPasswordStep == 1) TextInputDialog(
        title = "Задайте пароль копии",
        label = "Минимум 8 символов",
        password = true,
        onDismiss = { exportPasswordStep = 0 },
        onConfirm = { password ->
            if (password.length >= 8) {
                exportPasswordFirst = password
                exportPasswordStep = 2
            } else {
                exportPasswordStep = 0
                a.toast("Пароль должен содержать не менее 8 символов")
            }
        },
    )

    if (exportPasswordStep == 2) TextInputDialog(
        title = "Повторите пароль копии",
        label = "Пароль",
        password = true,
        onDismiss = { exportPasswordStep = 0; exportPasswordFirst = "" },
        onConfirm = { password ->
            if (password == exportPasswordFirst) {
                pendingExportPassword = password
                exporter.launch("maysk_cloner_backup.json")
            } else {
                a.toast("Пароли не совпадают")
            }
            exportPasswordStep = 0
            exportPasswordFirst = ""
        },
    )

    val importUri = pendingImportUri
    if (importPasswordDialog && importUri != null) key(importAttempt) {
        TextInputDialog(
            title = "Пароль резервной копии",
            label = "Введите пароль",
            password = true,
            onDismiss = { importPasswordDialog = false; pendingImportUri = null },
            onConfirm = { password -> importBackup(importUri, password) },
        )
    }
}
