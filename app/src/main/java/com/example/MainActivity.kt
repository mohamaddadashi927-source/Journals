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
import android.widget.Toast
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.lifecycleScope
import com.example.data.config.RemoteConfigManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.JournalViewModel

class MainActivity : ComponentActivity() {
    private var configState by mutableStateOf<RemoteConfigManager.RemoteConfig?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Fetch remote config safely in Dispatchers.IO on app startup
        lifecycleScope.launch {
            val config = RemoteConfigManager.fetchConfig()
            if (config != null) {
                configState = config
                if (config.message.isNotEmpty()) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@MainActivity, config.message, Toast.LENGTH_LONG).show()
                    }
                }
            }
        }

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

                // Check version and blocking status
                val currentVersion = remember {
                    try {
                        packageManager.getPackageInfo(packageName, 0).versionName ?: "1.0.0"
                    } catch (e: Exception) {
                        "1.0.0"
                    }
                }

                val config = configState
                if (config != null) {
                    val isDifferent = remember(config.latestVersion, currentVersion) {
                        RemoteConfigManager.isVersionDifferent(config.latestVersion, currentVersion)
                    }

                    if (config.appStatus == "disabled") {
                        AlertDialog(
                            onDismissRequest = {}, // blocking
                            confirmButton = {
                                Button(onClick = { finish() }) {
                                    Text("Exit")
                                }
                            },
                            title = { Text("App is disabled", fontWeight = FontWeight.Bold) },
                            text = { Text("This application has been temporarily disabled by the administrator. Please contact support.") }
                        )
                    } else if (config.forceUpdate && isDifferent) {
                        AlertDialog(
                            onDismissRequest = {}, // blocking
                            confirmButton = {
                                Button(onClick = { /* Blocking confirmation button */ }) {
                                    Text("Update Required")
                                }
                            },
                            title = { Text("Update Required", fontWeight = FontWeight.Bold) },
                            text = { Text("A newer version (${config.latestVersion}) is required to continue. Please update the application.") }
                        )
                    }
                }
                
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
