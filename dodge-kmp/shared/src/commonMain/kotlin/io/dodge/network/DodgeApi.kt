package io.dodge.network

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

object DodgeApi {
    // Set this at app startup
    var baseUrl: String = ""

    private val client = HttpClient {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
            })
        }
    }

    suspend fun submitDailyScore(request: SubmitScoreRequest): LeaderboardEntry {
        return client.post("$baseUrl/api/daily-scores") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
    }

    suspend fun getDailyLeaderboard(date: String): List<LeaderboardEntry> {
        return client.get("$baseUrl/api/daily-scores/$date").body()
    }

    suspend fun getDeviceBest(date: String, deviceId: String): LeaderboardEntry? {
        return try {
            client.get("$baseUrl/api/daily-scores/$date/device/$deviceId").body()
        } catch (_: Exception) {
            null
        }
    }
}
