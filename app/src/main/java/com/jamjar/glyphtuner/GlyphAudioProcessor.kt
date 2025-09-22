package com.jamjar.glyphtuner

import be.tarsos.dsp.AudioDispatcher
import be.tarsos.dsp.AudioEvent
import be.tarsos.dsp.AudioProcessor
import be.tarsos.dsp.filters.LowPassFS
import be.tarsos.dsp.io.android.AudioDispatcherFactory
import be.tarsos.dsp.pitch.PitchProcessor
import kotlin.math.abs
import kotlin.math.sqrt

class GlyphAudioProcessor {

    private var dispatcher: AudioDispatcher? = null
    private var medianPitchProcessor = MedianPitchProcessor(windowSize = 12)

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
        val bufferSize = 4096
        val overlap = 1024

        dispatcher = AudioDispatcherFactory.fromDefaultMicrophone(sampleRate, bufferSize, overlap)

        val lowPassCutoff = 1500f
        val lowPassFilter = LowPassFS(lowPassCutoff, sampleRate.toFloat())

        val pitchProcessor = PitchProcessor(
            PitchProcessor.PitchEstimationAlgorithm.MPM,
            sampleRate.toFloat(),
            bufferSize,
            medianPitchProcessor.getPitchHandler()
        )

        val rmsProcessor = object : AudioProcessor {
            override fun process(audioEvent: AudioEvent): Boolean {
                val buffer = audioEvent.floatBuffer
                var sum = 0f
                for (s in buffer) {
                    sum += s * s
                }
                val rms = sqrt(sum / buffer.size)

                val rmsThreshold = 0.001f
                return if (rms < rmsThreshold) {
                    // Too quiet: skip pitch detection by not passing audio forward
                    // Optionally, report silence to your handler here
                    false
                } else {
                    // Enough energy: continue to pitch detection
                    true
                }
            }

            override fun processingFinished() {}
        }

        dispatcher?.addAudioProcessor(lowPassFilter)
        dispatcher?.addAudioProcessor(rmsProcessor)
        dispatcher?.addAudioProcessor(pitchProcessor)

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
            try { getMedianPitch() } catch (_: Exception) { frequencies.first() }
        }
    }

    fun stop() {
        dispatcher?.stop()
        dispatcher = null
    }

}