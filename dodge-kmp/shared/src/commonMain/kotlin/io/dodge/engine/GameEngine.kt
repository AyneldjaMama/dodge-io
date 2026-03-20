package io.dodge.engine

import io.dodge.audio.SoundEffect
import io.dodge.model.*
import io.dodge.util.Mulberry32
import io.dodge.util.seedFromString
import kotlin.math.*
import kotlin.random.Random

data class FrameResult(
    val renderCommands: List<RenderCommand>,
    val events: List<GameEvent>
)

class GameEngine(
    private val screenWidth: Float,
    private val screenHeight: Float,
    private val mode: GameMode,
    private val dailySeed: String? = null
) {
    // RNG
    private val seededRng: Mulberry32? = dailySeed?.let { Mulberry32(seedFromString(it)) }
    private val kotlinRandom = Random.Default
    private fun rng(): Float = seededRng?.next() ?: kotlinRandom.nextFloat()

    // Play area
    private val playAreaH = screenHeight - GameConfig.BOTTOM_SAFE

    // Entities
    private val player = Player()
    private val enemies = mutableListOf<Enemy>()
    private val particleSystem = ParticleSystem()
    private val powerUps = mutableListOf<PowerUp>()
    private val spawner = EnemySpawner()

    // State
    private var score = 0f
    private var time = 0
    private var spawnTimer = 0
    private var difficulty = 1f
    var alive = false; private set
    var running = false; private set
    private var touchX = screenWidth / 2f
    private var touchY = playAreaH / 2f

    // Power-up state
    private var shieldActive = false
    private var shieldTimer = 0
    private var slowActive = false
    private var slowTimer = 0
    private var shrinkActive = false
    private var shrinkTimer = 0

    // Near-miss
    private var nearMissStreak = 0
    private var lastNearMiss = 0
    private var pendingNearMisses = 0

    // Visual effects
    private var comboText = ""
    private var comboTimer = 0
    private var screenShake = 0f
    private var flashTimer = 0
    private var iFrames = 0

    // Death delay
    private var deathDelayTimer = -1

    fun start() {
        player.reset(screenWidth / 2f, playAreaH / 2f)
        enemies.clear()
        particleSystem.clear()
        powerUps.clear()
        score = 0f
        time = 0
        spawnTimer = 0
        difficulty = 1f
        alive = true
        running = true
        touchX = screenWidth / 2f
        touchY = playAreaH / 2f
        shieldActive = false; shieldTimer = 0
        slowActive = false; slowTimer = 0
        shrinkActive = false; shrinkTimer = 0
        nearMissStreak = 0; lastNearMiss = 0; pendingNearMisses = 0
        comboText = ""; comboTimer = 0
        screenShake = 0f; flashTimer = 0; iFrames = 0
        deathDelayTimer = -1
    }

    fun respawn() {
        player.reset(screenWidth / 2f, playAreaH / 2f)
        alive = true
        running = true
        iFrames = GameConfig.IFRAMES_RESPAWN
        shieldActive = true
        shieldTimer = GameConfig.POWER_UP_DURATION
        particleSystem.spawnParticles(player.x, player.y, GameColors.NEON_CYAN, 40, 5f) { rng() }
        comboText = "CONTINUE!"
        comboTimer = 90
        deathDelayTimer = -1
    }

    fun updateTouchPosition(x: Float, y: Float) {
        touchX = x
        touchY = y
    }

    fun getScore(): Int = floor(score).toInt()
    fun getTime(): Int = time / 60

    fun tick(): FrameResult {
        val events = mutableListOf<GameEvent>()
        val commands = mutableListOf<RenderCommand>()

        // Handle death delay
        if (deathDelayTimer > 0) {
            deathDelayTimer -= 1
            if (deathDelayTimer <= 0) {
                running = false
                events.add(GameEvent.PlayerDied(
                    score = floor(score).toInt(),
                    time = time / 60,
                    isDaily = mode == GameMode.DAILY
                ))
            }
        }

        // === RENDER: Background ===
        commands.add(RenderCommand.ClearBackground(GameColors.BACKGROUND))
        commands.add(RenderCommand.DrawGrid(screenWidth, playAreaH, GameConfig.GRID_SPACING, GameColors.GRID_COLOR))

        // === RENDER: Particles (always) ===
        particleSystem.update()
        for (p in particleSystem.particles) {
            commands.add(RenderCommand.DrawParticle(p.x, p.y, p.size * p.life, p.r, p.g, p.b, p.life))
        }

        // === Screen shake ===
        if (screenShake > 0f) {
            screenShake -= 1f
            commands.add(RenderCommand.SetScreenShake(screenShake))
        } else {
            commands.add(RenderCommand.ResetTransform)
        }

        // === Flash ===
        if (flashTimer > 0) {
            flashTimer -= 1
            commands.add(RenderCommand.DrawFlash(flashTimer / 10f))
        }

        if (!running || !alive) {
            return FrameResult(commands, events)
        }

        // === ACTIVE GAME ===
        time += 1
        difficulty = DifficultyManager.computeDifficulty(time)

        // Score
        val scoreMultiplier = if (nearMissStreak > 0) 1f + nearMissStreak * GameConfig.NEAR_MISS_MULTIPLIER_PER_STREAK else 1f
        score += (GameConfig.SCORE_BASE + difficulty * GameConfig.SCORE_DIFFICULTY_FACTOR) * scoreMultiplier

        // Periodic score update + batched near-miss flush
        if (time % 60 == 0) {
            events.add(GameEvent.ScoreUpdate(floor(score).toInt(), time / 60))
            if (pendingNearMisses > 0) {
                events.add(GameEvent.NearMiss(nearMissStreak, pendingNearMisses))
                pendingNearMisses = 0
            }
        }

        // Timers
        if (shieldActive) {
            shieldTimer -= 1
            if (shieldTimer <= 0) shieldActive = false
        }
        if (slowActive) {
            slowTimer -= 1
            if (slowTimer <= 0) slowActive = false
        }
        if (shrinkActive) {
            shrinkTimer -= 1
            if (shrinkTimer <= 0) {
                shrinkActive = false
                player.radius = GameConfig.PLAYER_RADIUS
            }
        }
        if (comboTimer > 0) comboTimer -= 1
        if (time - lastNearMiss > GameConfig.NEAR_MISS_STREAK_TIMEOUT) nearMissStreak = 0

        // Move player
        player.updatePosition(touchX, touchY, screenWidth, playAreaH)
        player.updateTrail()

        // Speed multiplier
        val elapsedSec = time / 60f
        val speedMultiplier = DifficultyManager.computeSpeedMultiplier(elapsedSec)
        val slowFactor = if (slowActive) GameConfig.SLOW_FACTOR else 1f

        // Spawn enemies
        val spawnRate = DifficultyManager.computeSpawnRate(difficulty)
        spawnTimer += 1
        if (spawnTimer >= spawnRate.toInt()) {
            spawnTimer = 0
            val count = DifficultyManager.computeSpawnCount(difficulty)
            for (i in 0 until count) {
                enemies.add(spawner.spawnEnemy(time, difficulty, player.x, player.y, screenWidth, playAreaH, ::rng, events))
            }
        }

        // Spawn power-ups
        if (time % GameConfig.POWER_UP_SPAWN_INTERVAL == 0 && time > 0) {
            val types = PowerUpType.entries.toTypedArray()
            val type = types[floor(rng() * types.size).toInt().coerceIn(0, types.size - 1)]
            powerUps.add(
                PowerUp(
                    x = 50f + rng() * (screenWidth - 100f),
                    y = 50f + rng() * (playAreaH - 100f),
                    radius = GameConfig.POWER_UP_RADIUS,
                    type = type,
                    maxAge = GameConfig.POWER_UP_MAX_AGE
                )
            )
        }

        // Update enemies
        val newEnemies = mutableListOf<Enemy>()
        val iter = enemies.iterator()
        while (iter.hasNext()) {
            val e = iter.next()
            val result = e.update(player.x, player.y, difficulty, slowFactor, speedMultiplier, screenWidth, playAreaH, ::rng, events) { px, py, color, count, speed ->
                particleSystem.spawnParticles(px, py, color, count, speed) { rng() }
            }
            if (!result.alive) {
                iter.remove()
                newEnemies.addAll(result.children)
            }
        }
        enemies.addAll(newEnemies)

        // Update power-ups
        val puIter = powerUps.iterator()
        while (puIter.hasNext()) {
            val p = puIter.next()
            p.age += 1
            if (p.age > p.maxAge) {
                puIter.remove()
                continue
            }
            val pd = CollisionDetector.circleCircleDistance(player.x, player.y, p.x, p.y)
            if (pd < player.radius + p.radius) {
                events.add(GameEvent.PlaySound(SoundEffect.POWER_UP))
                events.add(GameEvent.PowerUpCollected(p.type))
                particleSystem.spawnParticles(p.x, p.y, GameColors.NEON_CYAN, 15, 3f) { rng() }
                when (p.type) {
                    PowerUpType.SHIELD -> { shieldActive = true; shieldTimer = GameConfig.POWER_UP_DURATION }
                    PowerUpType.SLOW -> { slowActive = true; slowTimer = GameConfig.POWER_UP_DURATION }
                    PowerUpType.SHRINK -> { shrinkActive = true; shrinkTimer = GameConfig.POWER_UP_DURATION; player.radius = GameConfig.PLAYER_RADIUS_SHRUNK }
                }
                comboText = p.type.name + "!"
                comboTimer = 60
                puIter.remove()
            }
        }

        // Collision detection
        var hit = false
        var hitIdx = -1
        if (iFrames <= 0) {
            for (ci in enemies.indices) {
                val e = enemies[ci]
                if (e is Enemy.Laser) {
                    if (e.width > GameConfig.LASER_HIT_MIN_WIDTH) {
                        if (CollisionDetector.pointToLaser(
                                player.x, player.y, player.radius,
                                e.x, e.y, e.angle, e.length, e.width
                            )
                        ) {
                            hit = true; hitIdx = ci; break
                        }
                    }
                    continue
                }

                val cd = CollisionDetector.circleCircleDistance(player.x, player.y, e.x, e.y)
                if (cd < player.radius + e.radius) {
                    hit = true; hitIdx = ci
                    particleSystem.spawnParticles(e.x, e.y, e.color, 15, 3f) { rng() }
                    break
                }

                // Near miss
                if (cd < player.radius + e.radius + GameConfig.NEAR_MISS_ZONE && cd > player.radius + e.radius) {
                    if (time - lastNearMiss > GameConfig.NEAR_MISS_COOLDOWN) {
                        nearMissStreak += 1
                        lastNearMiss = time
                        score += GameConfig.NEAR_MISS_SCORE_PER_STREAK * nearMissStreak
                        comboText = "NEAR MISS x$nearMissStreak"
                        comboTimer = 45
                        events.add(GameEvent.PlaySound(SoundEffect.NEAR_MISS))
                        if (nearMissStreak > 1) events.add(GameEvent.PlaySound(SoundEffect.COMBO))
                        particleSystem.spawnParticles(player.x, player.y, GameColors.NEON_GREEN, 5, 2f) { rng() }
                        pendingNearMisses += 1
                    }
                }
            }
        } else {
            iFrames -= 1
        }

        // Handle hit
        if (hit && shieldActive) {
            // Shield absorbs the hit
            events.add(GameEvent.PlaySound(SoundEffect.SHIELD_BREAK))
            events.add(GameEvent.ShieldBreak)
            shieldActive = false
            iFrames = GameConfig.IFRAMES_SHIELD_BREAK

            if (hitIdx >= 0 && hitIdx < enemies.size) {
                val rem = enemies.removeAt(hitIdx)
                particleSystem.spawnParticles(rem.x, rem.y, GameColors.NEON_CYAN, 20, 4f) { rng() }
            }

            // Knockback nearby enemies
            for (e in enemies) {
                val rdx = e.x - player.x
                val rdy = e.y - player.y
                val rd = sqrt(rdx * rdx + rdy * rdy)
                if (rd < GameConfig.SHIELD_KNOCKBACK_RADIUS && rd > 0f) {
                    val f = (1f - rd / GameConfig.SHIELD_KNOCKBACK_RADIUS) * GameConfig.SHIELD_KNOCKBACK_FORCE
                    e.vx += (rdx / rd) * f
                    e.vy += (rdy / rd) * f
                }
            }
            screenShake = 10f; flashTimer = 4
            particleSystem.spawnParticles(player.x, player.y, GameColors.NEON_CYAN, 40, 6f) { rng() }
            comboText = "SHIELD BREAK!"
            comboTimer = 60
        } else if (hit) {
            // No shield — player dies
            killPlayer(events)
        }

        // === RENDER: Trail ===
        for (t in player.trail) {
            val a = 1f - t.age / 25f
            if (a <= 0f) continue
            commands.add(RenderCommand.DrawCircle(
                t.x, t.y, player.radius * a * 0.6f,
                GameColors.NEON_GREEN, alpha = a * 0.2f
            ))
        }

        // === RENDER: Player ===
        val inv = iFrames > 0
        val show = !inv || (iFrames / 3) % 2 == 0
        if (show) {
            // Glow
            commands.add(RenderCommand.DrawGlowCircle(
                player.x, player.y, player.radius * 0.5f, player.radius * 3f,
                GameColors.NEON_GREEN
            ))
            // Body
            commands.add(RenderCommand.DrawCircle(
                player.x, player.y, player.radius,
                if (inv) GameColors.NEON_CYAN else GameColors.NEON_GREEN
            ))
            // Core
            commands.add(RenderCommand.DrawCircle(
                player.x, player.y, player.radius * 0.5f,
                GameColors.WHITE
            ))
        }

        // === RENDER: Shield ring ===
        if (shieldActive) {
            val shieldAlpha = 0.5f + sin(time * 0.1f) * 0.3f
            commands.add(RenderCommand.DrawRing(
                player.x, player.y, player.radius + 8f,
                GameColors.NEON_CYAN, shieldAlpha, 2f
            ))
        }

        // === RENDER: Enemies ===
        for (e in enemies) {
            if (e is Enemy.Laser) {
                commands.add(RenderCommand.DrawLaser(
                    e.x, e.y, e.angle, e.length, e.width,
                    e.age, GameConfig.LASER_CHARGE_FRAMES
                ))
                continue
            }

            // Glow
            commands.add(RenderCommand.DrawGlowCircle(
                e.x, e.y, 0f, e.radius * 2.5f, e.color
            ))
            // Body
            commands.add(RenderCommand.DrawCircle(e.x, e.y, e.radius, e.color))

            // Type-specific decorations
            when (e) {
                is Enemy.Seeker -> {
                    commands.add(RenderCommand.DrawRing(e.x, e.y, e.radius + 4f, e.color, 0.4f, 1f))
                }
                is Enemy.Bomber -> {
                    val progress = e.age.toFloat() / e.maxAge
                    commands.add(RenderCommand.DrawArc(
                        e.x, e.y, e.radius + 3f,
                        -PI.toFloat() / 2f, PI.toFloat() * 2f * progress,
                        GameColors.NEON_RED, 2f
                    ))
                }
                is Enemy.Spiral -> {
                    val ringRadius = e.radius + 5f + sin(e.age * 0.1f) * 3f
                    val ringAlpha = 0.2f + sin(e.age * 0.08f) * 0.15f
                    commands.add(RenderCommand.DrawRing(e.x, e.y, ringRadius, GameColors.NEON_PURPLE, ringAlpha, 1f))
                }
                is Enemy.Splitter -> {
                    val splitProgress = e.age.toFloat() / e.maxAge
                    commands.add(RenderCommand.DrawArc(
                        e.x, e.y, e.radius + 4f,
                        -PI.toFloat() / 2f, PI.toFloat() * 2f * splitProgress,
                        GameColors.NEON_WHITE, 2f
                    ))
                    if (splitProgress > 0.7f) {
                        val pulse = sin(e.age * 0.3f) * 0.3f + 0.7f
                        commands.add(RenderCommand.DrawCircle(e.x, e.y, e.radius * pulse, GameColors.NEON_WHITE, 0.3f))
                    }
                }
                is Enemy.Teleporter -> {
                    val telAlpha = if (e.teleportCooldown < 15) 0.3f + sin(e.age * 0.5f) * 0.3f else 0.15f
                    commands.add(RenderCommand.DrawRing(
                        e.x, e.y, e.radius + 6f,
                        GameColors.TELEPORTER_COLOR, telAlpha, 1f,
                        dashPattern = floatArrayOf(4f, 4f)
                    ))
                }
                else -> {} // Bullet has no extra decoration
            }
        }

        // === RENDER: Power-ups ===
        for (p in powerUps) {
            val pulse = 1f + sin(p.age * 0.08f) * 0.2f
            val alpha = if (p.age > p.maxAge - 60) (p.maxAge - p.age) / 60f else 1f
            val label = when (p.type) {
                PowerUpType.SHIELD -> "S"
                PowerUpType.SLOW -> "~"
                PowerUpType.SHRINK -> "-"
            }
            commands.add(RenderCommand.DrawPowerUp(p.x, p.y, p.radius * pulse, label, pulse, alpha))
        }

        // === RENDER: HUD elements ===
        commands.add(RenderCommand.ResetTransform)

        if (nearMissStreak > 0) {
            commands.add(RenderCommand.DrawText(
                "STREAK x$nearMissStreak",
                screenWidth - 24f, 40f,
                GameColors.NEON_GREEN, 1f, 14f,
                bold = true, align = TextAlign.RIGHT
            ))
        }

        if (comboTimer > 0) {
            commands.add(RenderCommand.DrawText(
                comboText,
                screenWidth / 2f, playAreaH / 2f - 60f,
                GameColors.NEON_GREEN, comboTimer / 60f, 18f,
                bold = true
            ))
        }

        return FrameResult(commands, events)
    }

    private fun killPlayer(events: MutableList<GameEvent>) {
        alive = false
        events.add(GameEvent.PlaySound(SoundEffect.DEATH))
        particleSystem.spawnParticles(player.x, player.y, GameColors.NEON_GREEN, GameConfig.DEATH_PARTICLES_GREEN, 6f) { rng() }
        particleSystem.spawnParticles(player.x, player.y, GameColors.WHITE, GameConfig.DEATH_PARTICLES_WHITE, 4f) { rng() }
        screenShake = GameConfig.DEATH_SCREEN_SHAKE
        flashTimer = GameConfig.DEATH_FLASH

        // Delay before sending playerDied (matches JS setTimeout 600ms = ~36 frames)
        deathDelayTimer = GameConfig.DEATH_DELAY_FRAMES
    }
}
