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
    private var pitchHz = 0f
    private var pitchBuffer: ArrayDeque<Float>? = null
    private val frequencies = listOf(
        82.41f,
        110f,
        146.83f,
        196f,
        246.94f,
        329.63f
    )

    fun start() {
        pitchBuffer = ArrayDeque(10)
        repeat(10) {
            enqueuePitch(82.41f) // todo better solution here
        }

        val sampleRate = 44100
        val bufferSize = 2048
        val overlap = 0

        dispatcher = AudioDispatcherFactory.fromDefaultMicrophone(sampleRate, bufferSize, overlap)

        val pdh = PitchDetectionHandler { res: PitchDetectionResult, _: AudioEvent ->
            if (res.pitch > 0) {
                pitchHz = res.pitch
                enqueuePitch(pitchHz)
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

    fun getClosestNote(): Float {
        var lowestDiff = Float.MAX_VALUE
        var closestNote = 82.41f
        val averagedPitch = robustAverage()
        frequencies.forEach { note ->
            val diff = abs(note - averagedPitch)
            if (diff < lowestDiff) {
                lowestDiff = diff
                closestNote = note
            }
        }
        return closestNote
    }

    fun robustAverage(bufferSize: Int = 10, tolerance: Float = 3f): Float {
        try { // todo - figure out cause of crash
            require(pitchBuffer?.size == bufferSize) { "List must contain exactly 5 numbers" }

            val sorted = pitchBuffer!!.sorted()
            var bestWindow: List<Float> = emptyList()

            for (windowSize in bufferSize downTo bufferSize-4) {
                for (start in 0..(sorted.size - windowSize)) {
                    val window = sorted.subList(start, start + windowSize)
                    if (window.last() - window.first() <= tolerance) {
                        bestWindow = window
                        break
                    }
                }
                if (bestWindow.isNotEmpty()) break
            }

            if (bestWindow.average().toFloat().isNaN()) {
                return 82.41f // todo just for testing
            }

            return bestWindow.average().toFloat()
        } catch (e: Exception) {
            e.message?.let { Log.d("crash", it) }
            return 82.41f
        }
    }

    fun stop() {
        dispatcher?.stop()
        dispatcher = null
    }

    private fun enqueuePitch(pitch: Float) {
        if (pitchBuffer!!.size == 10) {
            pitchBuffer!!.removeFirst()
        }
        pitchBuffer!!.addLast(pitch)
    }
}