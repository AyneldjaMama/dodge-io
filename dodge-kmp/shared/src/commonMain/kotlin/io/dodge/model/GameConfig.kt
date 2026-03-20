package io.dodge.model

object GameConfig {
    // Player
    const val PLAYER_RADIUS = 14f
    const val PLAYER_RADIUS_SHRUNK = 8f
    const val PLAYER_SPEED_FACTOR = 0.15f
    const val PLAYER_MAX_SPEED = 12f
    const val TOUCH_OFFSET = 120f
    const val TRAIL_LENGTH = 20

    // Play area
    const val BOTTOM_SAFE = 120f

    // Near-miss
    const val NEAR_MISS_ZONE = 20f
    const val NEAR_MISS_COOLDOWN = 10
    const val NEAR_MISS_STREAK_TIMEOUT = 120

    // Power-ups
    const val POWER_UP_SPAWN_INTERVAL = 600
    const val POWER_UP_DURATION = 300
    const val POWER_UP_MAX_AGE = 400
    const val POWER_UP_RADIUS = 10f

    // Power-up effects
    const val SLOW_FACTOR = 0.4f
    const val SHIELD_KNOCKBACK_RADIUS = 120f
    const val SHIELD_KNOCKBACK_FORCE = 8f

    // Rendering
    const val FRAME_CAP_MS = 14L
    const val MAX_PARTICLES = 200
    const val GRID_SPACING = 50f

    // Difficulty
    const val DIFFICULTY_SCALE = 600f // difficulty = 1 + time / 600
    const val SPAWN_RATE_BASE = 90f
    const val SPAWN_RATE_REDUCTION = 1.5f
    const val SPAWN_RATE_MIN = 30f
    const val SPEED_MULTIPLIER_START_SEC = 60f
    const val SPEED_MULTIPLIER_TIER_SEC = 10f
    const val SPEED_MULTIPLIER_BASE = 1.2f
    const val EARLY_SLOWDOWN_DURATION = 30f
    const val EARLY_SLOWDOWN_MIN = 0.4f

    // Score
    const val SCORE_BASE = 0.02f
    const val SCORE_DIFFICULTY_FACTOR = 0.005f
    const val NEAR_MISS_SCORE_PER_STREAK = 5
    const val NEAR_MISS_MULTIPLIER_PER_STREAK = 0.3f

    // Invulnerability
    const val IFRAMES_RESPAWN = 180
    const val IFRAMES_SHIELD_BREAK = 30

    // Death
    const val DEATH_DELAY_FRAMES = 36 // 600ms at 60fps
    const val DEATH_PARTICLES_GREEN = 60
    const val DEATH_PARTICLES_WHITE = 30
    const val DEATH_SCREEN_SHAKE = 15f
    const val DEATH_FLASH = 5

    // Enemy unlock times (in seconds)
    const val UNLOCK_BULLET = 0f
    const val UNLOCK_SEEKER = 20f
    const val UNLOCK_WAVE = 45f
    const val UNLOCK_SPIRAL = 70f
    const val UNLOCK_SPLITTER = 100f
    const val UNLOCK_BOMBER = 135f
    const val UNLOCK_TELEPORTER = 175f
    const val UNLOCK_LASER = 220f

    // Bullet
    const val BULLET_BASE_SPEED = 0.7f
    const val BULLET_DIFFICULTY_SPEED = 0.05f
    const val BULLET_RANDOM_SPEED = 0.6f
    const val BULLET_MIN_RADIUS = 5f
    const val BULLET_RANDOM_RADIUS = 4f
    const val BULLET_MAX_AGE = 600

    // Seeker
    const val SEEKER_INITIAL_SPEED_FACTOR = 0.3f
    const val SEEKER_ACCEL = 0.015f
    const val SEEKER_BASE_MAX_SPEED = 1.2f
    const val SEEKER_DIFFICULTY_MAX_SPEED = 0.02f
    const val SEEKER_RADIUS = 7f
    const val SEEKER_MAX_AGE = 300

    // Wave
    const val WAVE_SPEED_FACTOR = 0.6f
    const val WAVE_MIN_AMPLITUDE = 40f
    const val WAVE_RANDOM_AMPLITUDE = 60f
    const val WAVE_MIN_FREQUENCY = 0.03f
    const val WAVE_RANDOM_FREQUENCY = 0.03f
    const val WAVE_RADIUS = 6f

    // Spiral
    const val SPIRAL_SPEED_FACTOR = 0.6f
    const val SPIRAL_MIN_SPEED = 0.08f
    const val SPIRAL_RANDOM_SPEED = 0.04f
    const val SPIRAL_MIN_RADIUS = 20f
    const val SPIRAL_RANDOM_RADIUS = 30f
    const val SPIRAL_RADIUS_GROWTH = 0.05f
    const val SPIRAL_RADIUS = 6f
    const val SPIRAL_MAX_AGE = 400

    // Splitter
    const val SPLITTER_SPEED_FACTOR = 0.8f
    const val SPLITTER_RADIUS = 10f
    const val SPLITTER_MAX_AGE = 180
    const val SPLITTER_CHILD_COUNT = 4
    const val SPLITTER_CHILD_SPEED = 2.5f
    const val SPLITTER_CHILD_RADIUS = 4f

    // Bomber
    const val BOMBER_SPEED_FACTOR = 0.7f
    const val BOMBER_RADIUS = 12f
    const val BOMBER_MAX_AGE = 120
    const val BOMBER_CHILD_COUNT = 12
    const val BOMBER_CHILD_SPEED = 2.5f
    const val BOMBER_CHILD_RADIUS = 4f
    const val BOMBER_SCREEN_SHAKE = 5f

    // Teleporter
    const val TELEPORTER_SPEED_FACTOR = 0.4f
    const val TELEPORTER_RADIUS = 7f
    const val TELEPORTER_MAX_AGE = 360
    const val TELEPORTER_MIN_COOLDOWN = 60
    const val TELEPORTER_RANDOM_COOLDOWN = 40
    const val TELEPORTER_MIN_DISTANCE = 60f
    const val TELEPORTER_RANDOM_DISTANCE = 80f
    const val TELEPORTER_BASE_SPEED = 1.0f
    const val TELEPORTER_DIFFICULTY_SPEED = 0.04f

    // Laser
    const val LASER_CHARGE_FRAMES = 30
    const val LASER_FIRE_FRAMES = 10
    const val LASER_MAX_AGE = 90
    const val LASER_INITIAL_WIDTH = 2f
    const val LASER_WIDTH_GROWTH = 3f
    const val LASER_MAX_WIDTH = 50f
    const val LASER_DECAY_RATE = 2f
    const val LASER_HIT_MIN_WIDTH = 5f

    // Spawn margin
    const val SPAWN_MARGIN = 20f
    const val ENEMY_OFFSCREEN_MARGIN = 100f
}
