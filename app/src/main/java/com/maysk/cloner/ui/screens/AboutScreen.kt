package com.maysk.cloner.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.background
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.maysk.cloner.update.UpdateStatus
import com.maysk.cloner.ui.LocalActions
import com.maysk.design.GlassCard
import com.maysk.design.GradientButton
import com.maysk.design.LocalMaysk
import com.maysk.design.TopBar

@Composable
fun AboutScreen() {
    val a = LocalActions.current
    val c = LocalMaysk.current
    val status by a.container.updates.status.collectAsState()

    Column(Modifier.fillMaxSize()) {
        TopBar("О приложении", onBack = { a.nav.pop() })
        Column(Modifier.fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.size(12.dp))
            Box(Modifier.size(92.dp).clip(RoundedCornerShape(26.dp)).background(Brush.linearGradient(listOf(c.accent, c.accent2))), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.GridView, null, tint = Color.White, modifier = Modifier.size(48.dp))
            }
            Spacer(Modifier.size(14.dp))
            Text("Maysk App Cloner", color = c.text, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text("Версия ${a.container.updates.currentVersion}", color = c.textDim, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.size(8.dp))
            Text("Клонируйте. Используйте. Наслаждайтесь.", color = c.textDim, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
            Spacer(Modifier.size(20.dp))

            GlassCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        "Google-запросы блокируются локальным файрволом на вашем устройстве. " +
                            "Код сторонних приложений не изменяется, токены не подделываются, пароли не перехватываются.",
                        color = c.textDim, style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
            Spacer(Modifier.size(16.dp))
            val checking = status is UpdateStatus.Checking
            GradientButton(if (checking) "Проверяю…" else "Проверить обновления", Modifier.fillMaxWidth(), enabled = !checking) {
                a.container.updates.check(silent = false)
            }
            if (status is UpdateStatus.UpToDate) { Spacer(Modifier.size(10.dp)); Text("У вас последняя версия", color = c.textDim, style = MaterialTheme.typography.bodySmall) }
            Spacer(Modifier.size(20.dp))
            Text("© 2026 Maysk Studio", color = c.textDim, style = MaterialTheme.typography.labelSmall)
        }
    }
}
