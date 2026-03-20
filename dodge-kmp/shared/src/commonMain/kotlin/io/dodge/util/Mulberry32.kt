package io.dodge.util

/**
 * Mulberry32 PRNG - must produce identical output to the JavaScript implementation.
 * JS uses 32-bit integer arithmetic with Math.imul and bitwise OR for int32 coercion.
 * Kotlin Int is already 32-bit, so regular multiplication truncates identically.
 */
class Mulberry32(seed: Int) {
    private var state: Int = seed

    fun next(): Float {
        state += 0x6D2B79F5.toInt()
        var t = (state xor (state ushr 15)) * (1 or state)
        t = (t + (t xor (t ushr 7)) * (61 or t)) xor t
        // Convert to unsigned 32-bit range [0, 4294967296) then divide
        return ((t xor (t ushr 14)).toLong() and 0xFFFFFFFFL).toFloat() / 4294967296f
    }
}

fun seedFromString(str: String): Int {
    var h = 0
    for (c in str) {
        h = 31 * h + c.code
    }
    return h
}
