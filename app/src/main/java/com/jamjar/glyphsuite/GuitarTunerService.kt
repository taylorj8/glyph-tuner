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

    private val CHANNEL_ID = "ForegroundServiceChannel"
    private var dispatcher: AudioDispatcher? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Foreground Service")
            .setContentText("Service is running...")
            .setSmallIcon(R.drawable.ic_launcher_foreground) // replace with your icon
            .build()

        startForeground(1, notification)

        // Do your background work here (e.g., a thread, coroutine, or handler)

        return START_NOT_STICKY
    }

//    private var glyphMatrixManager: GlyphMatrixManager? = null
    private val glyphSprite = GlyphSprite()

    override fun onBind(intent: Intent?): IBinder? {
        Log.d("test", "BIND")
        startTuner()
        return serviceMessenger.binder
    }

    private fun startTuner() {
        glyphSprite.init(applicationContext)
        glyphSprite.render(R.mipmap.music_icon)
        startAudioProcessing()
    }


    private fun stopTuner() {
        // Stop audio processing first
        dispatcher?.stop()
        dispatcher = null

        // Clean up Glyph Matrix
        glyphSprite.unInit()
    }

    private val serviceHandler: Handler = object : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            when (msg.what) {
                GlyphToy.MSG_GLYPH_TOY -> {
                    val bundle: Bundle = msg.getData()
                    val event = bundle.getString(GlyphToy.MSG_GLYPH_TOY_DATA)
                    when (event) {
                        GlyphToy.EVENT_ACTION_UP -> startTuner()
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
            CHANNEL_ID,
            "Foreground Service Channel",
            NotificationManager.IMPORTANCE_LOW
        )
        val manager = getSystemService(NotificationManager::class.java)
        manager?.createNotificationChannel(serviceChannel)
    }

    private fun startAudioProcessing() {
        val sampleRate = 44100
        val bufferSize = 2048
        val overlap = 0

        dispatcher = AudioDispatcherFactory.fromDefaultMicrophone(sampleRate, bufferSize, overlap)

        val pdh = PitchDetectionHandler { res: PitchDetectionResult, _: AudioEvent ->
            val pitchHz = res.pitch
            if (pitchHz > 0) {
                Log.d("GlyphToy", "Detected pitch: $pitchHz Hz")
                // TODO: map pitch to guitar string and update GlyphMatrix
            }
        }

        dispatcher?.addAudioProcessor(PitchProcessor(
            PitchProcessor.PitchEstimationAlgorithm.YIN,
            sampleRate.toFloat(),
            bufferSize,
            pdh
        ))

        Thread { dispatcher?.run() }.start()

        Handler(Looper.getMainLooper()).postDelayed({
            stopTuner()
            Log.d("GlyphToy", "Audio processing stopped automatically after 30 seconds")
        }, 5000) // stop automatically after 30s
    }

}
