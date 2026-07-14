package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.data.config.RemoteConfigManager
import kotlinx.coroutines.launch
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.JournalViewModel

class MainActivity : ComponentActivity() {
    private var configState by mutableStateOf<RemoteConfigManager.RemoteConfig?>(null)
    private var isConfigLoaded by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Fetch remote config safely on app startup, failing safely for offline-first behavior
        lifecycleScope.launch {
            try {
                configState = RemoteConfigManager.fetchConfig()
            } catch (e: Exception) {
                configState = null
            } finally {
                isConfigLoaded = true
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
                if (!isConfigLoaded) {
                    // Modern, non-flashing loading screen
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.background),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.primary,
                                strokeWidth = 4.dp
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Loading Settings...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                            )
                        }
                    }
                } else {
                    Box(modifier = Modifier.fillMaxSize()) {
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

                        // Banner overlay at the top (Modern M3 Design, non-blocking)
                        var showBanner by remember { mutableStateOf(true) }
                        val messageText = config?.message
                        val shouldShowBanner = remember(config, showBanner) {
                            showBanner && messageText != null && messageText.trim().isNotEmpty()
                        }

                        if (shouldShowBanner) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .statusBarsPadding()
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                contentAlignment = Alignment.TopCenter
                            ) {
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = Color(0xFFE8F5E9), // Soft green accent
                                        contentColor = Color(0xFF1B5E20)
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                                    border = BorderStroke(1.dp, Color(0xFFC3E6CB)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("remote_config_banner")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Info,
                                            contentDescription = "Info",
                                            tint = Color(0xFF1B5E20),
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(
                                            text = messageText?.trim() ?: "",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = Color(0xFF1B5E20),
                                            modifier = Modifier.weight(1f)
                                        )
                                        IconButton(
                                            onClick = { showBanner = false },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Close",
                                                tint = Color(0xFF1B5E20),
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
