package io.dodge.android.ui.game

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import io.dodge.android.ads.AdMobManager
import io.dodge.android.game.GameSurfaceView
import io.dodge.android.storage.DataStoreStatsRepo
import io.dodge.android.ui.theme.*
import io.dodge.model.GameEvent
import io.dodge.model.GameMode
import io.dodge.model.GameStats
import io.dodge.model.PowerUpType
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

enum class GameState { READY, PLAYING, CONTINUE, DEAD }

@Composable
fun GameScreen(
    mode: GameMode,
    statsRepo: DataStoreStatsRepo,
    onNavigateBack: () -> Unit,
    onOpenLeaderboard: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var gameState by remember { mutableStateOf(GameState.READY) }
    var currentScore by remember { mutableIntStateOf(0) }
    var finalScore by remember { mutableIntStateOf(0) }
    var finalTime by remember { mutableIntStateOf(0) }
    var isNewHigh by remember { mutableStateOf(false) }
    var countdown by remember { mutableIntStateOf(5) }
    var continueUsed by remember { mutableStateOf(false) }
    var stats by remember { mutableStateOf(GameStats()) }

    val dailySeed = remember {
        if (mode == GameMode.DAILY) LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE) else null
    }

    var surfaceView by remember { mutableStateOf<GameSurfaceView?>(null) }

    // AdMob
    val adMobManager = remember {
        (context as? Activity)?.let { AdMobManager(it) }
    }
    val adAvailable by adMobManager?.adAvailable?.collectAsState() ?: remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        stats = statsRepo.loadStats()
        adMobManager?.initialize()
    }

    // Countdown timer for continue screen
    LaunchedEffect(gameState) {
        if (gameState == GameState.CONTINUE) {
            countdown = 5
            while (countdown > 0) {
                delay(1000)
                countdown -= 1
            }
            if (gameState == GameState.CONTINUE) {
                recordDeath(statsRepo, mode, finalScore, finalTime) { updated, newHigh ->
                    stats = updated
                    isNewHigh = newHigh
                }
                gameState = GameState.DEAD
            }
        }
    }

    fun handleGameEvent(event: GameEvent) {
        when (event) {
            is GameEvent.ScoreUpdate -> {
                currentScore = event.score
            }
            is GameEvent.PlayerDied -> {
                finalScore = event.score
                finalTime = event.time
                currentScore = event.score

                if (continueUsed) {
                    scope.launch {
                        recordDeath(statsRepo, mode, event.score, event.time) { updated, newHigh ->
                            stats = updated
                            isNewHigh = newHigh
                        }
                    }
                    gameState = GameState.DEAD
                } else {
                    gameState = GameState.CONTINUE
                }
            }
            is GameEvent.NearMiss -> {
                scope.launch {
                    val s = statsRepo.loadStats()
                    statsRepo.saveStats(s.copy(totalNearMisses = s.totalNearMisses + event.count))
                }
            }
            is GameEvent.PowerUpCollected -> {
                if (event.type == PowerUpType.SHIELD) {
                    scope.launch {
                        val s = statsRepo.loadStats()
                        statsRepo.saveStats(s.copy(totalShieldsUsed = s.totalShieldsUsed + 1))
                    }
                }
            }
            is GameEvent.GameStarted -> {
                gameState = GameState.PLAYING
                currentScore = 0
            }
            else -> {}
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Game SurfaceView
        AndroidView(
            factory = { ctx ->
                GameSurfaceView(ctx, mode, dailySeed) { event ->
                    handleGameEvent(event)
                }.also { surfaceView = it }
            },
            modifier = Modifier.fillMaxSize()
        )

        // HUD overlay (during play)
        if (gameState == GameState.PLAYING) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onNavigateBack) {
                    Text("←", color = Color.White, fontSize = 24.sp)
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    if (mode == GameMode.DAILY) {
                        Text("DAILY", color = NeonCyan, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                    Text(
                        text = "$currentScore",
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.width(48.dp))
            }
        }

        // Continue overlay
        if (gameState == GameState.CONTINUE) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.7f)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "$countdown",
                        color = Color.White,
                        fontSize = 64.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = {
                            // Show real ad, respawn on reward
                            val adShown = adMobManager?.showAd {
                                // onRewarded callback — runs on main thread
                                continueUsed = true
                                surfaceView?.sendRespawn()
                                gameState = GameState.PLAYING
                                scope.launch {
                                    val s = statsRepo.loadStats()
                                    statsRepo.saveStats(s.copy(totalAdsWatched = s.totalAdsWatched + 1))
                                }
                            } ?: false

                            if (!adShown) {
                                // No ad available — just respawn (same as simulated ad)
                                continueUsed = true
                                surfaceView?.sendRespawn()
                                gameState = GameState.PLAYING
                                scope.launch {
                                    val s = statsRepo.loadStats()
                                    statsRepo.saveStats(s.copy(totalAdsWatched = s.totalAdsWatched + 1))
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonGreen),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.padding(horizontal = 32.dp)
                    ) {
                        Text(
                            text = "WATCH AD TO CONTINUE",
                            color = Color.Black,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "No Thanks",
                        color = TextMuted,
                        fontSize = 14.sp,
                        modifier = Modifier.clickable {
                            scope.launch {
                                recordDeath(statsRepo, mode, finalScore, finalTime) { updated, newHigh ->
                                    stats = updated
                                    isNewHigh = newHigh
                                }
                            }
                            gameState = GameState.DEAD
                        }
                    )
                }
            }
        }

        // Death overlay
        if (gameState == GameState.DEAD) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.8f)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(32.dp)
                ) {
                    if (mode == GameMode.ARCADE) {
                        Text("DESTROYED", color = NeonRed, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                    } else {
                        Text("DAILY CHALLENGE", color = NeonCyan, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Text(
                            text = dailySeed ?: "",
                            color = TextMuted,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = "$finalScore",
                        color = Color.White,
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = "${finalTime}s survived",
                        color = TextSecondary,
                        fontSize = 16.sp
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    if (isNewHigh && mode == GameMode.ARCADE) {
                        Text(
                            text = "NEW HIGH SCORE!",
                            color = NeonYellow,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    } else if (mode == GameMode.ARCADE && stats.highScore > 0) {
                        Text(
                            text = "Best: ${stats.highScore}",
                            color = TextMuted,
                            fontSize = 14.sp
                        )
                    }

                    if (mode == GameMode.DAILY && stats.dailyChallengeStreak > 1) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "\uD83D\uDD25 ${stats.dailyChallengeStreak} day streak",
                            color = NeonOrange,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                gameState = GameState.READY
                                continueUsed = false
                                isNewHigh = false
                                surfaceView?.getEngine()?.start()
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonGreen)
                        ) {
                            Text("RETRY", fontWeight = FontWeight.Bold)
                        }

                        if (mode == GameMode.DAILY) {
                            OutlinedButton(
                                onClick = onOpenLeaderboard,
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonCyan)
                            ) {
                                Text("LEADERBOARD", fontWeight = FontWeight.Bold)
                            }
                        }

                        OutlinedButton(
                            onClick = onNavigateBack,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary)
                        ) {
                            Text("MENU", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

private suspend fun recordDeath(
    statsRepo: DataStoreStatsRepo,
    mode: GameMode,
    score: Int,
    time: Int,
    onResult: (GameStats, Boolean) -> Unit
) {
    val s = statsRepo.loadStats()
    val newHigh = score > s.highScore
    val updated = if (mode == GameMode.ARCADE) {
        s.copy(
            totalGames = s.totalGames + 1,
            highScore = maxOf(s.highScore, score),
            bestTime = maxOf(s.bestTime, time)
        )
    } else {
        val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
        val streak = if (s.lastDailyDate == today) s.dailyChallengeStreak
        else if (isYesterday(s.lastDailyDate)) s.dailyChallengeStreak + 1
        else 1
        s.copy(
            totalGames = s.totalGames + 1,
            dailyHighScore = maxOf(s.dailyHighScore, score),
            dailyBestTime = maxOf(s.dailyBestTime, time),
            dailyChallengeStreak = streak,
            lastDailyDate = today
        )
    }
    statsRepo.saveStats(updated)
    onResult(updated, newHigh)
}

private fun isYesterday(dateStr: String): Boolean {
    if (dateStr.isBlank()) return false
    return try {
        val yesterday = LocalDate.now().minusDays(1).format(DateTimeFormatter.ISO_LOCAL_DATE)
        dateStr == yesterday
    } catch (e: Exception) {
        false
    }
}
