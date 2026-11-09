package com.maysk.cloner.data

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.Bitmap
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.Icon
import android.net.Uri
import android.os.Build
import android.provider.Settings

/**
 * Менеджер клонов: не пересобирает чужие приложения, а помогает пользователю создать копию
 * штатными средствами — встроенным клонированием производителя телефона и ярлыками Android.
 */
object CloneManager {

    enum class OpenResult { OPENED_CLONE_SETTINGS, OPENED_APP_INFO, FAILED }

    /**
     * Открыть встроенное клонирование приложений телефона. У каждого производителя своя
     * настройка (Samsung Dual Messenger, Xiaomi «Клонирование приложений», Huawei App Twin и т.д.).
     * Пробуем известные экраны по очереди; если не нашли — открываем «О приложении», где у многих
     * прошивок есть пункт клонирования.
     */
    fun openSystemClone(context: Context, packageName: String): OpenResult {
        for (intent in cloneSettingsCandidates()) {
            if (tryStart(context, intent)) return OpenResult.OPENED_CLONE_SETTINGS
        }
        // Запасной путь: страница «О приложении» конкретного приложения.
        val appInfo = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName"))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (tryStart(context, appInfo)) return OpenResult.OPENED_APP_INFO
        return OpenResult.FAILED
    }

    private fun cloneSettingsCandidates(): List<Intent> {
        fun comp(pkg: String, cls: String) = Intent().setComponent(ComponentName(pkg, cls)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return listOf(
            // Xiaomi / Redmi / POCO (MIUI / HyperOS)
            comp("com.miui.securitycore", "com.miui.xspace.ui.activity.XSpaceSettingActivity"),
            comp("com.android.settings", "com.android.settings.Settings\$DualAppsSettingsActivity"),
            // Samsung (One UI) — Dual Messenger
            comp("com.samsung.android.settings", "com.samsung.android.settings.dualapps.DualAppsActivity"),
            Intent("com.samsung.android.settings.DUAL_MESSENGER").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            // Oppo / Realme (ColorOS) и vivo (Funtouch) — Клонирование приложений
            comp("com.coloros.settings", "com.coloros.settings.feature.cloneapp.ClonedAppSettingsActivity"),
            comp("com.vivo.doubleinstance", "com.vivo.doubleinstance.MainActivity"),
            // Huawei / Honor — App Twin
            comp("com.android.settings", "com.android.settings.Settings\$AppMultiSettingsActivity"),
        )
    }

    private fun tryStart(context: Context, intent: Intent): Boolean = try {
        if (intent.resolveActivity(context.packageManager) != null) { context.startActivity(intent); true } else false
    } catch (e: Exception) { false }

    /** Есть ли на телефоне встроенное клонирование (нашёлся хотя бы один экран). */
    fun hasSystemClone(context: Context): Boolean =
        cloneSettingsCandidates().any { it.resolveActivity(context.packageManager) != null }

    /** Создать ярлык запуска приложения на рабочем столе (с именем клона). */
    fun createShortcut(context: Context, packageName: String, label: String, profileId: String = packageName): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return false
        val launch = context.packageManager.getLaunchIntentForPackage(packageName) ?: return false
        launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        val sm = context.getSystemService(ShortcutManager::class.java) ?: return false
        if (!sm.isRequestPinShortcutSupported) return false
        val icon = appIcon(context, packageName)
        val id = "clone_${packageName}_${profileId.hashCode().toString(16)}"
        val info = ShortcutInfo.Builder(context, id)
            .setShortLabel(label.ifBlank { packageName })
            .setIntent(launch.setAction(Intent.ACTION_MAIN))
            .apply { if (icon != null) setIcon(icon) }
            .build()
        return runCatching { sm.requestPinShortcut(info, null) }.getOrDefault(false)
    }

    private fun appIcon(context: Context, packageName: String): Icon? = try {
        val d: Drawable = context.packageManager.getApplicationIcon(packageName)
        val bmp = drawableToBitmap(d)
        if (bmp != null) Icon.createWithBitmap(bmp) else null
    } catch (e: Exception) { null }

    private fun drawableToBitmap(d: Drawable): Bitmap? = try {
        when {
            d is BitmapDrawable && d.bitmap != null -> d.bitmap
            else -> {
                val w = (if (d.intrinsicWidth > 0) d.intrinsicWidth else 108).coerceAtMost(256)
                val h = (if (d.intrinsicHeight > 0) d.intrinsicHeight else 108).coerceAtMost(256)
                val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                val canvas = android.graphics.Canvas(bmp)
                d.setBounds(0, 0, w, h)
                d.draw(canvas)
                bmp
            }
        }
    } catch (e: Exception) { null }

    /** Запустить исходное приложение. */
    fun launch(context: Context, packageName: String): Boolean {
        val intent = context.packageManager.getLaunchIntentForPackage(packageName) ?: return false
        return runCatching { context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); true }.getOrDefault(false)
    }

    /** Открыть «О приложении» (там же обычно и пункт клонирования производителя). */
    fun openAppInfo(context: Context, packageName: String): Boolean {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName"))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return tryStart(context, intent)
    }
}
