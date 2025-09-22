package com.jamjar.glyphtuner

import GlyphSprite
import android.app.Service
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.Message
import android.os.Messenger
import android.util.Log
import com.jamjar.glyphtuner.util.TuningMode
import com.nothing.ketchum.GlyphToy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.ln
import kotlin.math.roundToInt


class GuitarTuner : Service() {

    private var backgroundScope: CoroutineScope? = null
    private var glyphSprite: GlyphSprite? = null
    private var audioProcessor: GlyphAudioProcessor? = null
    private var tuningMode: TuningMode = TuningMode.AUTO

    override fun onBind(intent: Intent?): IBinder {
        startTuner()
        return serviceMessenger.binder
    }

    override fun onUnbind(intent: Intent?): Boolean {
        stopTuner()
        return false
    }

    private fun startTuner() {
        audioProcessor = GlyphAudioProcessor().apply {
            start()
        }
        glyphSprite = GlyphSprite().apply {
            init(applicationContext)
        }
        backgroundScope = CoroutineScope(Dispatchers.Main)

        mainLoop()
    }

    private fun stopTuner() {
//        Log.d("test", "TUNER STOPPING")
        audioProcessor?.stop()
        audioProcessor = null
        glyphSprite?.unInit()
        glyphSprite = null
        backgroundScope?.cancel()
        backgroundScope = null
    }

    private fun mainLoop() {
        backgroundScope?.launch {
            while (isActive) {
                val currentFreq = audioProcessor!!.getCurrentPitch()
//                Log.d("", currentFreq.toString())

                val targetNote = when (tuningMode) {
                    TuningMode.AUTO -> audioProcessor!!.getClosestNote()
                    else -> tuningMode.hz!!
                }

                val cents = pitchDifferenceInCents(currentFreq, targetNote)
                val offset = centsToOffset(cents)

                val noteRes = when (targetNote) {
                    82.41f -> R.drawable.overlay_elow
                    110f -> R.drawable.overlay_a
                    146.83f -> R.drawable.overlay_d
                    196f -> R.drawable.overlay_g
                    246.94f -> R.drawable.overlay_b
                    else -> R.drawable.overlay_ehigh
                }

                glyphSprite!!.renderTuner(R.drawable.background, noteRes, cents, offset, tuningMode)
                delay(30)
            }
        }
    }

    private val serviceHandler: Handler = object : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            when (msg.what) {
                GlyphToy.MSG_GLYPH_TOY -> {
                    val bundle: Bundle = msg.data
                    val event = bundle.getString(GlyphToy.MSG_GLYPH_TOY_DATA)
                    when (event) {
                        GlyphToy.EVENT_CHANGE -> tuningMode = tuningMode.next()
                    }
                }

                else -> super.handleMessage(msg)
            }
        }
    }
    private val serviceMessenger: Messenger = Messenger(serviceHandler)

    private fun pitchDifferenceInCents(currentHz: Float, targetHz: Float): Float {
        if (currentHz <= 0.0 || targetHz <= 0.0) return 0.0f // treat no signal as neutral
        return (1200.0 * (ln(currentHz / targetHz) / ln(2.0))).toFloat()
    }

    private fun centsToOffset(cents: Float, maxCents: Float = 50.0f, maxOffset: Int = 12): Int {
        val normalized = (cents / maxCents).coerceIn(-1.0f, 1.0f)
        return (normalized * maxOffset).roundToInt()
    }
}
