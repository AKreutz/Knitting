package com.akreutz.knitting.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Watch
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

/** Top bar menu for letting a Garmin watch control the counters. */
@Composable
fun WatchButton() {
    val context = LocalContext.current
    var expanded by remember { mutableStateOf(false) }
    var enabled by remember { mutableStateOf(WatchSettings.isEnabled(context)) }
    val connection by WatchStatus.connection.collectAsState()
    // The service runs either way; without the permission its notification is just hidden.
    val askForNotifications = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}

    fun toggle() {
        enabled = !enabled
        WatchSettings.setEnabled(context, enabled)
        if (enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            askForNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(Icons.Filled.Watch, contentDescription = stringResource(R.string.watch))
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = {
                    Column {
                        Text(stringResource(R.string.watch_control))
                        if (enabled) {
                            Text(
                                stringResource(connection.labelRes()),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                },
                trailingIcon = { Switch(checked = enabled, onCheckedChange = null) },
                onClick = ::toggle,
            )
        }
    }
}
