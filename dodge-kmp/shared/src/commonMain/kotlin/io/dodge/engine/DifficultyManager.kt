package io.dodge.engine

import io.dodge.model.GameConfig
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

object DifficultyManager {
    fun computeDifficulty(time: Int): Float {
        return 1f + time / GameConfig.DIFFICULTY_SCALE
    }

    fun computeSpawnRate(difficulty: Float): Float {
        return max(GameConfig.SPAWN_RATE_MIN, GameConfig.SPAWN_RATE_BASE - difficulty * GameConfig.SPAWN_RATE_REDUCTION)
    }

    fun computeSpawnCount(difficulty: Float): Int {
        return min(floor(difficulty / 10f).toInt() + 1, 3)
    }

    fun computeSpeedMultiplier(elapsedSec: Float): Float {
        if (elapsedSec <= GameConfig.SPEED_MULTIPLIER_START_SEC) return 1f
        val tiers = floor((elapsedSec - GameConfig.SPEED_MULTIPLIER_START_SEC) / GameConfig.SPEED_MULTIPLIER_TIER_SEC).toInt()
        return GameConfig.SPEED_MULTIPLIER_BASE.pow(tiers)
    }

    fun computeEarlySlowdown(elapsedSec: Float): Float {
        return if (elapsedSec < GameConfig.EARLY_SLOWDOWN_DURATION) {
            GameConfig.EARLY_SLOWDOWN_MIN + (elapsedSec / GameConfig.EARLY_SLOWDOWN_DURATION) * (1f - GameConfig.EARLY_SLOWDOWN_MIN)
        } else {
            1f
        }
    }
}
