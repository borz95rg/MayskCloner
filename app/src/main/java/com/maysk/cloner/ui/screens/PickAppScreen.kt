package com.maysk.cloner.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.maysk.cloner.data.Apps
import com.maysk.cloner.data.CloneProfile
import com.maysk.cloner.data.InstalledApp
import com.maysk.cloner.ui.AppIcon
import com.maysk.cloner.ui.LocalActions
import com.maysk.cloner.ui.Route
import com.maysk.design.LocalMaysk
import com.maysk.design.TopBar

@Composable
fun PickAppScreen() {
    val a = LocalActions.current
    val c = LocalMaysk.current
    val context = LocalContext.current
    var query by remember { mutableStateOf("") }
    val existing = a.container.settings.clones.value.map { it.packageName }.toSet()

    val apps by produceState<List<InstalledApp>?>(null) { value = Apps.installed(context) }

    Column(Modifier.fillMaxSize()) {
        TopBar("Выберите приложение", onBack = { a.nav.pop() })
        OutlinedTextField(
            value = query, onValueChange = { query = it },
            label = { Text("Поиск") }, singleLine = true,
            keyboardOptions = KeyboardOptions.Default,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        )
        Spacer(Modifier.padding(4.dp))
        val list = apps
        when {
            list == null -> Row(Modifier.fillMaxWidth().padding(32.dp), horizontalArrangement = androidx.compose.foundation.layout.Arrangement.Center) { CircularProgressIndicator(color = c.accent) }
            else -> {
                val filtered = list.filter { query.isBlank() || it.label.contains(query, true) || it.packageName.contains(query, true) }
                LazyColumn(Modifier.fillMaxSize(), contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp)) {
                    items(filtered, key = { it.packageName }) { app ->
                        AppPickRow(app, already = app.packageName in existing) {
                            val id = "clone_${System.currentTimeMillis()}"
                            val clone = CloneProfile(
                                cloneId = id, packageName = app.packageName, appLabel = app.label,
                                cloneName = "${app.label} Клон 1",
                            )
                            a.container.settings.addClone(clone)
                            a.nav.pop()
                            a.nav.push(Route.CloneCard(id))
                        }
                        Spacer(Modifier.padding(5.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun AppPickRow(app: InstalledApp, already: Boolean, onClick: () -> Unit) {
    val c = LocalMaysk.current
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppIcon(app.packageName, 44.dp, app.label)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(app.label, color = c.text, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Medium)
            Text(app.packageName, color = c.textDim, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (already) Text("Добавлено", color = c.accent, style = MaterialTheme.typography.labelSmall)
    }
}
