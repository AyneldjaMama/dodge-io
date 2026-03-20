package io.dodge.storage

import io.dodge.model.GameStats

interface StatsRepository {
    suspend fun loadStats(): GameStats
    suspend fun saveStats(stats: GameStats)
    suspend fun getDeviceId(): String
    suspend fun getDisplayName(): String
    suspend fun setDisplayName(name: String)
}
