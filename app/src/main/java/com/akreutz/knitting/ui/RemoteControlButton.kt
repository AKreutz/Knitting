package com.akreutz.knitting.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SettingsRemote
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import com.akreutz.knitting.R
import com.akreutz.knitting.watch.WatchSettings
import com.akreutz.knitting.watch.WatchStatus
import com.akreutz.knitting.watch.labelRes
import com.akreutz.knitting.web.WebConnection
import com.akreutz.knitting.web.WebSettings
import com.akreutz.knitting.web.WebStatus

/** Top bar menu for letting a Garmin watch and a browser on the same network control the counters. */
@Composable
fun RemoteControlButton() {
    val context = LocalContext.current
    var expanded by remember { mutableStateOf(false) }
    var enabled by remember { mutableStateOf(WatchSettings.isEnabled(context) || WebSettings.isEnabled(context)) }
    var pin by remember { mutableStateOf(WebSettings.pin(context)) }
    val watch by WatchStatus.connection.collectAsState()
    val web by WebStatus.connection.collectAsState()
    // The services run either way; without the permission their notifications are just hidden.
    val askForNotifications = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}

    fun toggle() {
        enabled = !enabled
        WatchSettings.setEnabled(context, enabled)
        WebSettings.setEnabled(context, enabled)
        if (enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            askForNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(Icons.Filled.SettingsRemote, contentDescription = stringResource(R.string.remote))
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = {
                    Column {
                        Text(stringResource(R.string.remote_control))
                        if (enabled) {
                            Text(
                                stringResource(watch.labelRes()),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                },
                trailingIcon = { Switch(checked = enabled, onCheckedChange = null) },
                onClick = ::toggle,
            )
            if (enabled) {
                // Looked up each time the menu opens, as the phone may have joined another network since.
                val address = remember(expanded) { WebSettings.localAddress() }
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(
                                when {
                                    web == WebConnection.Error -> stringResource(R.string.web_status_error)
                                    address == null -> stringResource(R.string.web_no_network)
                                    else -> "http://$address:${WebSettings.PORT}"
                                },
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            Text(
                                stringResource(R.string.web_pin, pin),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    },
                    onClick = {},
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.web_new_pin)) },
                    onClick = { pin = WebSettings.newPin(context) },
                )
            }
        }
    }
}
