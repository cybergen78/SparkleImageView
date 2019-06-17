package de.jarosz.sparkle.wave

import kotlin.math.asin
import kotlin.math.sin

class SinusoidWave : WaveFunction {
    override fun value(angle: Float) = sin(angle)

    override fun angle(value: Float) = asin(value)
}