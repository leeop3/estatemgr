package com.estate.manager

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Agriculture
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Grass
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.estate.manager.rns.RnsService
import com.estate.manager.ui.fertilize.FertilizeScreen
import com.estate.manager.ui.harvest.HarvestScreen
import com.estate.manager.ui.pest.PestScreen
import com.estate.manager.ui.settings.SettingsScreen
import com.estate.manager.ui.theme.EstateTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Start RNS as a foreground service so it survives tab switches
        val svcIntent = Intent(this, RnsService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(svcIntent)
        } else {
            startService(svcIntent)
        }

        setContent {
            EstateTheme {
                MainScaffold()
            }
        }
    }
}

@Composable
fun MainScaffold() {
    var selectedTab by remember { mutableIntStateOf(0) }

    val tabs = listOf(
        Triple("Harvest",     Icons.Default.Agriculture, 0),
        Triple("Pest",        Icons.Default.BugReport,   1),
        Triple("Fertilizing", Icons.Default.Grass,       2),
        Triple("Settings",    Icons.Default.Settings,    3)
    )

    Scaffold(
        bottomBar = {
            NavigationBar {
                tabs.forEach { (label, icon, idx) ->
                    NavigationBarItem(
                        selected  = selectedTab == idx,
                        onClick   = { selectedTab = idx },
                        icon      = { Icon(icon, contentDescription = label) },
                        label     = { Text(label) }
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            when (selectedTab) {
                0 -> HarvestScreen()
                1 -> PestScreen()
                2 -> FertilizeScreen()
                3 -> SettingsScreen()
            }
        }
    }
}
