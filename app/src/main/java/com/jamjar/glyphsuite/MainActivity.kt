package com.jamjar.glyphsuite

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Surface
import com.jamjar.glyphsuite.ui.theme.GlyphSuiteTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            GlyphSuiteTheme {
                Surface {
                    MainScreen()
                }
            }
        }
        val serviceIntent = Intent(this, TunerForegroundService::class.java)
        startForegroundService(serviceIntent)
    }
}
