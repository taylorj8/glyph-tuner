package com.jamjar.glyphtuner

import be.tarsos.dsp.AudioEvent
import be.tarsos.dsp.AudioProcessor
import be.tarsos.dsp.pitch.PitchDetectionHandler
import java.util.*
import kotlin.math.abs
import kotlin.math.ln

class MedianPitchProcessor(private val windowSize: Int = 10) : AudioProcessor {
    private val pitchQueue = ArrayDeque<Float>(windowSize)
    private val pitchHandler = PitchDetectionHandler { result, _ ->
        val pitch = result.pitch
        if (pitch != -1f) {
            enqueuePitch(pitch)
        }
    }

    private fun enqueuePitch(pitch: Float) {
        if (pitchQueue.size == windowSize) {
            pitchQueue.removeFirst()
        }
        pitchQueue.addLast(pitch)
    }

    fun getMedianPitch(): Float {
        val sortedPitches = pitchQueue.sorted()
        val medianPitch = sortedPitches[pitchQueue.size / 2]
        return correctHarmonicError(medianPitch)
    }

    override fun process(audioEvent: AudioEvent): Boolean {
        return true
    }

    override fun processingFinished() {}

    fun getPitchHandler(): PitchDetectionHandler = pitchHandler

    private fun correctHarmonicError(detectedFreq: Float): Float {
        val standardFrequencies = listOf(82.41f, 110f, 146.83f, 196f, 246.94f, 329.63f)
        var bestFreq = standardFrequencies.first()
        var minCentsDiff = Float.MAX_VALUE

        val candidates = listOf(detectedFreq, detectedFreq / 2, detectedFreq * 2)
        for (c in candidates) {
            for (f in standardFrequencies) {
                val centsDiff = 1200 * (ln(f / c) / ln(2.0)).toFloat()
                if (abs(centsDiff) < minCentsDiff) {
                    minCentsDiff = abs(centsDiff)
                    bestFreq = c
                }
            }
        }

        return bestFreq
    }
}
