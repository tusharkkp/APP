package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.example.data.AppDatabase
import com.example.data.ScanRepository
import com.example.ui.screens.AnalyticsScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.ScanScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.VisionViewModel
import com.example.ui.viewmodel.VisionViewModelFactory

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Setup edge-to-edge full bleed rendering
        enableEdgeToEdge()

        // Initialize Room local storage database & repository
        val database = AppDatabase.getDatabase(this)
        val repository = ScanRepository(database.scanDao())

        // ViewModel instantiation via our explicit factory provider
        val viewModel: VisionViewModel by viewModels {
            VisionViewModelFactory(application, repository)
        }

        setContent {
            MyApplicationTheme {
                var currentTab by remember { mutableStateOf("Scan") }
                val coroutineScope = rememberCoroutineScope()

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        NavigationBar(
                            modifier = Modifier.testTag("app_navigation_bar")
                        ) {
                            NavigationBarItem(
                                selected = currentTab == "Scan",
                                onClick = { currentTab = "Scan" },
                                label = { Text("Scan") },
                                icon = {
                                    Icon(
                                        imageVector = Icons.Default.PhotoCamera,
                                        contentDescription = "Active scanning viewfinder"
                                    )
                                },
                                modifier = Modifier.testTag("nav_tab_scan")
                            )

                            NavigationBarItem(
                                selected = currentTab == "History",
                                onClick = { currentTab = "History" },
                                label = { Text("History") },
                                icon = {
                                    Icon(
                                        imageVector = Icons.Default.History,
                                        contentDescription = "Scan logs history"
                                    )
                                },
                                modifier = Modifier.testTag("nav_tab_history")
                            )

                            NavigationBarItem(
                                selected = currentTab == "Analytics",
                                onClick = { currentTab = "Analytics" },
                                label = { Text("Metrics") },
                                icon = {
                                    Icon(
                                        imageVector = Icons.Default.BarChart,
                                        contentDescription = "Metrics and dashboard"
                                    )
                                },
                                modifier = Modifier.testTag("nav_tab_analytics")
                            )
                        }
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        when (currentTab) {
                            "Scan" -> ScanScreen(
                                viewModel = viewModel,
                                scope = coroutineScope
                            )

                            "History" -> HistoryScreen(
                                viewModel = viewModel
                            )

                            "Analytics" -> AnalyticsScreen(
                                viewModel = viewModel
                            )
                        }
                    }
                }
            }
        }
    }
}
