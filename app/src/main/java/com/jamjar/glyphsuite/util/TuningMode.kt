package com.jamjar.glyphsuite.util

enum class TuningMode(val hz: Float?) {
    AUTO(null), LOW_E(82.41f), A(110f), D(146.83f), G(196f), B(246.94f), HIGH_E(329.63f);

    fun next(): TuningMode {
        val values = entries.toTypedArray()
        val nextOrdinal = (this.ordinal + 1) % values.size
        return values[nextOrdinal]
    }
}