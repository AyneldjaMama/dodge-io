package io.dodge.engine

import io.dodge.model.GameConfig
import io.dodge.util.clamp
import kotlin.math.min
import kotlin.math.sqrt

data class TrailPoint(val x: Float, val y: Float, var age: Int = 0)

class Player {
    var x: Float = 0f
    var y: Float = 0f
    var radius: Float = GameConfig.PLAYER_RADIUS
    val trail: MutableList<TrailPoint> = mutableListOf()

    fun reset(centerX: Float, centerY: Float) {
        x = centerX
        y = centerY
        radius = GameConfig.PLAYER_RADIUS
        trail.clear()
    }

    fun updatePosition(touchX: Float, touchY: Float, screenW: Float, playAreaH: Float) {
        val dx = touchX - x
        val dy = (touchY - GameConfig.TOUCH_OFFSET) - y
        val dist = sqrt(dx * dx + dy * dy)
        if (dist > 1f) {
            val spd = min(dist * GameConfig.PLAYER_SPEED_FACTOR, GameConfig.PLAYER_MAX_SPEED)
            x += (dx / dist) * spd
            y += (dy / dist) * spd
        }
        x = x.clamp(radius, screenW - radius)
        y = y.clamp(radius, playAreaH - radius)
    }

    fun updateTrail() {
        trail.add(TrailPoint(x, y))
        if (trail.size > GameConfig.TRAIL_LENGTH) {
            trail.removeAt(0)
        }
        for (t in trail) {
            t.age += 1
        }
    }
}
