package io.dodge.engine

import kotlin.math.*

object CollisionDetector {
    fun circleCircle(
        ax: Float, ay: Float, ar: Float,
        bx: Float, by: Float, br: Float
    ): Boolean {
        val dx = ax - bx
        val dy = ay - by
        val dist = sqrt(dx * dx + dy * dy)
        return dist < ar + br
    }

    fun circleCircleDistance(
        ax: Float, ay: Float,
        bx: Float, by: Float
    ): Float {
        val dx = ax - bx
        val dy = ay - by
        return sqrt(dx * dx + dy * dy)
    }

    /**
     * Check if a point (player) collides with a laser line segment.
     * Matches the JS laser collision exactly.
     */
    fun pointToLaser(
        px: Float, py: Float, playerRadius: Float,
        laserX: Float, laserY: Float,
        angle: Float, length: Float, width: Float
    ): Boolean {
        val lcos = cos(angle)
        val lsin = sin(angle)
        val relX = px - laserX
        val relY = py - laserY
        val perp = abs(-relX * lsin + relY * lcos)
        val along = relX * lcos + relY * lsin
        return perp < (width / 2f + playerRadius) && abs(along) < length
    }
}
