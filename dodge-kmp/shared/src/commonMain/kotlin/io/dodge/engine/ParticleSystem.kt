package io.dodge.engine

import io.dodge.model.GameColors
import io.dodge.model.GameConfig
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.max
import kotlin.math.sin

data class Particle(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    var life: Float,
    val maxLife: Float,
    val r: Int,
    val g: Int,
    val b: Int,
    val size: Float
)

class ParticleSystem {
    val particles: MutableList<Particle> = mutableListOf()

    fun spawnParticles(x: Float, y: Float, color: Long, count: Int, speed: Float, rng: () -> Float) {
        val room = GameConfig.MAX_PARTICLES - particles.size
        val actual = min(count, max(room, 0))
        val (r, g, b) = GameColors.colorToRgb(color)

        for (i in 0 until actual) {
            val angle = (PI.toFloat() * 2f * i) / count + (rng() - 0.5f) * 0.5f
            val spd = speed * (0.5f + rng())
            particles.add(
                Particle(
                    x = x, y = y,
                    vx = cos(angle) * spd,
                    vy = sin(angle) * spd,
                    life = 1f,
                    maxLife = 0.5f + rng() * 0.5f,
                    r = r, g = g, b = b,
                    size = 1f + rng() * 3f
                )
            )
        }
    }

    fun update() {
        val iter = particles.iterator()
        while (iter.hasNext()) {
            val p = iter.next()
            p.x += p.vx
            p.y += p.vy
            p.vx *= 0.96f
            p.vy *= 0.96f
            p.life -= 1f / 60f / p.maxLife
            if (p.life <= 0f) {
                iter.remove()
            }
        }
    }

    fun clear() {
        particles.clear()
    }
}
