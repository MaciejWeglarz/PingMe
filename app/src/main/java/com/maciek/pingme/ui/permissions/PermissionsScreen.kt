package com.maciek.pingme.ui.permissions

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Context
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import com.maciek.pingme.R
import com.maciek.pingme.ui.theme.PingMeTheme

/**
 * Blokujący ekran uprawnień: pokazuje się zamiast aplikacji, dopóki czegoś brakuje.
 * [onChanged] wywołujemy po odpowiedzi na systemową prośbę, żeby od razu odświeżyć statusy.
 */
@Composable
fun PermissionsScreen(missing: List<AppPermission>, onChanged: () -> Unit) {
    val context = LocalContext.current
    val notificationLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        // Po odmowie system przestaje pokazywać okienko, więc otwieramy ustawienia powiadomień.
        if (!granted) context.openSettings(AppPermission.NOTIFICATIONS)
        onChanged()
    }
    PermissionsContent(
        required = requiredPermissions(),
        missing = missing,
        onEnable = { permission ->
            if (permission == AppPermission.NOTIFICATIONS && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                context.openSettings(permission)
            }
        },
    )
    IntroDialog()
}

private fun Context.openSettings(permission: AppPermission) {
    try {
        startActivity(settingsIntent(permission))
    } catch (_: ActivityNotFoundException) {
        startActivity(appDetailsIntent())
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PermissionsContent(
    required: List<AppPermission>,
    missing: List<AppPermission>,
    onEnable: (AppPermission) -> Unit,
) {
    Scaffold(topBar = { TopAppBar(title = { Text(stringResource(R.string.permissions_title)) }) }) { padding ->
        LazyColumn(
            contentPadding = padding,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(horizontal = 16.dp),
        ) {
            item {
                Text(stringResource(R.string.permissions_intro), style = MaterialTheme.typography.bodyLarge)
            }
            items(required) { permission ->
                PermissionCard(permission, granted = permission !in missing, onEnable = { onEnable(permission) })
            }
        }
    }
}

@Composable
private fun PermissionCard(permission: AppPermission, granted: Boolean, onEnable: () -> Unit) {
    OutlinedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(stringResource(permission.title), style = MaterialTheme.typography.titleMedium)
            Text(
                stringResource(permission.description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 8.dp),
            )
            if (granted) {
                Text(
                    stringResource(R.string.permission_granted),
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.align(Alignment.End),
                )
            } else {
                Button(onClick = onEnable, modifier = Modifier.align(Alignment.End)) {
                    Text(stringResource(R.string.permission_enable))
                }
            }
        }
    }
}

private const val PREFS = "pingme"
private const val KEY_INTRO_SHOWN = "intro_shown"

/** Dialog pokazywany tylko przy pierwszym uruchomieniu aplikacji. */
@Composable
private fun IntroDialog() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences(PREFS, Context.MODE_PRIVATE) }
    var visible by remember { mutableStateOf(!prefs.getBoolean(KEY_INTRO_SHOWN, false)) }
    if (!visible) return
    val close = {
        prefs.edit { putBoolean(KEY_INTRO_SHOWN, true) }
        visible = false
    }
    val list = requiredPermissions().map { "• " + stringResource(it.title) }.joinToString("\n")
    AlertDialog(
        onDismissRequest = close,
        confirmButton = { TextButton(onClick = close) { Text(stringResource(R.string.intro_confirm)) } },
        title = { Text(stringResource(R.string.intro_title)) },
        text = { Text(stringResource(R.string.intro_text, list)) },
    )
}

@Preview(showBackground = true)
@Composable
private fun PermissionsContentPreview() {
    PingMeTheme {
        PermissionsContent(
            required = AppPermission.entries,
            missing = listOf(AppPermission.BATTERY, AppPermission.UNUSED_APP),
            onEnable = {},
        )
    }
}
