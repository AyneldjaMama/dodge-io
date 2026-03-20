package io.dodge.util

/**
 * Platform-expect function for getting today's date as YYYY-MM-DD.
 * Each platform provides its own implementation.
 * For now, this can be injected from the Android side.
 */
fun getDailySeed(dateString: String): Int {
    return seedFromString(dateString)
}
