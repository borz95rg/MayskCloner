package com.maysk.cloner.ui.screens

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.maysk.cloner.data.Backup
import com.maysk.cloner.ui.LocalActions
import com.maysk.cloner.ui.NavRow
import com.maysk.design.GlassCard
import com.maysk.design.GradientButton
import com.maysk.design.LocalMaysk
import com.maysk.design.TopBar

@Composable
fun BackupScreen() {
    val a = LocalActions.current
    val c = LocalMaysk.current
    val context = LocalContext.current

    val exporter = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) {
            val ok = Backup.export(context, a.container.settings, uri)
            a.toast(if (ok) "Резервная копия сохранена" else "Не удалось сохранить")
        }
    }
    val importer = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            val n = Backup.import(context, a.container.settings, uri)
            if (n >= 0) { a.applyFirewall(); a.toast("Восстановлено профилей: $n") } else a.toast("Не удалось прочитать файл")
        }
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
                    Text("Резервное копирование настроек клонов", color = c.text, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
                    Spacer(Modifier.size(6.dp))
                    Text("Сохраните настройки профилей и Google для восстановления в будущем.", color = c.textDim, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
                }
            }
            Spacer(Modifier.size(16.dp))
            GradientButton("Создать резервную копию", Modifier.fillMaxWidth(), icon = Icons.Rounded.SaveAlt) {
                exporter.launch("maysk_cloner_backup.json")
            }
            Spacer(Modifier.size(12.dp))
            NavRow(Icons.Rounded.Restore, "Восстановление из копии") { importer.launch(arrayOf("application/json", "text/*", "*/*")) }
        }
    }
}
