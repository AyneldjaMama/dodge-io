package io.dodge.android.ui.game

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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.dodge.android.ui.theme.*
import kotlinx.coroutines.delay

private data class FakeVideoAd(
    val title: String,
    val subtitle: String,
    val ctaText: String,
    val accentColor: Color
)

private val fakeVideoAds = listOf(
    FakeVideoAd("PUZZLE QUEST", "Download Free — 4.8★", "INSTALL NOW", Color(0xFF4CAF50)),
    FakeVideoAd("BATTLE ARENA", "Join 10M+ Players", "PLAY FREE", Color(0xFFFF5722)),
    FakeVideoAd("WORD MASTER", "Train Your Brain Daily", "GET IT FREE", Color(0xFF2196F3)),
    FakeVideoAd("SPEED RACER", "New Season Available!", "DOWNLOAD", Color(0xFFFF9800)),
    FakeVideoAd("CANDY BLAST", "#1 Puzzle Game 2026", "PLAY NOW", Color(0xFFE91E63))
)

@Composable
fun SimulatedAdOverlay(
    visible: Boolean,
    durationSeconds: Int = 5,
    onComplete: () -> Unit
) {
    if (!visible) return

    val ad = remember { fakeVideoAds.random() }
    var elapsedMs by remember { mutableIntStateOf(0) }
    val totalMs = durationSeconds * 1000
    val canClose = elapsedMs >= totalMs
    val progress = (elapsedMs.toFloat() / totalMs).coerceIn(0f, 1f)
    val remainingSeconds = ((totalMs - elapsedMs) / 1000) + 1

    // Animate elapsed time
    LaunchedEffect(visible) {
        elapsedMs = 0
        while (elapsedMs < totalMs) {
            delay(50)
            elapsedMs += 50
        }
    }

    // Fake "animation" shimmer
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    val shimmerOffset by infiniteTransition.animateFloat(
        initialValue = -1f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Fake video content area
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 140.dp),
            contentAlignment = Alignment.Center
        ) {
            // Dark gradient background simulating video
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF1A1A2E),
                                ad.accentColor.copy(alpha = 0.15f),
                                Color(0xFF1A1A2E)
                            )
                        )
                    )
            )

            // Animated "game footage" placeholder
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Pulsing game icon
                val pulseScale by infiniteTransition.animateFloat(
                    initialValue = 0.9f,
                    targetValue = 1.1f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(800, easing = EaseInOutCubic),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "pulse"
                )

                Box(
                    modifier = Modifier
                        .size((80 * pulseScale).dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(ad.accentColor),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = ad.title.take(1),
                        color = Color.White,
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = ad.title,
                    color = Color.White,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = ad.subtitle,
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 16.sp
                )

                Spacer(modifier = Modifier.height(24.dp))

                // CTA button
                Button(
                    onClick = { /* fake — does nothing */ },
                    colors = ButtonDefaults.buttonColors(containerColor = ad.accentColor),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Text(
                        text = ad.ctaText,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }
            }
        }

        // Top bar with countdown / close
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Ad", color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp)

            if (canClose) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.2f))
                        .clickable { onComplete() },
                    contentAlignment = Alignment.Center
                ) {
                    Text("✕", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            } else {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("$remainingSeconds", color = Color.White.copy(alpha = 0.7f), fontSize = 14.sp)
                }
            }
        }

        // Bottom progress bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .align(Alignment.BottomCenter)
                .background(Color.White.copy(alpha = 0.1f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(progress)
                    .background(ad.accentColor)
            )
        }
    }
}
