package io.dodge.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import io.dodge.android.navigation.DodgeNavGraph
import io.dodge.android.storage.DataStoreStatsRepo
import io.dodge.android.ui.theme.Background
import io.dodge.android.ui.theme.DodgeTheme

class MainActivity : ComponentActivity() {
    private lateinit var statsRepo: DataStoreStatsRepo

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        statsRepo = DataStoreStatsRepo(applicationContext)

        setContent {
            DodgeTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Background
                ) {
                    val navController = rememberNavController()
                    DodgeNavGraph(
                        navController = navController,
                        statsRepo = statsRepo
                    )
                }
            }
        }
    }
}
