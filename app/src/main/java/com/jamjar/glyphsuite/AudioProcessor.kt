package com.jamjar.glyphsuite

import android.util.Log
import be.tarsos.dsp.AudioDispatcher
import be.tarsos.dsp.AudioEvent
import be.tarsos.dsp.io.android.AudioDispatcherFactory
import be.tarsos.dsp.pitch.PitchDetectionHandler
import be.tarsos.dsp.pitch.PitchDetectionResult
import be.tarsos.dsp.pitch.PitchProcessor
import kotlin.math.abs

class AudioProcessor {

    private var dispatcher: AudioDispatcher? = null
    private var medianPitchProcessor = MedianPitchProcessor(windowSize = 10)

    private val frequencies = listOf(
        82.41f,
        110f,
        146.83f,
        196f,
        246.94f,
        329.63f
    )

    fun start() {
        val sampleRate = 44100
        val bufferSize = 2048
        val overlap = 0

        dispatcher = AudioDispatcherFactory.fromDefaultMicrophone(sampleRate, bufferSize, overlap)

        val pitchProcessor = PitchProcessor(
            PitchProcessor.PitchEstimationAlgorithm.YIN,
            sampleRate.toFloat(),
            bufferSize,
            medianPitchProcessor.getPitchHandler()
        )

        dispatcher?.addAudioProcessor(pitchProcessor)
        dispatcher?.addAudioProcessor(medianPitchProcessor)

        Thread { dispatcher?.run() }.start()
    }

    fun getClosestNote(): Float {
        var closestNote = frequencies.first()
        var minDiff = Float.MAX_VALUE
        for (note in frequencies) {
            val diff = abs(note - getCurrentPitch())
            if (diff < minDiff) {
                minDiff = diff
                closestNote = note
            }
        }
        return closestNote
    }

    fun getCurrentPitch(): Float {
        return medianPitchProcessor.run {
            try { getMedianPitch() } catch (e: Exception) { frequencies.first() }
        }
    }

    fun stop() {
        dispatcher?.stop()
        dispatcher = null
    }

}