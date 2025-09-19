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
import be.tarsos.dsp.AudioDispatcher
import be.tarsos.dsp.AudioEvent
import be.tarsos.dsp.io.android.AudioDispatcherFactory
import be.tarsos.dsp.pitch.PitchDetectionHandler
import be.tarsos.dsp.pitch.PitchDetectionResult
import be.tarsos.dsp.pitch.PitchProcessor
import com.nothing.ketchum.GlyphToy


class GuitarTunerService : Service() {

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

        // Do your background work here (e.g., a thread, coroutine, or handler)

        return START_NOT_STICKY
    }

//    private var glyphMatrixManager: GlyphMatrixManager? = null
    private var glyphSprite: GlyphSprite? = null
    private var audioProcessor: AudioProcessor? = null

    override fun onBind(intent: Intent?): IBinder? {
        Log.d("test", "BIND")
        startTuner()
        return serviceMessenger.binder
    }

    private fun startTuner() {
        glyphSprite = GlyphSprite()
        glyphSprite?.init(applicationContext)
        audioProcessor = AudioProcessor()
        audioProcessor?.start()
        Handler(Looper.getMainLooper()).postDelayed({
            glyphSprite?.render(R.mipmap.music_icon)
        }, 1) // this delay fixes a crash don't question it

        Handler(Looper.getMainLooper()).postDelayed({
            stopTuner()
            Log.d("GlyphToy", "Audio processing stopped automatically after 30 seconds")
        }, 30000) // stop automatically after 30s
    }


    private fun stopTuner() {
        audioProcessor?.stop()
        audioProcessor = null
        glyphSprite?.unInit()
        glyphSprite = null
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
