package com.maysk.cloner.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Android
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.maysk.cloner.data.Apps
import com.maysk.design.LocalMaysk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Иконка установленного приложения (или запасной значок, если приложение не найдено). */
@Composable
fun AppIcon(packageName: String, size: Dp, label: String = "") {
    val context = LocalContext.current
    val shape = RoundedCornerShape(size / 4)
    val c = LocalMaysk.current
    val bitmap by produceState<ImageBitmap?>(null, packageName) {
        value = withContext(Dispatchers.IO) {
            runCatching { Apps.icon(context, packageName)?.toBitmap(96, 96)?.asImageBitmap() }.getOrNull()
        }
    }
    Box(Modifier.size(size).clip(shape).background(c.accent.copy(alpha = 0.18f)), contentAlignment = Alignment.Center) {
        val bmp = bitmap
        when {
            bmp != null -> Image(bmp, null, Modifier.size(size).clip(shape))
            label.isNotEmpty() -> Text(label.first().uppercaseChar().toString(), color = c.accent, fontWeight = FontWeight.Bold, fontSize = (size.value / 2.4f).sp)
            else -> Icon(Icons.Rounded.Android, null, tint = c.accent, modifier = Modifier.size(size * 0.55f))
        }
    }
}

/** Строка настройки с переключателем в «стеклянной» карточке. */
@Composable
fun SwitchRow(
    icon: ImageVector?,
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    enabled: Boolean = true,
    onChange: (Boolean) -> Unit,
) {
    val c = LocalMaysk.current
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(c.cardBg)
            .clickable(enabled = enabled) { onChange(!checked) }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Box(Modifier.size(34.dp).clip(CircleShape).background(c.accent.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = c.accent, modifier = Modifier.size(19.dp))
            }
            Spacer(Modifier.width(12.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(title, color = if (enabled) c.text else c.textDim, style = MaterialTheme.typography.bodyLarge)
            if (subtitle != null) Text(subtitle, color = c.textDim, fontSize = 12.sp)
        }
        Spacer(Modifier.width(8.dp))
        Switch(
            checked = checked, onCheckedChange = onChange, enabled = enabled,
            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = c.accent),
        )
    }
}

/** Строка-пункт с иконкой и стрелкой (как в карточке клона). */
@Composable
fun NavRow(icon: ImageVector, title: String, tint: Color? = null, onClick: () -> Unit) {
    val c = LocalMaysk.current
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(c.cardBg)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = tint ?: c.accent, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(14.dp))
        Text(title, color = tint ?: c.text, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        Text("›", color = c.textDim, fontSize = 20.sp)
    }
}
