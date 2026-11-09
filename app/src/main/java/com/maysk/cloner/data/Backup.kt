package com.maysk.cloner.data

import android.content.Context
import android.net.Uri

/** Экспорт и импорт резервной копии настроек клонов в выбранный пользователем файл. */
object Backup {
    fun export(context: Context, store: SettingsStore, uri: Uri): Boolean = runCatching {
        context.contentResolver.openOutputStream(uri)?.use { it.write(store.exportJson().toByteArray()) }
        true
    }.getOrDefault(false)

    fun import(context: Context, store: SettingsStore, uri: Uri): Int = runCatching {
        val text = context.contentResolver.openInputStream(uri)?.use { it.readBytes().toString(Charsets.UTF_8) } ?: return -1
        store.importJson(text)
    }.getOrDefault(-1)
}
