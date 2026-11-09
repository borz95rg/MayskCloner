package com.maysk.cloner.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Login
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.ShoppingBag
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.maysk.cloner.data.CloneGoogleSettings
import com.maysk.cloner.engine.GoogleMode
import com.maysk.cloner.ui.LocalActions
import com.maysk.cloner.ui.SwitchRow
import com.maysk.design.GlassCard
import com.maysk.design.LocalMaysk
import com.maysk.design.TopBar

@Composable
fun GoogleAuthScreen(cloneId: String) {
    val a = LocalActions.current
    val c = LocalMaysk.current
    val clones by a.container.settings.clones.collectAsState()
    val running by a.container.firewall.running.collectAsState()
    val clone = clones.firstOrNull { it.cloneId == cloneId } ?: run { a.nav.pop(); return }

    fun edit(transform: (CloneGoogleSettings) -> CloneGoogleSettings) {
        a.container.settings.updateClone(cloneId) { transform(it) }
        a.applyFirewall()
    }

    Column(Modifier.fillMaxSize()) {
        TopBar("Google и авторизация", onBack = { a.nav.pop() }, subtitle = clone.appLabel)
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {

            // Заголовок с логотипом
            GlassCard(Modifier.fillMaxWidth()) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    com.maysk.cloner.ui.AppIcon(clone.packageName, 44.dp, clone.appLabel)
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Управление Google для этого клона", color = c.text, fontWeight = FontWeight.SemiBold)
                        Text("Настройте, как будет работать Google Services и microG", color = c.textDim, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            Spacer(Modifier.height(18.dp))
            Text("Режим Google", style = MaterialTheme.typography.titleMedium, color = c.text)
            Spacer(Modifier.height(10.dp))
            ModeOption(GoogleMode.STANDARD, Icons.Rounded.CheckCircle, clone.googleMode) { edit { it.copy(googleMode = GoogleMode.STANDARD) } }
            Spacer(Modifier.height(10.dp))
            ModeOption(GoogleMode.NO_AUTO_GOOGLE, Icons.Rounded.Bolt, clone.googleMode) { edit { it.copy(googleMode = GoogleMode.NO_AUTO_GOOGLE) } }
            Spacer(Modifier.height(10.dp))
            ModeOption(GoogleMode.COMPAT, Icons.Rounded.Shield, clone.googleMode) { edit { it.copy(googleMode = GoogleMode.COMPAT) } }

            if (clone.googleMode == GoogleMode.NO_AUTO_GOOGLE) {
                Spacer(Modifier.height(18.dp))
                Text("Что блокировать", style = MaterialTheme.typography.titleMedium, color = c.text)
                Text(
                    if (running) "Файрвол активен — выбранные запросы Google не уходят в сеть." else "Включите хотя бы один клон в этом режиме, чтобы запустить файрвол.",
                    color = c.textDim, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 2.dp, bottom = 10.dp),
                )
                SwitchRow(Icons.Rounded.Login, "Вход в аккаунт Google", "Отключите автоматический Google Sign-In", checked = !clone.autoGoogleSignIn) { edit { c2 -> c2.copy(autoGoogleSignIn = !it) } }
                Spacer(Modifier.height(10.dp))
                SwitchRow(Icons.Rounded.SportsEsports, "Google Play Games", "Блокировать сервис игр", checked = !clone.playGames) { edit { c2 -> c2.copy(playGames = !it) } }
                Spacer(Modifier.height(10.dp))
                SwitchRow(Icons.Rounded.VerifiedUser, "microG Services", "Блокировать обращения к microG", checked = !clone.microG) { edit { c2 -> c2.copy(microG = !it) } }
                Spacer(Modifier.height(10.dp))
                SwitchRow(Icons.Rounded.Language, "Внешний браузер", "Не открывать браузер для входа Google", checked = !clone.externalBrowser) { edit { c2 -> c2.copy(externalBrowser = !it) } }
                Spacer(Modifier.height(10.dp))
                SwitchRow(Icons.Rounded.ShoppingBag, "Переход в Google Play", "Блокировать переходы в магазин", checked = !clone.googlePlayRedirect) { edit { c2 -> c2.copy(googlePlayRedirect = !it) } }
            }

            Spacer(Modifier.height(18.dp))
            Text("Сохранение данных", style = MaterialTheme.typography.titleMedium, color = c.text)
            Spacer(Modifier.height(10.dp))
            SwitchRow(null, "Сохранять обычную авторизацию", "Собственный логин/пароль приложения не блокируется", checked = clone.preserveLogin) { v -> edit { it.copy(preserveLogin = v) } }
            Spacer(Modifier.height(10.dp))
            SwitchRow(null, "Сохранять данные игрового аккаунта", "Данные и сессия игры не сбрасываются", checked = clone.preserveGameData) { v -> edit { it.copy(preserveGameData = v) } }

            Spacer(Modifier.height(18.dp))
            // Индикатор результата совместимости
            GlassCard(Modifier.fillMaxWidth()) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.CheckCircle, null, tint = Color(0xFF22C55E))
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("Проверка совместимости", color = c.text, fontWeight = FontWeight.SemiBold)
                        Text(statusText(clone, running), color = c.textDim, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

private fun statusText(c: CloneGoogleSettings, running: Boolean): String = when (c.googleMode) {
    GoogleMode.STANDARD -> "Google Services работают как обычно."
    GoogleMode.NO_AUTO_GOOGLE -> if (running) "Необязательные Google-запросы ограничены. Свой вход приложения сохранён." else "Режим выбран. Файрвол запустится при включении VPN-разрешения."
    GoogleMode.COMPAT -> "Полная совместимость: ничего не ограничивается."
}

@Composable
private fun ModeOption(mode: GoogleMode, icon: androidx.compose.ui.graphics.vector.ImageVector, current: GoogleMode, onClick: () -> Unit) {
    val c = LocalMaysk.current
    val selected = mode == current
    Row(
        Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(c.cardBg)
            .border(if (selected) 2.dp else 1.dp, if (selected) c.accent else c.border, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = if (selected) c.accent else c.textDim, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(mode.title, color = c.text, fontWeight = FontWeight.SemiBold)
            Text(mode.subtitle, color = c.textDim, style = MaterialTheme.typography.bodySmall)
        }
        Box(Modifier.size(22.dp).clip(CircleShape).border(2.dp, if (selected) c.accent else c.textDim, CircleShape), contentAlignment = Alignment.Center) {
            if (selected) Box(Modifier.size(12.dp).clip(CircleShape).background(c.accent))
        }
    }
}
