package com.maysk.cloner.ui

import android.os.SystemClock
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

@Composable
fun AppLockGate(onUnlock: (String) -> Boolean) {
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var failedAttempts by remember { mutableIntStateOf(0) }
    var blockedUntil by remember { mutableLongStateOf(0L) }
    var now by remember { mutableLongStateOf(SystemClock.elapsedRealtime()) }

    LaunchedEffect(blockedUntil) {
        while (SystemClock.elapsedRealtime() < blockedUntil) {
            now = SystemClock.elapsedRealtime()
            delay(1_000)
        }
        now = SystemClock.elapsedRealtime()
    }
    val waitSeconds = ((blockedUntil - now).coerceAtLeast(0L) / 1_000L).toInt()

    Column(
        Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(Icons.Rounded.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(42.dp))
        Spacer(Modifier.height(16.dp))
        Text("Приложение защищено", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))
        Text("Введите PIN-код, чтобы продолжить", style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(20.dp))
        OutlinedTextField(
            value = pin,
            onValueChange = { pin = it.filter(Char::isDigit).take(8); error = null },
            label = { Text("PIN-код") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            isError = error != null,
            supportingText = { Text(error ?: "") },
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(12.dp))
        Button(
            onClick = {
                if (waitSeconds > 0) return@Button
                if (onUnlock(pin)) {
                    pin = ""
                    error = null
                    failedAttempts = 0
                } else {
                    pin = ""
                    failedAttempts++
                    error = if (failedAttempts >= 5) {
                        blockedUntil = SystemClock.elapsedRealtime() + 30_000L
                        failedAttempts = 0
                        "Слишком много попыток. Подождите 30 секунд"
                    } else "Неверный PIN"
                }
            },
            enabled = pin.isNotEmpty() && waitSeconds == 0,
            modifier = Modifier.fillMaxWidth(),
        ) { Text(if (waitSeconds > 0) "Подождите ${waitSeconds + 1} сек." else "Разблокировать") }
    }
}
