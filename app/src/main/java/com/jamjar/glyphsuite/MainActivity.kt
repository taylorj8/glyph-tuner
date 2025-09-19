package com.jamjar.glyphsuite

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat

class MainActivity : ComponentActivity() {

    private lateinit var requestPermissionsLauncher: ActivityResultLauncher<Array<String>>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Create notification channel (needed for foreground service notifications)
        createNotificationChannel()

        // Initialize the launcher for requesting permissions
        requestPermissionsLauncher = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            if (permissions[Manifest.permission.RECORD_AUDIO] == true &&
                permissions[Manifest.permission.POST_NOTIFICATIONS] == true) {
                // Permissions granted – Nothing OS will bind your service automatically
//                finish() // optional: close the activity
            } else {
                // Handle the case where permissions are denied
                // e.g., show a dialog or guide the user to settings
            }
        }

        // Check if both permissions are granted
        when {
            ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED -> {
                // Permissions already granted – nothing else to do
//                finish()
            }
            else -> {
                // Request the permissions
                requestPermissionsLauncher.launch(
                    arrayOf(
                        Manifest.permission.RECORD_AUDIO,
                        Manifest.permission.POST_NOTIFICATIONS
                    )
                )
            }
        }

        val serviceIntent = Intent(this, GuitarTunerService::class.java)
        startForegroundService(serviceIntent)

    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            "guitar_tuner_channel",
            "Glyph Toy Channel",
            NotificationManager.IMPORTANCE_LOW
        )
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }
}
