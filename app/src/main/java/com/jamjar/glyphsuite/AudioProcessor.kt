package com.jamjar.glyphsuite

import android.os.Handler
import android.os.Looper
import android.util.Log
import be.tarsos.dsp.AudioDispatcher
import be.tarsos.dsp.AudioEvent
import be.tarsos.dsp.io.android.AudioDispatcherFactory
import be.tarsos.dsp.pitch.PitchDetectionHandler
import be.tarsos.dsp.pitch.PitchDetectionResult
import be.tarsos.dsp.pitch.PitchProcessor

class AudioProcessor {

    private var dispatcher: AudioDispatcher? = null

    fun start() {
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

        dispatcher?.addAudioProcessor(
            PitchProcessor(
                PitchProcessor.PitchEstimationAlgorithm.YIN,
                sampleRate.toFloat(),
                bufferSize,
                pdh
            )
        )

        Thread { dispatcher?.run() }.start()
    }

    fun stop() {
        dispatcher?.stop()
        dispatcher = null
    }
}