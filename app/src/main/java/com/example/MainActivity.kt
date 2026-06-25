package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.JournalViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: JournalViewModel = viewModel()
            val themeMode by viewModel.themeMode.collectAsState()
            
            // Determine theme based on settings or system default
            val useDarkTheme = when (themeMode) {
                "LIGHT" -> false
                "DARK" -> true
                else -> isSystemInDarkTheme()
            }

            MyApplicationTheme(darkTheme = useDarkTheme) {
                val navController = rememberNavController()
                
                NavHost(
                    navController = navController,
                    startDestination = "dashboard",
                    modifier = Modifier.fillMaxSize()
                ) {
                    composable("dashboard") {
                        DashboardScreen(
                            viewModel = viewModel,
                            onNavigateToAddTrade = { navController.navigate("add_trade") },
                            onNavigateToTradeDetail = { id -> navController.navigate("trade_detail/$id") },
                            onNavigateToTradeList = { navController.navigate("trade_list") },
                            onNavigateToSettings = { navController.navigate("settings") }
                        )
                    }

                    composable("trade_list") {
                        TradeListScreen(
                            viewModel = viewModel,
                            onNavigateToTradeDetail = { id -> navController.navigate("trade_detail/$id") },
                            onNavigateBack = { navController.popBackStack() }
                        )
                    }

                    composable("add_trade") {
                        AddEditTradeScreen(
                            viewModel = viewModel,
                            tradeId = null,
                            onNavigateBack = { navController.popBackStack() }
                        )
                    }

                    composable(
                        route = "edit_trade/{tradeId}",
                        arguments = listOf(navArgument("tradeId") { type = NavType.IntType })
                    ) { backStackEntry ->
                        val tradeId = backStackEntry.arguments?.getInt("tradeId")
                        AddEditTradeScreen(
                            viewModel = viewModel,
                            tradeId = tradeId,
                            onNavigateBack = { navController.popBackStack() }
                        )
                    }

                    composable(
                        route = "trade_detail/{tradeId}",
                        arguments = listOf(navArgument("tradeId") { type = NavType.IntType })
                    ) { backStackEntry ->
                        val tradeId = backStackEntry.arguments?.getInt("tradeId") ?: 0
                        TradeDetailScreen(
                            viewModel = viewModel,
                            tradeId = tradeId,
                            onNavigateToEditTrade = { id -> navController.navigate("edit_trade/$id") },
                            onNavigateBack = { navController.popBackStack() }
                        )
                    }

                    composable("settings") {
                        SettingsScreen(
                            viewModel = viewModel,
                            onNavigateBack = { navController.popBackStack() }
                        )
                    }
                }
            }
        }
    }
}
