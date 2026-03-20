package io.dodge.android.ui.leaderboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import io.dodge.network.DodgeApi
import io.dodge.network.LeaderboardEntry
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun LeaderboardScreen(
    statsRepo: DataStoreStatsRepo,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val today = remember { LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE) }

    var deviceId by remember { mutableStateOf("") }
    var displayName by remember { mutableStateOf("") }
    var editingName by remember { mutableStateOf(false) }
    var nameInput by remember { mutableStateOf("") }
    var entries by remember { mutableStateOf<List<LeaderboardEntry>>(emptyList()) }
    var personalBest by remember { mutableStateOf<LeaderboardEntry?>(null) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        deviceId = statsRepo.getDeviceId()
        displayName = statsRepo.getDisplayName()
        nameInput = displayName
        try {
            entries = DodgeApi.getDailyLeaderboard(today)
            personalBest = DodgeApi.getDeviceBest(today, deviceId)
        } catch (_: Exception) {}
        loading = false
    }

    // Auto-refresh every 30s
    LaunchedEffect(Unit) {
        while (true) {
            delay(30_000)
            try {
                entries = DodgeApi.getDailyLeaderboard(today)
            } catch (_: Exception) {}
        }
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
            Text("LEADERBOARD", color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }

        // Date
        Text(
            text = today,
            color = TextMuted,
            fontSize = 12.sp,
            modifier = Modifier.padding(start = 48.dp, bottom = 8.dp)
        )

        // Player name
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (editingName) {
                OutlinedTextField(
                    value = nameInput,
                    onValueChange = { if (it.length <= 32) nameInput = it },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = NeonCyan,
                        cursorColor = NeonCyan
                    )
                )
                IconButton(onClick = {
                    editingName = false
                    displayName = nameInput
                    scope.launch { statsRepo.setDisplayName(nameInput) }
                }) {
                    Text("✓", color = NeonGreen, fontSize = 20.sp)
                }
            } else {
                Text(displayName, color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                IconButton(onClick = {
                    editingName = true
                    nameInput = displayName
                }) {
                    Text("✏️", fontSize = 16.sp)
                }
            }
        }

        // Personal best card
        if (personalBest != null) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Surface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("YOUR BEST TODAY", color = TextMuted, fontSize = 10.sp)
                        Text("${personalBest?.score}", color = NeonGreen, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                        Text("${personalBest?.survivalTime}s", color = TextSecondary, fontSize = 12.sp)
                    }
                    val rank = entries.indexOfFirst { it.deviceId == deviceId } + 1
                    if (rank > 0) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = when (rank) {
                                    1 -> "🥇"
                                    2 -> "🥈"
                                    3 -> "🥉"
                                    else -> "#$rank"
                                },
                                fontSize = if (rank <= 3) 28.sp else 20.sp,
                                color = TextPrimary
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Leaderboard list
        if (loading) {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = NeonCyan)
            }
        } else if (entries.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth().padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("No scores yet today", color = TextMuted, fontSize = 14.sp)
            }
        } else {
            LazyColumn {
                itemsIndexed(entries) { index, entry ->
                    val rank = index + 1
                    val isMe = entry.deviceId == deviceId
                    val bgColor = when {
                        isMe -> SurfaceLight
                        rank <= 3 -> Surface
                        else -> Background
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(bgColor, RoundedCornerShape(8.dp))
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Rank
                        Text(
                            text = when (rank) {
                                1 -> "🥇"
                                2 -> "🥈"
                                3 -> "🥉"
                                else -> "$rank"
                            },
                            fontSize = if (rank <= 3) 20.sp else 14.sp,
                            color = TextMuted,
                            modifier = Modifier.width(40.dp),
                            textAlign = TextAlign.Center
                        )

                        // Name
                        Column(modifier = Modifier.weight(1f)) {
                            Row {
                                Text(
                                    text = entry.displayName,
                                    color = if (isMe) NeonGreen else TextPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = if (isMe) FontWeight.Bold else FontWeight.Normal
                                )
                                if (isMe) {
                                    Text(" (you)", color = NeonGreen, fontSize = 12.sp)
                                }
                            }
                        }

                        // Score + time
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "${entry.score}",
                                color = if (rank <= 3) NeonYellow else TextPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${entry.survivalTime}s",
                                color = TextMuted,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
