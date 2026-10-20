package com.maciek.pingme.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.maciek.pingme.ui.permissions.PermissionsScreen
import com.maciek.pingme.ui.permissions.missingPermissions

/**
 * Bramka uprawnień: przy każdym starcie i powrocie do aplikacji sprawdza uprawnienia.
 * Dopóki czegoś brakuje (także gdy system sam je cofnął), zamiast aplikacji pokazuje ekran uprawnień.
 */
@Composable
fun PingMeRoot() {
    val context = LocalContext.current
    var missing by remember { mutableStateOf(context.missingPermissions()) }
    LifecycleResumeEffect(Unit) {
        missing = context.missingPermissions()
        onPauseOrDispose { }
    }
    if (missing.isEmpty()) {
        PingMeNavHost()
    } else {
        PermissionsScreen(missing, onChanged = { missing = context.missingPermissions() })
    }
}
