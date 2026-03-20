package io.dodge.android.ui.menu

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.dodge.android.storage.DataStoreStatsRepo
import io.dodge.android.ui.theme.*
import io.dodge.model.GameStats
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun MenuScreen(
    statsRepo: DataStoreStatsRepo,
    onPlayArcade: () -> Unit,
    onPlayDaily: () -> Unit,
    onOpenStats: () -> Unit,
    onOpenLeaderboard: () -> Unit
) {
    var stats by remember { mutableStateOf(GameStats()) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        stats = statsRepo.loadStats()
    }

    val today = remember { LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE) }
    val hasDoneToday = stats.lastDailyDate == today

    // Pulse animation for play button
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val playScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.03f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "playPulse"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
    ) {
        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .statusBarsPadding(),
            horizontalArrangement = Arrangement.End
        ) {
            IconButton(onClick = onOpenLeaderboard) {
                Text("🏆", fontSize = 24.sp)
            }
            IconButton(onClick = onOpenStats) {
                Text("📊", fontSize = 24.sp)
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Title
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = "DODGE",
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Bold,
                    color = NeonGreen
                )
                Text(
                    text = ".IO",
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Bold,
                    color = NeonPink
                )
            }

            Text(
                text = "dodge everything. survive.",
                color = TextMuted,
                fontSize = 14.sp,
                modifier = Modifier.padding(top = 8.dp)
            )

            Spacer(modifier = Modifier.height(48.dp))

            // PLAY button
            Button(
                onClick = onPlayArcade,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .scale(playScale),
                colors = ButtonDefaults.buttonColors(containerColor = NeonGreen),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "PLAY",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // DAILY CHALLENGE button
            OutlinedButton(
                onClick = onPlayDaily,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonCyan),
                border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(
                    width = 1.dp
                )
            ) {
                Text(
                    text = "DAILY CHALLENGE",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = NeonCyan
                )
                if (hasDoneToday) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("✓", color = NeonGreen, fontSize = 16.sp)
                }
            }

            // High score
            if (stats.highScore > 0) {
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "HIGH SCORE",
                    color = TextMuted,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "${stats.highScore}",
                    color = NeonGreen,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(48.dp))

            // Threat legend
            Text(
                text = "THREATS",
                color = TextMuted,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            val threats = listOf(
                "Bullet" to NeonPink,
                "Seeker" to NeonOrange,
                "Wave" to NeonYellow,
                "Spiral" to NeonPurple,
                "Splitter" to NeonWhite,
                "Bomber" to NeonRed,
                "Teleporter" to TeleporterColor,
                "Laser" to NeonCyan
            )

            // 2x4 grid
            for (row in threats.chunked(4)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    for ((name, color) in row) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .clip(CircleShape)
                                    .background(color)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = name,
                                color = TextMuted,
                                fontSize = 9.sp
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "near misses = bonus points + streak multiplier",
                color = TextMuted,
                fontSize = 10.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}
