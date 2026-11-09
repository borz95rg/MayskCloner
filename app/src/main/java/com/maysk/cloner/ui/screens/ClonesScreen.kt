package com.maysk.cloner.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.maysk.cloner.data.CloneProfile
import com.maysk.cloner.ui.AppIcon
import com.maysk.cloner.ui.LocalActions
import com.maysk.cloner.ui.Route
import com.maysk.design.EmptyState
import com.maysk.design.GlassCard
import com.maysk.design.LocalMaysk
import com.maysk.design.TopBar

@Composable
fun ClonesScreen() {
    val a = LocalActions.current
    val c = LocalMaysk.current
    val clones by a.container.settings.clones.collectAsState()

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            TopBar("Мои профили", subtitle = "${clones.size} приложений")
            if (clones.isEmpty()) {
                EmptyState(
                    Icons.Rounded.GridView, "Пока нет профилей",
                    "Добавьте приложение, чтобы быстро открыть его системное клонирование или создать ярлык запуска.",
                    action = "Добавить приложение", onAction = { a.nav.push(Route.PickApp) },
                )
            } else {
                LazyColumn(Modifier.fillMaxSize(), contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp)) {
                    items(clones, key = { it.cloneId }) { clone ->
                        CloneRow(clone) { a.nav.push(Route.CloneCard(clone.cloneId)) }
                        Spacer(Modifier.size(12.dp))
                    }
                }
            }
        }
        androidx.compose.material3.FloatingActionButton(
            onClick = { a.nav.push(Route.PickApp) },
            containerColor = c.accent,
            modifier = Modifier.align(Alignment.BottomEnd).padding(20.dp).navBar(),
        ) { Icon(Icons.Rounded.Add, "Создать клон", tint = Color.White) }
    }
}

private fun Modifier.navBar() = this.then(Modifier)

@Composable
private fun CloneRow(clone: CloneProfile, onClick: () -> Unit) {
    val c = LocalMaysk.current
    GlassCard(Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            AppIcon(clone.packageName, 48.dp, clone.appLabel)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(clone.cloneName.ifBlank { clone.appLabel }, color = c.text, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(clone.appLabel, color = c.textDim, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.size(4.dp))
            }
        }
    }
}

