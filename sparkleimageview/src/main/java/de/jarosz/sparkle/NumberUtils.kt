package de.jarosz.sparkle

import java.util.*
import kotlin.math.max
import kotlin.math.min

infix fun Float.max(other: Float) = min(this, other)

infix fun Float.min(other: Float) = max(this, other)

fun Float.format(format: String = "%.2f") = Formatter(Locale.getDefault()).format(format, this).toString()

infix fun Int.max(other: Int) = min(this, other)

infix fun Int.min(other: Int) = max(this, other)