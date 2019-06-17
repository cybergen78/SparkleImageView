package de.jarosz.sparkle.wave

interface WaveFunction {
    fun value(angle: Float): Float
    
    fun angle(value: Float): Float
}