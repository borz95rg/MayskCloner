package com.maysk.cloner.data

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Установленное приложение, которое можно выбрать для профиля. */
data class InstalledApp(
    val packageName: String,
    val label: String,
    val versionName: String,
    val system: Boolean,
) {
    val initial: Char get() = label.firstOrNull()?.uppercaseChar() ?: '?'
}

object Apps {
    /** Список запускаемых приложений (без самого Maysk App Cloner). */
    suspend fun installed(context: Context): List<InstalledApp> = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val self = context.packageName
        val launchable = pm.getInstalledApplications(0).mapNotNull { ai ->
            if (ai.packageName == self) return@mapNotNull null
            if (pm.getLaunchIntentForPackage(ai.packageName) == null) return@mapNotNull null
            val label = runCatching { pm.getApplicationLabel(ai).toString() }.getOrDefault(ai.packageName)
            val ver = runCatching { pm.getPackageInfo(ai.packageName, 0).versionName }.getOrNull() ?: ""
            InstalledApp(ai.packageName, label, ver, (ai.flags and ApplicationInfo.FLAG_SYSTEM) != 0)
        }
        launchable.sortedBy { it.label.lowercase() }
    }

    fun icon(context: Context, packageName: String): Drawable? =
        runCatching { context.packageManager.getApplicationIcon(packageName) }.getOrNull()

    fun isInstalled(context: Context, packageName: String): Boolean =
        runCatching { context.packageManager.getApplicationInfo(packageName, 0); true }.getOrDefault(false)

    fun launch(context: Context, packageName: String): Boolean {
        val intent = context.packageManager.getLaunchIntentForPackage(packageName) ?: return false
        return runCatching { context.startActivity(intent); true }.getOrDefault(false)
    }
}
