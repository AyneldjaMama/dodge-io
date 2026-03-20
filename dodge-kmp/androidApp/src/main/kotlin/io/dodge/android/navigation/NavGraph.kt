package io.dodge.android.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import io.dodge.android.storage.DataStoreStatsRepo
import io.dodge.android.ui.game.GameScreen
import io.dodge.android.ui.leaderboard.LeaderboardScreen
import io.dodge.android.ui.menu.MenuScreen
import io.dodge.android.ui.stats.StatsScreen
import io.dodge.model.GameMode

object Routes {
    const val MENU = "menu"
    const val GAME = "game/{mode}"
    const val STATS = "stats"
    const val LEADERBOARD = "leaderboard"

    fun game(mode: GameMode) = "game/${mode.name.lowercase()}"
}

@Composable
fun DodgeNavGraph(
    navController: NavHostController,
    statsRepo: DataStoreStatsRepo
) {
    NavHost(navController = navController, startDestination = Routes.MENU) {
        composable(Routes.MENU) {
            MenuScreen(
                statsRepo = statsRepo,
                onPlayArcade = { navController.navigate(Routes.game(GameMode.ARCADE)) },
                onPlayDaily = { navController.navigate(Routes.game(GameMode.DAILY)) },
                onOpenStats = { navController.navigate(Routes.STATS) },
                onOpenLeaderboard = { navController.navigate(Routes.LEADERBOARD) }
            )
        }

        composable(Routes.GAME) { backStackEntry ->
            val modeStr = backStackEntry.arguments?.getString("mode") ?: "arcade"
            val mode = if (modeStr == "daily") GameMode.DAILY else GameMode.ARCADE
            GameScreen(
                mode = mode,
                statsRepo = statsRepo,
                onNavigateBack = { navController.popBackStack() },
                onOpenLeaderboard = { navController.navigate(Routes.LEADERBOARD) }
            )
        }

        composable(Routes.STATS) {
            StatsScreen(
                statsRepo = statsRepo,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.LEADERBOARD) {
            LeaderboardScreen(
                statsRepo = statsRepo,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
