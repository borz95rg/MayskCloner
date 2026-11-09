package com.maysk.cloner.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Shield
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.maysk.cloner.ui.LocalActions
import com.maysk.cloner.ui.Route
import com.maysk.design.GlassCard
import com.maysk.design.GradientButton
import com.maysk.design.LocalMaysk

@Composable
fun DashboardScreen() {
    val a = LocalActions.current
    val c = LocalMaysk.current
    val clones by a.container.settings.clones.collectAsState()
    val running by a.container.firewall.running.collectAsState()

    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).statusBarsPadding().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Maysk App Cloner", style = MaterialTheme.typography.titleLarge, color = c.text, modifier = Modifier.weight(1f))
            Icon(Icons.Rounded.Shield, null, tint = if (running) c.accent else c.textDim)
        }
        Spacer(Modifier.height(16.dp))

        // Hero
        GlassCard(Modifier.fillMaxWidth(), highlight = true) {
            Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(64.dp).clip(RoundedCornerShape(18.dp)).background(Brush.linearGradient(listOf(c.accent, c.accent2))), contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.GridView, null, tint = Color.White, modifier = Modifier.size(32.dp))
                }
                Spacer(Modifier.size(14.dp))
                Column(Modifier.weight(1f)) {
                    Text("Клонируйте приложения и игры", color = c.text, fontWeight = FontWeight.SemiBold)
                    Text(
                        if (running) "Файрвол активен — Google-запросы ограничены" else "Управляйте Google и авторизацией для каждого клона",
                        color = c.textDim, style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        GradientButton("Создать клон", Modifier.fillMaxWidth(), icon = Icons.Rounded.Add) { a.nav.selectTab(1); a.nav.push(Route.PickApp) }

        Spacer(Modifier.height(20.dp))
        Text("Быстрые функции", style = MaterialTheme.typography.titleMedium, color = c.text)
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            QuickTile("Мои клоны", "${clones.size} профилей", Icons.Rounded.GridView, Modifier.weight(1f)) { a.nav.selectTab(1) }
            QuickTile("Создать клон", "Быстро и легко", Icons.Rounded.Add, Modifier.weight(1f)) { a.nav.selectTab(1); a.nav.push(Route.PickApp) }
        }
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            QuickTile("Резервные копии", "Экспорт и импорт", Icons.Rounded.Backup, Modifier.weight(1f)) { a.nav.selectTab(2) }
            QuickTile("Настройки", "Тема и защита", Icons.Rounded.Settings, Modifier.weight(1f)) { a.nav.selectTab(3) }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun QuickTile(title: String, subtitle: String, icon: ImageVector, modifier: Modifier, onClick: () -> Unit) {
    val c = LocalMaysk.current
    GlassCard(modifier.height(118.dp).clickable(onClick = onClick)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Box(Modifier.size(40.dp).clip(CircleShape).background(c.accent.copy(alpha = 0.18f)), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = c.accent, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.weight(1f))
            Text(title, color = c.text, fontWeight = FontWeight.SemiBold)
            Text(subtitle, color = c.textDim, style = MaterialTheme.typography.bodySmall)
        }
    }
}
