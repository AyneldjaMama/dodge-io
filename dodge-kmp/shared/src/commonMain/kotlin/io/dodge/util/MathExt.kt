package io.dodge.util

import kotlin.math.sqrt

fun Float.clamp(min: Float, max: Float): Float = when {
    this < min -> min
    this > max -> max
    else -> this
}

fun distanceBetween(x1: Float, y1: Float, x2: Float, y2: Float): Float {
    val dx = x1 - x2
    val dy = y1 - y2
    return sqrt(dx * dx + dy * dy)
}
