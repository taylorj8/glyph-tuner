package com.jamjar.glyphtuner

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Surface
import androidx.core.content.ContextCompat
import com.jamjar.glyphtuner.ui.theme.GlyphTunerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            GlyphTunerTheme {
                Surface {
                    MainScreen()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()

        val micPermission = Manifest.permission.RECORD_AUDIO
        val fgMicPermission = Manifest.permission.FOREGROUND_SERVICE_MICROPHONE

        if (ContextCompat.checkSelfPermission(this, micPermission) != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, fgMicPermission) != PackageManager.PERMISSION_GRANTED) {

            // Stop the foreground service if the required permission is revoked
            stopService(Intent(this, TunerForegroundService::class.java))
        }
    }
}
