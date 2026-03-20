package io.dodge.engine

import io.dodge.model.PowerUpType

class PowerUp(
    var x: Float,
    var y: Float,
    val radius: Float,
    val type: PowerUpType,
    var age: Int = 0,
    val maxAge: Int
)
