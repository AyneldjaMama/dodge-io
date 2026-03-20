package io.dodge.model

object GameColors {
    const val BACKGROUND: Long = 0xFF0A0A1A
    const val BACKGROUND_LIGHT: Long = 0xFF12122A
    const val SURFACE: Long = 0xFF1A1A3A
    const val SURFACE_LIGHT: Long = 0xFF242450

    const val NEON_GREEN: Long = 0xFF00FF88
    const val NEON_PINK: Long = 0xFFFF2D95
    const val NEON_CYAN: Long = 0xFF00D4FF
    const val NEON_YELLOW: Long = 0xFFFFE14D
    const val NEON_ORANGE: Long = 0xFFFF6B2B
    const val NEON_RED: Long = 0xFFFF3344
    const val NEON_PURPLE: Long = 0xFFB44DFF
    const val NEON_WHITE: Long = 0xFFE0E0FF
    const val TELEPORTER_COLOR: Long = 0xFFFF66FF

    const val WHITE: Long = 0xFFFFFFFF
    const val GRID_COLOR: Long = 0x08FFFFFF // ~3% white

    // Enemy color mapping
    val ENEMY_COLORS = mapOf(
        EnemyType.BULLET to NEON_PINK,
        EnemyType.SEEKER to NEON_ORANGE,
        EnemyType.WAVE to NEON_YELLOW,
        EnemyType.SPIRAL to NEON_PURPLE,
        EnemyType.SPLITTER to NEON_WHITE,
        EnemyType.BOMBER to NEON_RED,
        EnemyType.TELEPORTER to TELEPORTER_COLOR,
        EnemyType.LASER to NEON_CYAN
    )

    fun colorToRgb(color: Long): Triple<Int, Int, Int> {
        val r = ((color shr 16) and 0xFF).toInt()
        val g = ((color shr 8) and 0xFF).toInt()
        val b = (color and 0xFF).toInt()
        return Triple(r, g, b)
    }
}
