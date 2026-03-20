package io.dodge.android.storage

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import io.dodge.model.GameStats
import io.dodge.storage.StatsRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.UUID

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "dodgeio_stats")

class DataStoreStatsRepo(private val context: Context) : StatsRepository {

    private object Keys {
        val TOTAL_GAMES = intPreferencesKey("totalGames")
        val HIGH_SCORE = intPreferencesKey("highScore")
        val BEST_TIME = intPreferencesKey("bestTime")
        val TOTAL_NEAR_MISSES = intPreferencesKey("totalNearMisses")
        val TOTAL_SHIELDS_USED = intPreferencesKey("totalShieldsUsed")
        val TOTAL_ADS_WATCHED = intPreferencesKey("totalAdsWatched")
        val DAILY_STREAK = intPreferencesKey("dailyChallengeStreak")
        val LAST_DAILY_DATE = stringPreferencesKey("lastDailyDate")
        val DAILY_HIGH_SCORE = intPreferencesKey("dailyHighScore")
        val DAILY_BEST_TIME = intPreferencesKey("dailyBestTime")
        val DEVICE_ID = stringPreferencesKey("deviceId")
        val DISPLAY_NAME = stringPreferencesKey("displayName")
    }

    override suspend fun loadStats(): GameStats {
        val prefs = context.dataStore.data.first()
        return GameStats(
            totalGames = prefs[Keys.TOTAL_GAMES] ?: 0,
            highScore = prefs[Keys.HIGH_SCORE] ?: 0,
            bestTime = prefs[Keys.BEST_TIME] ?: 0,
            totalNearMisses = prefs[Keys.TOTAL_NEAR_MISSES] ?: 0,
            totalShieldsUsed = prefs[Keys.TOTAL_SHIELDS_USED] ?: 0,
            totalAdsWatched = prefs[Keys.TOTAL_ADS_WATCHED] ?: 0,
            dailyChallengeStreak = prefs[Keys.DAILY_STREAK] ?: 0,
            lastDailyDate = prefs[Keys.LAST_DAILY_DATE] ?: "",
            dailyHighScore = prefs[Keys.DAILY_HIGH_SCORE] ?: 0,
            dailyBestTime = prefs[Keys.DAILY_BEST_TIME] ?: 0
        )
    }

    override suspend fun saveStats(stats: GameStats) {
        context.dataStore.edit { prefs ->
            prefs[Keys.TOTAL_GAMES] = stats.totalGames
            prefs[Keys.HIGH_SCORE] = stats.highScore
            prefs[Keys.BEST_TIME] = stats.bestTime
            prefs[Keys.TOTAL_NEAR_MISSES] = stats.totalNearMisses
            prefs[Keys.TOTAL_SHIELDS_USED] = stats.totalShieldsUsed
            prefs[Keys.TOTAL_ADS_WATCHED] = stats.totalAdsWatched
            prefs[Keys.DAILY_STREAK] = stats.dailyChallengeStreak
            prefs[Keys.LAST_DAILY_DATE] = stats.lastDailyDate
            prefs[Keys.DAILY_HIGH_SCORE] = stats.dailyHighScore
            prefs[Keys.DAILY_BEST_TIME] = stats.dailyBestTime
        }
    }

    override suspend fun getDeviceId(): String {
        val prefs = context.dataStore.data.first()
        val existing = prefs[Keys.DEVICE_ID]
        if (existing != null) return existing

        val newId = System.currentTimeMillis().toString(36) + UUID.randomUUID().toString().take(9)
        context.dataStore.edit { it[Keys.DEVICE_ID] = newId }
        return newId
    }

    override suspend fun getDisplayName(): String {
        val prefs = context.dataStore.data.first()
        val existing = prefs[Keys.DISPLAY_NAME]
        if (existing != null) return existing

        val defaultName = "Player_${(1000..9999).random()}"
        context.dataStore.edit { it[Keys.DISPLAY_NAME] = defaultName }
        return defaultName
    }

    override suspend fun setDisplayName(name: String) {
        context.dataStore.edit { it[Keys.DISPLAY_NAME] = name.take(32) }
    }
}
