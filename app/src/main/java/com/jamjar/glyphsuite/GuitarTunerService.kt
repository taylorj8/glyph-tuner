package com.jamjar.glyphsuite

import GlyphSprite
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.Message
import android.os.Messenger
import android.util.Log
import androidx.core.app.NotificationCompat
import com.nothing.ketchum.GlyphToy
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
    private var animationEngine: AnimationEngine? = null

    private val notes = mapOf(
        82.41f to R.mipmap.note_e,
        110f to R.mipmap.note_a,
        146.83f to R.mipmap.note_d,
        196f to R.mipmap.note_g,
        246.94f to R.mipmap.note_b,
        329.63f to R.mipmap.note_e
    )

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification: Notification = NotificationCompat.Builder(this, "ForegroundServiceChannel")
            .setContentTitle("Foreground Service")
            .setContentText("Service is running...")
            .setSmallIcon(R.drawable.ic_launcher_foreground) // replace with your icon
            .build()

        startForeground(1, notification)

        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        Log.d("test", "BIND")
        startTuner()
        return serviceMessenger.binder
    }

    private fun startTuner() {
        audioProcessor = AudioProcessor().apply {
            start()
        }
        glyphSprite = GlyphSprite().apply {
            init(applicationContext)
            Handler(Looper.getMainLooper()).postDelayed({
                render(R.mipmap.music_icon)
            }, 10) // this delay fixes a crash don't question it
        }
        backgroundScope = CoroutineScope(Dispatchers.Main)
        animationEngine = AnimationEngine()

        mainLoop()

//        Handler(Looper.getMainLooper()).postDelayed({
//            stopTuner()
//            Log.d("GlyphToy", "Audio processing stopped automatically after 30 seconds")
//        }, 30000) // stop automatically after 30s
    }

    private fun mainLoop() {
        backgroundScope?.launch {
            while (isActive) {
                val closestNote = audioProcessor!!.getClosestNote()
                val currentFreq = audioProcessor!!.robustAverage()
//                val array = animationEngine!!.generateTuningFrameForPitch(currentFreq.toDouble(), closestNote.toDouble())
                val offset = animationEngine!!.pitchToOffset(currentFreq, closestNote)

                val noteRes = when (closestNote) {
                    110f -> R.drawable.overlay_a
                    146.83f -> R.drawable.overlay_d
                    196f -> R.drawable.overlay_g
                    246.94f -> R.drawable.overlay_b
                    else -> R.drawable.overlay_e
                }

                glyphSprite!!.renderTuner(R.drawable.background, noteRes, offset)
                delay(50)
            }
        }
    }


    private fun stopTuner() {
        audioProcessor?.stop()
        audioProcessor = null
        glyphSprite?.unInit()
        glyphSprite = null
        backgroundScope?.cancel()
        backgroundScope = null
        animationEngine = null
    }

    private val serviceHandler: Handler = object : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            when (msg.what) {
                GlyphToy.MSG_GLYPH_TOY -> {
                    val bundle: Bundle = msg.getData()
                    val event = bundle.getString(GlyphToy.MSG_GLYPH_TOY_DATA)
                    when (event) {
                        GlyphToy.EVENT_ACTION_UP -> {
                            startTuner()
                            glyphSprite?.render(R.mipmap.music_icon)
                        }
                        GlyphToy.EVENT_ACTION_DOWN -> stopTuner()
                    }
                }

                else -> super.handleMessage(msg)
            }
        }
    }
    private val serviceMessenger: Messenger = Messenger(serviceHandler)

    private fun createNotificationChannel() {
        val serviceChannel = NotificationChannel(
            "ForegroundServiceChannel",
            "Foreground Service Channel",
            NotificationManager.IMPORTANCE_LOW
        )
        val manager = getSystemService(NotificationManager::class.java)
        manager?.createNotificationChannel(serviceChannel)
    }

    fun pitchToOffset(
        currentHz: Float,
        targetHz: Float,
        maxCents: Float = 50.0f,
        maxOffset: Int = 12
    ): Int {
        if (currentHz <= 0.0 || targetHz <= 0.0) return 0 // treat no signal as neutral

        // Difference in cents (positive if sharp, negative if flat)
        val cents = 1200.0 * (ln(currentHz / targetHz) / ln(2.0))

        // Normalize to -1.0..+1.0, then scale to offset range
        val normalized = (cents / maxCents).coerceIn(-1.0, 1.0)
        val offset = (normalized * maxOffset).roundToInt()

        return offset
    }

}
