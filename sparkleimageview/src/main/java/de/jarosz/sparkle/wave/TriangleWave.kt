package de.jarosz.sparkle.wave

import kotlin.math.asin
import kotlin.math.sin

class TriangleWave : WaveFunction {
    override fun value(angle: Float) = 2 / Math.PI.toFloat() * asin(sin(angle))

    override fun angle(value: Float) = asin(sin(value * Math.PI.toFloat() / 2))
}