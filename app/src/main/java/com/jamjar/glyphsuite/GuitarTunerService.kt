package com.jamjar.glyphsuite

import GlyphSprite
import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.ln
import kotlin.math.roundToInt


class GuitarTunerService : Service() {

    private var backgroundScope: CoroutineScope? = null
    private var glyphSprite: GlyphSprite? = null
    private var audioProcessor: AudioProcessor? = null

//    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
//        Log.d("test", "START")
//        createNotificationChannel()
//        val notification: Notification = NotificationCompat.Builder(this, "ForegroundServiceChannel")
//            .setContentTitle("Foreground Service")
//            .setContentText("Service is running...")
//            .setSmallIcon(R.drawable.ic_launcher_foreground) // replace with your icon
//            .build()
//
//        startForeground(1, notification)
//
//
//        return START_STICKY
//    }

    override fun onBind(intent: Intent?): IBinder {
        Log.d("test", "BIND")
        startTuner()
        return Binder()
    }

    override fun onUnbind(intent: Intent?): Boolean {
        Log.d("test", "UNBIND")
        stopTuner()
        return false
    }

    private fun startTuner() {
        Log.d("test", "TUNER STARTING")
        audioProcessor = AudioProcessor().apply {
            start()
        }
        glyphSprite = GlyphSprite().apply {
            init(applicationContext)
        }
        backgroundScope = CoroutineScope(Dispatchers.Main)

        mainLoop()
    }

    private fun mainLoop() {
        backgroundScope?.launch {
            while (isActive) {
                val currentFreq = audioProcessor!!.getCurrentPitch()
                val closestNote = audioProcessor!!.getClosestNote()
                val cents = pitchDifferenceInCents(currentFreq, closestNote)
                val offset = centsToOffset(cents)

                val noteRes = when (closestNote) {
                    110f -> R.drawable.overlay_a
                    146.83f -> R.drawable.overlay_d
                    196f -> R.drawable.overlay_g
                    246.94f -> R.drawable.overlay_b
                    else -> R.drawable.overlay_e
                }

                glyphSprite!!.renderTuner(R.drawable.background, noteRes, cents.roundToInt(), offset)
                delay(50)
            }
        }
    }

    private fun stopTuner() {
        Log.d("test", "TUNER STOPPING")
        audioProcessor?.stop()
        audioProcessor = null
        glyphSprite?.unInit()
        glyphSprite = null
        backgroundScope?.cancel()
        backgroundScope = null
    }

    private fun pitchDifferenceInCents(currentHz: Float, targetHz: Float): Float {
        if (currentHz <= 0.0 || targetHz <= 0.0) return 0.0f // treat no signal as neutral
        return (1200.0 * (ln(currentHz / targetHz) / ln(2.0))).toFloat()
    }

    private fun centsToOffset(cents: Float, maxCents: Float = 50.0f, maxOffset: Int = 12): Int {
        val normalized = (cents / maxCents).coerceIn(-1.0f, 1.0f)
        return (normalized * maxOffset).roundToInt()
    }
}
