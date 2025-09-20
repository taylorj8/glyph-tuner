package com.jamjar.glyphsuite

import be.tarsos.dsp.AudioEvent
import be.tarsos.dsp.AudioProcessor
import be.tarsos.dsp.pitch.PitchDetectionResult
import be.tarsos.dsp.pitch.PitchDetectionHandler
import java.util.*

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
        return sortedPitches[pitchQueue.size / 2]
    }

    override fun process(audioEvent: AudioEvent): Boolean {
        return true
    }

    override fun processingFinished() {}

    fun getPitchHandler(): PitchDetectionHandler = pitchHandler
}
