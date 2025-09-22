package com.jamjar.glyphtuner

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat.startForegroundService
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState

@Preview
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun MainScreen() {
    val context = LocalContext.current
    var showSettingsLink by remember { mutableStateOf(false) }
    var showLicenseDialog by remember { mutableStateOf(false) }

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
            showSettingsLink = false
        }
    }

    // Launch permission request if not all granted
    LaunchedEffect(multiplePermissionsState.allPermissionsGranted) {
        if (!multiplePermissionsState.allPermissionsGranted) {
            multiplePermissionsState.launchMultiplePermissionRequest()
        }
    }

    Scaffold { paddingValues ->
        Column(
            modifier = Modifier

                .padding(paddingValues)
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            Text(
                text = "Glyph Tuner",
                style = MaterialTheme.typography.headlineLarge,
                fontSize = 38.sp
            )
            Spacer(modifier = Modifier.height(30.dp))
            Card {
                Text(
                    text = "Glyph Tuner adds a Glyph Toy for tuning your guitar. The tuner will automatically detect which string you are tuning, or long press to tune a specific string.",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(16.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            when {
                multiplePermissionsState.allPermissionsGranted -> {
                    Text(
                        "Permissions granted! You're all set.",
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    launchForegroundService(context)
                }
                showSettingsLink -> {
                    val deniedPermissions = multiplePermissionsState.revokedPermissions
                    val rationaleText = buildString {
                        if (deniedPermissions.any { it.permission == Manifest.permission.RECORD_AUDIO }) {
                            append("Audio Recording: Required to detect your guitar's pitch.\n")
                        }
                        if (deniedPermissions.any { it.permission == Manifest.permission.POST_NOTIFICATIONS }) {
                            append("Post Notifications: Required to allow access to the microphone while using the Glyph.\n")
                        }
                        append("You can enable the missing permissions from app settings.")
                    }
                    Card {
                        Text(
                            text = "The following permissions were denied:",
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(16.dp)
                        )
                        Text(
                            text = rationaleText,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(16.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            modifier = Modifier
                                .padding(0.dp, 0.dp, 12.dp, 12.dp)
                                .align(Alignment.End),
                            onClick = { openAppSettings(context) },
                        ) {
                            Text("App Settings")
                        }
                    }
                }
                else -> {
                    Text(
                        "Granting permissions will enable all app features.",
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            Column(
                verticalArrangement = Arrangement.Bottom,
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxSize()
            ) {
                Image(
                    painter = painterResource(id = R.drawable.guitar_tuner_icon),
                    contentDescription = "Guitar Tuner Icon",
                    modifier = Modifier.fillMaxWidth(0.4f)
                )
                Spacer(modifier = Modifier.height(28.dp))
                // License Button
                Button(
                    onClick = { showLicenseDialog = true },
                ) {
                    Text("License Info")
                }
            }
            // License Dialog
            if (showLicenseDialog) {
                AlertDialog(
                    onDismissRequest = { showLicenseDialog = false },
                    title = { Text("GPLv3 License") },
                    text = {
                        Text(
                            "This app uses the TarsosDSP library licensed under the GNU General Public License v3 (GPLv3).\n\n" +
                                    "Copyright (C) 2010-2023 Joren Six\n\n" +
                                    "Full source code of this app is available at: https://github.com/taylorj8/glyph-tuner\n\n" +
                                    "This program is distributed WITHOUT ANY WARRANTY; see https://www.gnu.org/licenses/gpl-3.0.html for details."
                        )
                    },
                    confirmButton = {
                        TextButton(onClick = { showLicenseDialog = false }) {
                            Text("Close")
                        }
                    }
                )
            }
        }
    }
}

fun launchForegroundService(context: Context) {
    val serviceIntent = Intent(context, TunerForegroundService::class.java)
    startForegroundService(context, serviceIntent)
}


fun openAppSettings(context: Context) {
    val intent = Intent(
        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        Uri.fromParts("package", context.packageName, null)
    )
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    context.startActivity(intent)
}
