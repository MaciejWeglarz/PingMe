package com.maciek.pingme.ui.permissions

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.annotation.StringRes
import androidx.core.app.NotificationManagerCompat
import androidx.core.net.toUri
import com.maciek.pingme.R

/** Uprawnienia i ustawienia, bez których przypomnienia nie przychodziłyby o czasie. */
enum class AppPermission(@param:StringRes val title: Int, @param:StringRes val description: Int) {
    NOTIFICATIONS(R.string.perm_notifications, R.string.perm_notifications_desc),
    FULL_SCREEN(R.string.perm_full_screen, R.string.perm_full_screen_desc),
    EXACT_ALARMS(R.string.perm_exact_alarms, R.string.perm_exact_alarms_desc),
    BATTERY(R.string.perm_battery, R.string.perm_battery_desc),
    UNUSED_APP(R.string.perm_unused_app, R.string.perm_unused_app_desc),
}

/** Uprawnienia, które mają sens na tej wersji Androida. */
fun requiredPermissions(): List<AppPermission> = buildList {
    add(AppPermission.NOTIFICATIONS)
    // Od Androida 14 zgodę na ekran pełnoekranowy trzeba czasem włączyć ręcznie.
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) add(AppPermission.FULL_SCREEN)
    // Na Androidzie 12 dokładne alarmy da się wyłączyć; od 13 USE_EXACT_ALARM jest przyznawane automatycznie.
    if (Build.VERSION.SDK_INT in Build.VERSION_CODES.S..Build.VERSION_CODES.S_V2) add(AppPermission.EXACT_ALARMS)
    add(AppPermission.BATTERY)
    // Od Androida 11 system sam cofa uprawnienia nieużywanym aplikacjom.
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) add(AppPermission.UNUSED_APP)
}

fun Context.isGranted(permission: AppPermission): Boolean = when (permission) {
    AppPermission.NOTIFICATIONS -> NotificationManagerCompat.from(this).areNotificationsEnabled()
    AppPermission.FULL_SCREEN -> Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE ||
        getSystemService(NotificationManager::class.java).canUseFullScreenIntent()
    AppPermission.EXACT_ALARMS -> Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
        getSystemService(AlarmManager::class.java).canScheduleExactAlarms()
    AppPermission.BATTERY -> getSystemService(PowerManager::class.java).isIgnoringBatteryOptimizations(packageName)
    AppPermission.UNUSED_APP -> Build.VERSION.SDK_INT < Build.VERSION_CODES.R || packageManager.isAutoRevokeWhitelisted
}

fun Context.missingPermissions(): List<AppPermission> = requiredPermissions().filterNot { isGranted(it) }

/** Ekran ustawień systemu, na którym włącza się dane uprawnienie. */
@SuppressLint("InlinedApi", "BatteryLife")
fun Context.settingsIntent(permission: AppPermission): Intent {
    val thisApp = "package:$packageName".toUri()
    return when (permission) {
        AppPermission.NOTIFICATIONS ->
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
        AppPermission.FULL_SCREEN -> Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT, thisApp)
        AppPermission.EXACT_ALARMS -> Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, thisApp)
        AppPermission.BATTERY -> Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, thisApp)
        AppPermission.UNUSED_APP -> Intent(Intent.ACTION_AUTO_REVOKE_PERMISSIONS, thisApp)
    }
}

/** Ogólny ekran „Informacje o aplikacji”, gdy telefon nie ma dedykowanego ekranu ustawień. */
fun Context.appDetailsIntent() = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, "package:$packageName".toUri())
