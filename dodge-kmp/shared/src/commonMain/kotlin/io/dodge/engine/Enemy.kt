package io.dodge.engine

import io.dodge.audio.SoundEffect
import io.dodge.model.EnemyType
import io.dodge.model.GameColors
import io.dodge.model.GameConfig
import io.dodge.model.GameEvent
import kotlin.math.*

/**
 * Result of updating an enemy for one frame.
 * alive=false means the enemy should be removed.
 * children contains any new enemies spawned on death (bomber/splitter fragments).
 */
data class EnemyUpdateResult(
    val alive: Boolean,
    val children: List<Enemy> = emptyList()
)

sealed class Enemy(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    val radius: Float,
    val color: Long,
    val type: EnemyType,
    var age: Int = 0,
    val maxAge: Int = Int.MAX_VALUE
) {
    abstract fun update(
        playerX: Float, playerY: Float, difficulty: Float,
        slowFactor: Float, speedMultiplier: Float,
        screenW: Float, playAreaH: Float,
        rng: () -> Float, events: MutableList<GameEvent>,
        spawnParticles: (Float, Float, Long, Int, Float) -> Unit
    ): EnemyUpdateResult

    class Bullet(
        x: Float, y: Float, vx: Float, vy: Float,
        radius: Float = GameConfig.BULLET_MIN_RADIUS,
        color: Long = GameColors.NEON_PINK,
        maxAge: Int = GameConfig.BULLET_MAX_AGE
    ) : Enemy(x, y, vx, vy, radius, color, EnemyType.BULLET, maxAge = maxAge) {
        override fun update(
            playerX: Float, playerY: Float, difficulty: Float,
            slowFactor: Float, speedMultiplier: Float,
            screenW: Float, playAreaH: Float,
            rng: () -> Float, events: MutableList<GameEvent>,
            spawnParticles: (Float, Float, Long, Int, Float) -> Unit
        ): EnemyUpdateResult {
            age += 1
            if (maxAge != Int.MAX_VALUE && age > maxAge) return EnemyUpdateResult(false)
            x += vx * slowFactor * speedMultiplier
            y += vy * slowFactor * speedMultiplier
            if (isOffScreen(screenW, playAreaH)) return EnemyUpdateResult(false)
            return EnemyUpdateResult(true)
        }
    }

    class Seeker(
        x: Float, y: Float, vx: Float, vy: Float
    ) : Enemy(x, y, vx, vy, GameConfig.SEEKER_RADIUS, GameColors.NEON_ORANGE, EnemyType.SEEKER, maxAge = GameConfig.SEEKER_MAX_AGE) {
        override fun update(
            playerX: Float, playerY: Float, difficulty: Float,
            slowFactor: Float, speedMultiplier: Float,
            screenW: Float, playAreaH: Float,
            rng: () -> Float, events: MutableList<GameEvent>,
            spawnParticles: (Float, Float, Long, Int, Float) -> Unit
        ): EnemyUpdateResult {
            age += 1
            if (age > maxAge) return EnemyUpdateResult(false)

            val sdx = playerX - x
            val sdy = playerY - y
            val sd = sqrt(sdx * sdx + sdy * sdy)
            if (sd > 0) {
                vx += (sdx / sd) * GameConfig.SEEKER_ACCEL * slowFactor * speedMultiplier
                vy += (sdy / sd) * GameConfig.SEEKER_ACCEL * slowFactor * speedMultiplier
                val sp = sqrt(vx * vx + vy * vy)
                val mx = (GameConfig.SEEKER_BASE_MAX_SPEED + difficulty * GameConfig.SEEKER_DIFFICULTY_MAX_SPEED) * speedMultiplier
                if (sp > mx) {
                    vx = (vx / sp) * mx
                    vy = (vy / sp) * mx
                }
            }

            x += vx * slowFactor * speedMultiplier
            y += vy * slowFactor * speedMultiplier
            return EnemyUpdateResult(true)
        }
    }

    class Wave(
        x: Float, y: Float, vx: Float, vy: Float,
        val amplitude: Float,
        val frequency: Float,
        val baseX: Float,
        val baseY: Float
    ) : Enemy(x, y, vx, vy, GameConfig.WAVE_RADIUS, GameColors.NEON_YELLOW, EnemyType.WAVE) {
        override fun update(
            playerX: Float, playerY: Float, difficulty: Float,
            slowFactor: Float, speedMultiplier: Float,
            screenW: Float, playAreaH: Float,
            rng: () -> Float, events: MutableList<GameEvent>,
            spawnParticles: (Float, Float, Long, Int, Float) -> Unit
        ): EnemyUpdateResult {
            age += 1
            if (vx != 0f) {
                x += vx * slowFactor * speedMultiplier
                y = baseY + sin(age * frequency) * amplitude
            } else {
                y += vy * slowFactor * speedMultiplier
                x = baseX + sin(age * frequency) * amplitude
            }
            if (isOffScreen(screenW, playAreaH)) return EnemyUpdateResult(false)
            return EnemyUpdateResult(true)
        }
    }

    class Spiral(
        x: Float, y: Float, vx: Float, vy: Float,
        var spiralAngle: Float,
        val spiralSpeed: Float,
        var spiralRadius: Float,
        var originX: Float,
        var originY: Float
    ) : Enemy(x, y, vx, vy, GameConfig.SPIRAL_RADIUS, GameColors.NEON_PURPLE, EnemyType.SPIRAL, maxAge = GameConfig.SPIRAL_MAX_AGE) {
        override fun update(
            playerX: Float, playerY: Float, difficulty: Float,
            slowFactor: Float, speedMultiplier: Float,
            screenW: Float, playAreaH: Float,
            rng: () -> Float, events: MutableList<GameEvent>,
            spawnParticles: (Float, Float, Long, Int, Float) -> Unit
        ): EnemyUpdateResult {
            age += 1
            if (age > maxAge) return EnemyUpdateResult(false)

            spiralAngle += spiralSpeed * slowFactor * speedMultiplier
            originX += vx * slowFactor * speedMultiplier
            originY += vy * slowFactor * speedMultiplier
            x = originX + cos(spiralAngle) * spiralRadius
            y = originY + sin(spiralAngle) * spiralRadius
            spiralRadius += GameConfig.SPIRAL_RADIUS_GROWTH
            return EnemyUpdateResult(true)
        }
    }

    class Splitter(
        x: Float, y: Float, vx: Float, vy: Float
    ) : Enemy(x, y, vx, vy, GameConfig.SPLITTER_RADIUS, GameColors.NEON_WHITE, EnemyType.SPLITTER, maxAge = GameConfig.SPLITTER_MAX_AGE) {
        override fun update(
            playerX: Float, playerY: Float, difficulty: Float,
            slowFactor: Float, speedMultiplier: Float,
            screenW: Float, playAreaH: Float,
            rng: () -> Float, events: MutableList<GameEvent>,
            spawnParticles: (Float, Float, Long, Int, Float) -> Unit
        ): EnemyUpdateResult {
            age += 1
            if (age >= maxAge) {
                events.add(GameEvent.PlaySound(SoundEffect.SPLIT_ENEMY))
                spawnParticles(x, y, GameColors.NEON_WHITE, 15, 3f)
                val children = mutableListOf<Enemy>()
                for (i in 0 until GameConfig.SPLITTER_CHILD_COUNT) {
                    val angle = (PI.toFloat() * 2f * i) / GameConfig.SPLITTER_CHILD_COUNT
                    children.add(
                        Bullet(
                            x = x, y = y,
                            vx = cos(angle) * GameConfig.SPLITTER_CHILD_SPEED,
                            vy = sin(angle) * GameConfig.SPLITTER_CHILD_SPEED,
                            radius = GameConfig.SPLITTER_CHILD_RADIUS,
                            color = GameColors.NEON_WHITE
                        )
                    )
                }
                return EnemyUpdateResult(false, children)
            }
            x += vx * slowFactor * speedMultiplier
            y += vy * slowFactor * speedMultiplier
            if (isOffScreen(screenW, playAreaH)) return EnemyUpdateResult(false)
            return EnemyUpdateResult(true)
        }
    }

    class Bomber(
        x: Float, y: Float, vx: Float, vy: Float
    ) : Enemy(x, y, vx, vy, GameConfig.BOMBER_RADIUS, GameColors.NEON_RED, EnemyType.BOMBER, maxAge = GameConfig.BOMBER_MAX_AGE) {
        override fun update(
            playerX: Float, playerY: Float, difficulty: Float,
            slowFactor: Float, speedMultiplier: Float,
            screenW: Float, playAreaH: Float,
            rng: () -> Float, events: MutableList<GameEvent>,
            spawnParticles: (Float, Float, Long, Int, Float) -> Unit
        ): EnemyUpdateResult {
            age += 1
            if (age >= maxAge) {
                events.add(GameEvent.PlaySound(SoundEffect.BOMBER_EXPLODE))
                events.add(GameEvent.ScreenShake(GameConfig.BOMBER_SCREEN_SHAKE))
                spawnParticles(x, y, GameColors.NEON_RED, 20, 3f)
                val children = mutableListOf<Enemy>()
                for (i in 0 until GameConfig.BOMBER_CHILD_COUNT) {
                    val angle = (PI.toFloat() * 2f * i) / GameConfig.BOMBER_CHILD_COUNT
                    children.add(
                        Bullet(
                            x = x, y = y,
                            vx = cos(angle) * GameConfig.BOMBER_CHILD_SPEED,
                            vy = sin(angle) * GameConfig.BOMBER_CHILD_SPEED,
                            radius = GameConfig.BOMBER_CHILD_RADIUS,
                            color = GameColors.NEON_RED
                        )
                    )
                }
                return EnemyUpdateResult(false, children)
            }
            x += vx * slowFactor * speedMultiplier
            y += vy * slowFactor * speedMultiplier
            return EnemyUpdateResult(true)
        }
    }

    class Teleporter(
        x: Float, y: Float, vx: Float, vy: Float,
        var teleportCooldown: Int,
        val teleportInterval: Int
    ) : Enemy(x, y, vx, vy, GameConfig.TELEPORTER_RADIUS, GameColors.TELEPORTER_COLOR, EnemyType.TELEPORTER, maxAge = GameConfig.TELEPORTER_MAX_AGE) {
        override fun update(
            playerX: Float, playerY: Float, difficulty: Float,
            slowFactor: Float, speedMultiplier: Float,
            screenW: Float, playAreaH: Float,
            rng: () -> Float, events: MutableList<GameEvent>,
            spawnParticles: (Float, Float, Long, Int, Float) -> Unit
        ): EnemyUpdateResult {
            age += 1
            if (age > maxAge) return EnemyUpdateResult(false)

            teleportCooldown -= 1
            if (teleportCooldown <= 0) {
                events.add(GameEvent.PlaySound(SoundEffect.TELEPORT))
                spawnParticles(x, y, color, 8, 2f)

                val tAngle = atan2(playerY - y, playerX - x)
                val tDist = GameConfig.TELEPORTER_MIN_DISTANCE + rng() * GameConfig.TELEPORTER_RANDOM_DISTANCE
                x = (playerX - cos(tAngle) * tDist).coerceIn(20f, screenW - 20f)
                y = (playerY - sin(tAngle) * tDist).coerceIn(20f, playAreaH - 20f)

                spawnParticles(x, y, color, 8, 2f)
                teleportCooldown = teleportInterval

                val newAngle = atan2(playerY - y, playerX - x)
                val teleSpeed = (GameConfig.TELEPORTER_BASE_SPEED + difficulty * GameConfig.TELEPORTER_DIFFICULTY_SPEED) * speedMultiplier
                vx = cos(newAngle) * teleSpeed
                vy = sin(newAngle) * teleSpeed
            }

            x += vx * slowFactor * speedMultiplier
            y += vy * slowFactor * speedMultiplier
            return EnemyUpdateResult(true)
        }
    }

    class Laser(
        x: Float, y: Float,
        val angle: Float,
        val length: Float,
        var width: Float = 0f
    ) : Enemy(x, y, 0f, 0f, 0f, GameColors.NEON_CYAN, EnemyType.LASER, maxAge = GameConfig.LASER_MAX_AGE) {
        override fun update(
            playerX: Float, playerY: Float, difficulty: Float,
            slowFactor: Float, speedMultiplier: Float,
            screenW: Float, playAreaH: Float,
            rng: () -> Float, events: MutableList<GameEvent>,
            spawnParticles: (Float, Float, Long, Int, Float) -> Unit
        ): EnemyUpdateResult {
            age += 1
            if (age > maxAge) return EnemyUpdateResult(false)

            when {
                age < GameConfig.LASER_CHARGE_FRAMES -> {
                    width = GameConfig.LASER_INITIAL_WIDTH
                }
                age == GameConfig.LASER_CHARGE_FRAMES -> {
                    events.add(GameEvent.PlaySound(SoundEffect.LASER_FIRE))
                    width = 20f + (age - GameConfig.LASER_CHARGE_FRAMES) * GameConfig.LASER_WIDTH_GROWTH
                }
                age < GameConfig.LASER_CHARGE_FRAMES + GameConfig.LASER_FIRE_FRAMES -> {
                    width = 20f + (age - GameConfig.LASER_CHARGE_FRAMES) * GameConfig.LASER_WIDTH_GROWTH
                }
                else -> {
                    width = maxOf(0f, GameConfig.LASER_MAX_WIDTH - (age - 40) * GameConfig.LASER_DECAY_RATE)
                }
            }
            // Laser doesn't move
            return EnemyUpdateResult(true)
        }
    }

    protected fun isOffScreen(screenW: Float, playAreaH: Float): Boolean {
        val margin = GameConfig.ENEMY_OFFSCREEN_MARGIN
        return x < -margin || x > screenW + margin || y < -margin || y > playAreaH + 20f
    }
}
