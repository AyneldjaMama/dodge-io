package io.dodge.engine

import io.dodge.audio.SoundEffect
import io.dodge.model.EnemyType
import io.dodge.model.GameColors
import io.dodge.model.GameConfig
import io.dodge.model.GameEvent
import kotlin.math.*

class EnemySpawner {

    fun spawnEnemy(
        time: Int,
        difficulty: Float,
        playerX: Float,
        playerY: Float,
        screenW: Float,
        playAreaH: Float,
        rng: () -> Float,
        events: MutableList<GameEvent>
    ): Enemy {
        val seconds = time / 60f

        // Build available types based on time
        val types = mutableListOf(EnemyType.BULLET)
        if (seconds >= GameConfig.UNLOCK_SEEKER) types.add(EnemyType.SEEKER)
        if (seconds >= GameConfig.UNLOCK_WAVE) types.add(EnemyType.WAVE)
        if (seconds >= GameConfig.UNLOCK_SPIRAL) types.add(EnemyType.SPIRAL)
        if (seconds >= GameConfig.UNLOCK_SPLITTER) types.add(EnemyType.SPLITTER)
        if (seconds >= GameConfig.UNLOCK_BOMBER) types.add(EnemyType.BOMBER)
        if (seconds >= GameConfig.UNLOCK_TELEPORTER) types.add(EnemyType.TELEPORTER)
        if (seconds >= GameConfig.UNLOCK_LASER) types.add(EnemyType.LASER)

        val type = types[floor(rng() * types.size).toInt().coerceIn(0, types.size - 1)]

        // Determine spawn position from random side
        val side = floor(rng() * 4).toInt()
        val margin = GameConfig.SPAWN_MARGIN
        var x: Float
        var y: Float
        when (side) {
            0 -> { x = -margin; y = rng() * playAreaH }
            1 -> { x = screenW + margin; y = rng() * playAreaH }
            2 -> { x = rng() * screenW; y = -margin }
            else -> { x = rng() * screenW; y = playAreaH + margin + GameConfig.BOTTOM_SAFE }
        }

        val earlySlowdown = DifficultyManager.computeEarlySlowdown(seconds)
        val speed = (GameConfig.BULLET_BASE_SPEED + difficulty * GameConfig.BULLET_DIFFICULTY_SPEED + rng() * GameConfig.BULLET_RANDOM_SPEED) * earlySlowdown
        val angle = atan2(playerY - y, playerX - x)
        val vx = cos(angle) * speed
        val vy = sin(angle) * speed

        return when (type) {
            EnemyType.BULLET -> Enemy.Bullet(
                x = x, y = y, vx = vx, vy = vy,
                radius = GameConfig.BULLET_MIN_RADIUS + rng() * GameConfig.BULLET_RANDOM_RADIUS
            )
            EnemyType.SEEKER -> Enemy.Seeker(
                x = x, y = y,
                vx = vx * GameConfig.SEEKER_INITIAL_SPEED_FACTOR,
                vy = vy * GameConfig.SEEKER_INITIAL_SPEED_FACTOR
            )
            EnemyType.WAVE -> {
                val horiz = side <= 1
                Enemy.Wave(
                    x = x, y = y,
                    vx = if (horiz) (if (side == 0) speed * GameConfig.WAVE_SPEED_FACTOR else -speed * GameConfig.WAVE_SPEED_FACTOR) else 0f,
                    vy = if (!horiz) (if (side == 2) speed * GameConfig.WAVE_SPEED_FACTOR else -speed * GameConfig.WAVE_SPEED_FACTOR) else 0f,
                    amplitude = GameConfig.WAVE_MIN_AMPLITUDE + rng() * GameConfig.WAVE_RANDOM_AMPLITUDE,
                    frequency = GameConfig.WAVE_MIN_FREQUENCY + rng() * GameConfig.WAVE_RANDOM_FREQUENCY,
                    baseX = x, baseY = y
                )
            }
            EnemyType.SPIRAL -> {
                events.add(GameEvent.PlaySound(SoundEffect.SPIRAL_SPAWN))
                Enemy.Spiral(
                    x = x, y = y,
                    vx = vx * GameConfig.SPIRAL_SPEED_FACTOR,
                    vy = vy * GameConfig.SPIRAL_SPEED_FACTOR,
                    spiralAngle = rng() * PI.toFloat() * 2f,
                    spiralSpeed = GameConfig.SPIRAL_MIN_SPEED + rng() * GameConfig.SPIRAL_RANDOM_SPEED,
                    spiralRadius = GameConfig.SPIRAL_MIN_RADIUS + rng() * GameConfig.SPIRAL_RANDOM_RADIUS,
                    originX = x, originY = y
                )
            }
            EnemyType.SPLITTER -> Enemy.Splitter(
                x = x, y = y,
                vx = vx * GameConfig.SPLITTER_SPEED_FACTOR,
                vy = vy * GameConfig.SPLITTER_SPEED_FACTOR
            )
            EnemyType.BOMBER -> Enemy.Bomber(
                x = x, y = y,
                vx = vx * GameConfig.BOMBER_SPEED_FACTOR,
                vy = vy * GameConfig.BOMBER_SPEED_FACTOR
            )
            EnemyType.TELEPORTER -> Enemy.Teleporter(
                x = x, y = y,
                vx = vx * GameConfig.TELEPORTER_SPEED_FACTOR,
                vy = vy * GameConfig.TELEPORTER_SPEED_FACTOR,
                teleportCooldown = 0,
                teleportInterval = GameConfig.TELEPORTER_MIN_COOLDOWN + floor(rng() * GameConfig.TELEPORTER_RANDOM_COOLDOWN).toInt()
            )
            EnemyType.LASER -> {
                events.add(GameEvent.PlaySound(SoundEffect.LASER_CHARGE))
                val laserX = screenW / 2f
                val laserY = playAreaH / 2f
                Enemy.Laser(
                    x = laserX, y = laserY,
                    angle = atan2(playerY - laserY, playerX - laserX),
                    length = max(screenW, playAreaH) * 1.5f
                )
            }
        }
    }
}
