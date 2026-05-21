package com.snapword.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.snapword.ui.detail.DetailScreen
import com.snapword.ui.home.HomeScreen
import com.snapword.ui.result.ResultScreen
import com.snapword.ui.review.ReviewScreen
import com.snapword.ui.settings.SettingsScreen
import com.snapword.ui.vocab.VocabScreen
import com.snapword.ui.wordlist.WordListScreen
import java.net.URLDecoder
import java.net.URLEncoder

object Routes {
    const val HOME = "home"
    const val RESULT = "result/{words}"
    const val DETAIL = "detail/{word}"
    const val VOCAB = "vocab"
    const val REVIEW = "review"
    const val SETTINGS = "settings"
    const val WORDLIST = "wordlist"

    fun result(words: List<String>): String {
        val encoded = URLEncoder.encode(words.joinToString(","), "UTF-8")
        return "result/$encoded"
    }

    fun detail(word: String): String {
        val encoded = URLEncoder.encode(word, "UTF-8")
        return "detail/$encoded"
    }

    fun decodeWords(encoded: String): List<String> {
        return URLDecoder.decode(encoded, "UTF-8").split(",").filter { it.isNotBlank() }
    }

    fun decodeWord(encoded: String): String {
        return URLDecoder.decode(encoded, "UTF-8")
    }
}

data class BottomNavItem(
    val route: String,
    val label: String,
    val icon: ImageVector
)

val bottomNavItems = listOf(
    BottomNavItem(Routes.HOME, "拍照", Icons.Default.CameraAlt),
    BottomNavItem(Routes.VOCAB, "生词本", Icons.Default.Book),
    BottomNavItem(Routes.SETTINGS, "设置", Icons.Default.Settings)
)

@Composable
fun NavGraph() {
    val navController = rememberNavController()

    Scaffold(
        bottomBar = { BottomNavBar(navController) }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Routes.HOME,
            modifier = Modifier.padding(padding)
        ) {
            composable(Routes.HOME) {
                HomeScreen(
                    onWordsReady = { words: List<String> ->
                        navController.navigate(Routes.result(words))
                    }
                )
            }

            composable(
                route = Routes.RESULT,
                arguments = listOf(navArgument("words") { type = NavType.StringType })
            ) { backStackEntry ->
                val encoded = backStackEntry.arguments?.getString("words") ?: ""
                val words = remember(encoded) { Routes.decodeWords(encoded) }
                ResultScreen(
                    words = words,
                    onWordClick = { word: String ->
                        navController.navigate(Routes.detail(word))
                    },
                    onBack = { navController.popBackStack() }
                )
            }

            composable(
                route = Routes.DETAIL,
                arguments = listOf(navArgument("word") { type = NavType.StringType })
            ) { backStackEntry ->
                val encoded = backStackEntry.arguments?.getString("word") ?: ""
                val word = remember(encoded) { Routes.decodeWord(encoded) }
                DetailScreen(
                    word = word,
                    onBack = { navController.popBackStack() }
                )
            }

            composable(Routes.VOCAB) {
                VocabScreen(
                    onReviewClick = { navController.navigate(Routes.REVIEW) },
                    onWordClick = { word: String ->
                        navController.navigate(Routes.detail(word))
                    }
                )
            }

            composable(Routes.REVIEW) {
                ReviewScreen(
                    onBack = { navController.popBackStack() }
                )
            }

            composable(Routes.SETTINGS) {
                SettingsScreen(
                    onOpenWordList = { navController.navigate(Routes.WORDLIST) }
                )
            }

            composable(Routes.WORDLIST) {
                WordListScreen(
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}

@Composable
private fun BottomNavBar(navController: NavHostController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val mainRoutes = bottomNavItems.map { it.route }.toSet()
    if (currentRoute !in mainRoutes) return

    NavigationBar {
        bottomNavItems.forEach { item: BottomNavItem ->
            NavigationBarItem(
                icon = { Icon(item.icon, contentDescription = item.label) },
                label = { Text(item.label) },
                selected = currentRoute == item.route,
                onClick = {
                    if (currentRoute != item.route) {
                        navController.navigate(item.route) {
                            popUpTo(Routes.HOME) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                }
            )
        }
    }
}
