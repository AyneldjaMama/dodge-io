package io.dodge.android.ui.game

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.dodge.android.ui.theme.*
import kotlinx.coroutines.delay

private data class SampleAd(
    val headline: String,
    val body: String,
    val url: String
)

private val sampleAds = listOf(
    SampleAd("Level Up Your Skills", "Download the #1 brain training app today", "braintrainer.example.com"),
    SampleAd("Pizza Deals Near You", "50% off your first order with code DODGE", "pizzaplace.example.com"),
    SampleAd("Learn to Code", "Start your coding journey — free for 7 days", "codeacademy.example.com"),
    SampleAd("Fast Mobile Games", "Discover trending games this week", "gamestore.example.com"),
    SampleAd("Music Unlimited", "Stream 100M+ songs. Try free for 30 days", "musicapp.example.com")
)

@Composable
fun SimulatedAdOverlay(
    visible: Boolean,
    durationSeconds: Int = 5,
    onComplete: () -> Unit
) {
    if (!visible) return

    val ad = remember { sampleAds.random() }
    var countdown by remember { mutableIntStateOf(durationSeconds) }
    var canClose by remember { mutableStateOf(false) }

    LaunchedEffect(visible) {
        countdown = durationSeconds
        canClose = false
        while (countdown > 0) {
            delay(1000)
            countdown -= 1
        }
        canClose = true
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xF0000000))
    ) {
        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF1A1A2E))
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Ad",
                color = TextMuted,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
            if (canClose) {
                Text(
                    text = "✕ Close",
                    color = NeonCyan,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { onComplete() }
                )
            } else {
                Text(
                    text = "Closes in ${countdown}s",
                    color = TextMuted,
                    fontSize = 12.sp
                )
            }
        }

        // Ad card
        Card(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(32.dp)
                .fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Text(
                    text = "Sponsored",
                    color = Color.Gray,
                    fontSize = 10.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = ad.headline,
                    color = Color.Black,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = ad.body,
                    color = Color.DarkGray,
                    fontSize = 14.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = ad.url,
                    color = Color(0xFF1A73E8),
                    fontSize = 12.sp
                )
            }
        }
    }
}
