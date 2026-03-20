package io.dodge.network

import kotlinx.serialization.Serializable

@Serializable
data class SubmitScoreRequest(
    val deviceId: String,
    val displayName: String,
    val date: String,
    val score: Int,
    val survivalTime: Int
)

@Serializable
data class LeaderboardEntry(
    val id: String = "",
    val deviceId: String = "",
    val displayName: String = "",
    val date: String = "",
    val score: Int = 0,
    val survivalTime: Int = 0,
    val createdAt: String? = null
)
