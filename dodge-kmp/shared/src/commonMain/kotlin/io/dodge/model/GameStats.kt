package io.dodge.model

import kotlinx.serialization.Serializable

@Serializable
data class GameStats(
    val totalGames: Int = 0,
    val highScore: Int = 0,
    val bestTime: Int = 0,
    val totalNearMisses: Int = 0,
    val totalShieldsUsed: Int = 0,
    val totalAdsWatched: Int = 0,
    val dailyChallengeStreak: Int = 0,
    val lastDailyDate: String = "",
    val dailyHighScore: Int = 0,
    val dailyBestTime: Int = 0
)
