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
//        backgroundScope?.launch {
//            while(isActive) {
//                val closestNote = audioProcessor!!.getClosestNote()
//                glyphSprite!!.render(notes[closestNote]!!)
//                delay(100)
//            }
//        }
        backgroundScope?.launch {
            while (isActive) {
                val closestNote = audioProcessor!!.getClosestNote()
                val currentFreq = audioProcessor!!.robustAverage()
//                val array = animationEngine!!.generateTuningFrameForPitch(currentFreq.toDouble(), closestNote.toDouble())
                val i = animationEngine!!.pitchToImageIndex(currentFreq, closestNote)

                val frame = when (closestNote) {
                    110f -> {
                        if (currentFreq > closestNote) {
                            listOf(R.drawable.a_intune, R.drawable.a_down4, R.drawable.a_down3, R.drawable.a_down2, R.drawable.a_down1, R.drawable.a_down0)[i]
                        } else {
                            listOf(R.drawable.a_intune, R.drawable.a_up4, R.drawable.a_up3, R.drawable.a_up2, R.drawable.a_up1, R.drawable.a_up0)[i]
                        }
                    }
                    146.83f -> {
                        if (currentFreq > closestNote) {
                            listOf(R.drawable.d_intune, R.drawable.d_down4, R.drawable.d_down3, R.drawable.d_down2, R.drawable.d_down1, R.drawable.d_down0)[i]
                        } else {
                            listOf(R.drawable.d_intune, R.drawable.d_up4, R.drawable.d_up3, R.drawable.d_up2, R.drawable.d_up1, R.drawable.d_up0)[i]
                        }
                    }
                    196f -> {
                        if (currentFreq > closestNote) {
                            listOf(R.drawable.g_intune, R.drawable.g_down4, R.drawable.g_down3, R.drawable.g_down2, R.drawable.g_down1, R.drawable.g_down0)[i]
                        } else {
                            listOf(R.drawable.g_intune, R.drawable.g_up4, R.drawable.g_up3, R.drawable.g_up2, R.drawable.g_up1, R.drawable.g_up0)[i]
                        }
                    }
                    246.94f -> {
                        if (currentFreq > closestNote) {
                            listOf(R.drawable.b_intune, R.drawable.b_down4, R.drawable.b_down3, R.drawable.b_down2, R.drawable.b_down1, R.drawable.b_down0)[i]
                        } else {
                            listOf(R.drawable.b_intune, R.drawable.b_up4, R.drawable.b_up3, R.drawable.b_up2, R.drawable.b_up1, R.drawable.b_up0)[i]
                        }
                    }
                    else -> {
                        if (currentFreq > closestNote) {
                            listOf(R.drawable.e_intune, R.drawable.e_down4, R.drawable.e_down3, R.drawable.e_down2, R.drawable.e_down1, R.drawable.e_down0)[i]
                        } else {
                            listOf(R.drawable.e_intune, R.drawable.e_up4, R.drawable.e_up3, R.drawable.e_up2, R.drawable.e_up1, R.drawable.e_up0)[i]
                        }
                    }
                }


                glyphSprite!!.render(frame)
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

}
