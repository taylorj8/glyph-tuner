package com.jamjar.glyphsuite

import android.content.Context
import com.nothing.ketchum.GlyphMatrixManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.*

class AnimationEngine {

    @Volatile private var currentFreqHz: Double = 0.0
    @Volatile private var targetFreqHz: Double = 0.0

    // smoothed cents to reduce jitter
    private var smoothedCents = 0.0
    private val smoothingAlpha = 0.25 // tune between 0.05 (very smooth) and 0.5 (responsive)

    private var frame = 0

    // Call these from your pitch detector/update path
    fun setTargetFrequency(fHz: Double) {
        targetFreqHz = fHz
    }

    fun setCurrentFrequency(fHz: Double) {
        currentFreqHz = fHz
    }

    fun generateTuningFrameForPitch(
        currentHz: Double,
        targetHz: Double
    ): IntArray {
        val grid = IntArray(WIDTH * HEIGHT) { 0 }

        if (currentHz <= 0.0 || targetHz <= 0.0) {
            // nothing to show
            return grid
        }

        // 1) compute cents
        val cents = 1200.0 * (ln(currentHz / targetHz) / ln(2.0))

        // 2) smooth
        smoothedCents = smoothingAlpha * cents + (1.0 - smoothingAlpha) * smoothedCents

        // 3) map cents -> horizontal offset
        val maxCentsForFullWidth = 50.0 // ±50 cents maps to full left/right visible range
        val normalized = (smoothedCents / maxCentsForFullWidth).coerceIn(-1.0, 1.0)
        val centerX = WIDTH / 2
        val availableHalf = (WIDTH / 2) - 1
        val needleX = (centerX + (normalized * availableHalf)).roundToInt().coerceIn(0, WIDTH - 1)

        // 4) vertical needle with Gaussian cross-section (so it's visible on low res)
        val needlePeak = 255
        val needleWidth = 1.6 // subpixel width for the Gaussian
        for (r in 0 until HEIGHT) {
            for (c in 0 until WIDTH) {
                // distance from column center
                val dx = (c - needleX).toDouble()
                // small gaussian across columns to make the needle thicker and readable
                val g = exp(- (dx * dx) / (2.0 * needleWidth * needleWidth))
                // base brightness depends on row position to create slight taper effect
                // center row slightly brighter so the eye focuses center
                val rowCenter = HEIGHT / 2.0
                val dy = (r - rowCenter) / (HEIGHT / 2.0)
                val rowFactor = (1.0 - abs(dy)) // 1.0 at middle row, down to 0 at top/bottom
                val brightness = (needlePeak * g * (0.6 + 0.4 * rowFactor)).roundToInt()
                val idx = r * WIDTH + c
                grid[idx] = grid[idx].coerceAtLeast(brightness)
            }
        }

        // 5) stability indicator: if very close (±threshold), draw a strong center marker
        val stableThresholdCents = 5.0
        if (abs(smoothedCents) <= stableThresholdCents) {
            val midRow = HEIGHT / 2
            val midCol = WIDTH / 2
            // draw a small bright cross in the center
            for (dr in -1..1) {
                for (dc in -1..1) {
                    val r = (midRow + dr).coerceIn(0, HEIGHT - 1)
                    val c = (midCol + dc).coerceIn(0, WIDTH - 1)
                    grid[r * WIDTH + c] = 255
                }
            }
        }

        // 6) add a bottom "bar" showing magnitude of deviation (visual gauge)
        val magnitude = (abs(smoothedCents) / maxCentsForFullWidth).coerceIn(0.0, 1.0)
        val barLength = (magnitude * (WIDTH - 2)).roundToInt()
        val barRow = HEIGHT - 2
        val barStart = 1
        val barEnd = barStart + barLength
        for (c in barStart until barEnd.coerceAtMost(WIDTH - 1)) {
            grid[barRow * WIDTH + c] = 200
        }

        // 7) optional subtle strobe: uses beat-rate impression — faster shimmer when off
        // This helps human ear/eye notice beating; we modulate a low-bright pulse across rows
        val beatRate = (abs(smoothedCents) / 5.0).coerceIn(0.0, 10.0)  // arbitrary scale
        val shimmer = (sin(frame * 0.12 * (1.0 + beatRate)) * 0.5 + 0.5) // 0..1
        if (shimmer > 0.85) {
            // small top row blink
            for (c in 0 until WIDTH step 2) {
                grid[0 * WIDTH + c] = grid[0 * WIDTH + c].coerceAtLeast(100)
            }
        }

        frame = (frame + 1) % 1000000
        return grid
    }

    fun pitchToOffset(
        currentHz: Float,
        targetHz: Float,
        maxCents: Float = 50.0f,
        maxOffset: Int = 12
    ): Int {
        if (currentHz <= 0.0 || targetHz <= 0.0) return 0 // treat no signal as neutral

        // Difference in cents (positive if sharp, negative if flat)
        val cents = 1200.0 * (ln(currentHz / targetHz) / ln(2.0))

        // Normalize to -1.0..+1.0, then scale to offset range
        val normalized = (cents / maxCents).coerceIn(-1.0, 1.0)
        val offset = (normalized * maxOffset).roundToInt()

        return offset
    }

    private companion object {
        private const val WIDTH = 25
        private const val HEIGHT = 25
    }
}
