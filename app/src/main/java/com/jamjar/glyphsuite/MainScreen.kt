package com.jamjar.glyphsuite

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.accompanist.permissions.*

@Preview
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun MainScreen() {
    val context = LocalContext.current
    var showSettingsLink by remember { mutableStateOf(false) }
    var rationaleState by remember { mutableStateOf<PermissionRationaleState?>(null) }

    val permissionsToRequest = listOf(
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.POST_NOTIFICATIONS
    )

    val multiplePermissionsState = rememberMultiplePermissionsState(
        permissions = permissionsToRequest
    ) { permissionsResult ->
        val anyDenied = permissionsResult.values.any { !it }

        // Determine if we should show a settings link
        // We cannot access multiplePermissionsState inside this callback
        showSettingsLink = anyDenied
                && permissionsResult.values.any { !it } // This is sufficient to indicate permanent denial
        if (!anyDenied) {
            rationaleState = null
            showSettingsLink = false
        }
    }

    // Launch permission request if not all granted
    LaunchedEffect(multiplePermissionsState.allPermissionsGranted) {
        if (!multiplePermissionsState.allPermissionsGranted) {
            multiplePermissionsState.launchMultiplePermissionRequest()
        }
    }

    Scaffold{ paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            Text(
                text = "Glyph Tuner",
                style = MaterialTheme.typography.headlineLarge
            )
            Spacer(modifier = Modifier.height(24.dp))
            Card{
                Text(
                    text = "This application enhances your device experience by utilizing audio analysis and providing timely notifications. To fully function, we need a couple of permissions.",
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(16.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            when {
                multiplePermissionsState.allPermissionsGranted -> {
                    Text(
                        "All permissions granted! You're all set.",
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    // TODO: Add main app content here
                }
                showSettingsLink -> {
                    Spacer(modifier = Modifier.height(20.dp))
                    Text(
                        "Permissions were denied. To enable them, please go to app settings.",
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = { openAppSettings(context) }) {
                        Text("Open App Settings")
                    }
                }
                multiplePermissionsState.shouldShowRationale -> {
                    val deniedPermissions = multiplePermissionsState.revokedPermissions
                    LaunchedEffect(deniedPermissions) {
                        val rationaleText = buildString {
                            if (deniedPermissions.any { it.permission == Manifest.permission.RECORD_AUDIO }) {
                                append("Audio Recording: Needed to analyze sound for glyph patterns.\n\n")
                            }
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                deniedPermissions.any { it.permission == Manifest.permission.POST_NOTIFICATIONS }) {
                                append("Post Notifications: Required to inform you about detected glyph events or app status.")
                            }
                        }
                        rationaleState = PermissionRationaleState(
                            title = "Permissions Required",
                            text = rationaleText.trim(),
                            confirmButtonText = "Grant Permissions",
                            onConfirm = {
                                rationaleState = null
                                multiplePermissionsState.launchMultiplePermissionRequest()
                            }
                        )
                    }
                }
                else -> {
                    Text(
                        "Granting permissions will enable all app features.",
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = { multiplePermissionsState.launchMultiplePermissionRequest() }) {
                        Text("Request Permissions Again")
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

fun openAppSettings(context: Context) {
    val intent = Intent(
        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        Uri.fromParts("package", context.packageName, null)
    )
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    context.startActivity(intent)
}

data class PermissionRationaleState(
    val title: String,
    val text: String,
    val confirmButtonText: String,
    val onConfirm: () -> Unit
)

@Composable
fun PermissionRationaleDialog(
    rationaleState: PermissionRationaleState,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(rationaleState.title) },
        text = { Text(rationaleState.text) },
        confirmButton = {
            TextButton(onClick = rationaleState.onConfirm) {
                Text(rationaleState.confirmButtonText)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
