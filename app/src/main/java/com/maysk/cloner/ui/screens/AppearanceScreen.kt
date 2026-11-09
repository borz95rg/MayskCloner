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
import com.maysk.cloner.data.CloneGoogleSettings
import com.maysk.cloner.data.LabelStyle
import com.maysk.cloner.ui.AppIcon
import com.maysk.cloner.ui.LocalActions
import com.maysk.cloner.ui.SwitchRow
import com.maysk.design.ChoiceChipRow
import com.maysk.design.GlassCard
import com.maysk.design.GradientButton
import com.maysk.design.LocalMaysk
import com.maysk.design.TopBar

@Composable
fun AppearanceScreen(cloneId: String) {
    val a = LocalActions.current
    val c = LocalMaysk.current
    val clones by a.container.settings.clones.collectAsState()
    val clone = clones.firstOrNull { it.cloneId == cloneId } ?: run { a.nav.pop(); return }
    var name by remember { mutableStateOf(clone.cloneName) }

    fun edit(transform: (CloneGoogleSettings) -> CloneGoogleSettings) = a.container.settings.updateClone(cloneId, transform)

    Column(Modifier.fillMaxSize()) {
        TopBar("Оформление", onBack = { a.nav.pop() })
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
            GlassCard(Modifier.fillMaxWidth()) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    AppIcon(clone.packageName, 56.dp, clone.appLabel)
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text("Иконка клона", color = c.text, style = MaterialTheme.typography.titleMedium)
                        Text("Берётся из исходного приложения", color = c.textDim, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            Spacer(Modifier.size(18.dp))
            Text("Название клона", color = c.text, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.size(8.dp))
            OutlinedTextField(
                value = name, onValueChange = { name = it }, singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.size(18.dp))
            Text("Стиль ярлыка", color = c.text, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.size(10.dp))
            ChoiceChipRow(
                LabelStyle.entries.map { it.title },
                LabelStyle.entries.indexOf(clone.labelStyle),
                { edit { cl -> cl.copy(labelStyle = LabelStyle.entries[it]) } },
            )
            Spacer(Modifier.size(16.dp))
            SwitchRow(Icons.Rounded.Home, "Добавить на рабочий стол", null, checked = clone.addToHome) { v -> edit { it.copy(addToHome = v) } }
            Spacer(Modifier.size(20.dp))
            GradientButton("Сохранить", Modifier.fillMaxWidth()) {
                edit { it.copy(cloneName = name.trim().ifBlank { clone.appLabel }) }
                a.toast("Сохранено")
                a.nav.pop()
            }
            Spacer(Modifier.size(24.dp))
        }
    }
}
