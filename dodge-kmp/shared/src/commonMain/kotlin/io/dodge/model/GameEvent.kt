package io.dodge.model

import io.dodge.audio.SoundEffect

sealed interface GameEvent {
    data class ScoreUpdate(val score: Int, val time: Int) : GameEvent
    data class PlayerDied(val score: Int, val time: Int, val isDaily: Boolean) : GameEvent
    data object PlayerRespawned : GameEvent
    data object GameStarted : GameEvent
    data class NearMiss(val streak: Int, val count: Int) : GameEvent
    data class PowerUpCollected(val type: PowerUpType) : GameEvent
    data object ShieldBreak : GameEvent
    data class PlaySound(val sfx: SoundEffect) : GameEvent
    data class ScreenShake(val magnitude: Float) : GameEvent
}
