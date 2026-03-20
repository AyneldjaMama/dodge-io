package io.dodge.android.ui.stats

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.dodge.android.storage.DataStoreStatsRepo
import io.dodge.android.ui.theme.*
import io.dodge.model.GameStats

@Composable
fun StatsScreen(
    statsRepo: DataStoreStatsRepo,
    onBack: () -> Unit
) {
    var stats by remember { mutableStateOf(GameStats()) }

    LaunchedEffect(Unit) {
        stats = statsRepo.loadStats()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .statusBarsPadding()
            .padding(16.dp)
    ) {
        // Header
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Text("←", color = TextPrimary, fontSize = 24.sp)
            }
            Text("STATS", color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Hero card: High Score
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("HIGH SCORE", color = TextMuted, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                Text(
                    text = "${stats.highScore}",
                    color = NeonGreen,
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Best time: ${stats.bestTime}s",
                    color = TextSecondary,
                    fontSize = 14.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Gameplay section
        Text("GAMEPLAY", color = TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Medium)
        Spacer(modifier = Modifier.height(8.dp))

        // 2x2 grid
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StatCard("🎮", "Games\nPlayed", "${stats.totalGames}", NeonGreen, Modifier.weight(1f))
            StatCard("⚡", "Near\nMisses", "${stats.totalNearMisses}", NeonYellow, Modifier.weight(1f))
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StatCard("🛡️", "Shields\nUsed", "${stats.totalShieldsUsed}", NeonCyan, Modifier.weight(1f))
            StatCard("▶️", "Ads\nWatched", "${stats.totalAdsWatched}", NeonPink, Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Daily challenge section
        Text("DAILY CHALLENGE", color = TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Medium)
        Spacer(modifier = Modifier.height(8.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Surface)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🔥", fontSize = 20.sp)
                    Text("${stats.dailyChallengeStreak}", color = NeonOrange, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    Text("Streak", color = TextMuted, fontSize = 10.sp)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("⭐", fontSize = 20.sp)
                    Text("${stats.dailyHighScore}", color = TextPrimary, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    Text("Today's Best", color = TextMuted, fontSize = 10.sp)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("⏱", fontSize = 20.sp)
                    Text("${stats.dailyBestTime}s", color = TextPrimary, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    Text("Today's Time", color = TextMuted, fontSize = 10.sp)
                }
            }
        }
    }
}

@Composable
private fun StatCard(
    icon: String,
    label: String,
    value: String,
    valueColor: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(icon, fontSize = 20.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Text(value, color = valueColor, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Text(
                label, color = TextMuted, fontSize = 10.sp,
                textAlign = TextAlign.Center, lineHeight = 14.sp
            )
        }
    }
}
