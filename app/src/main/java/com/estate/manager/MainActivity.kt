package com.estate.manager

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Agriculture
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Grass
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.estate.manager.bt.BluetoothScreen
import com.estate.manager.bt.BluetoothViewModel
import com.estate.manager.bt.BridgeState
import com.estate.manager.rns.RnsService
import com.estate.manager.ui.fertilize.FertilizeScreen
import com.estate.manager.ui.harvest.HarvestScreen
import com.estate.manager.ui.pest.PestScreen
import com.estate.manager.ui.settings.SettingsScreen
import com.estate.manager.ui.settings.SettingsViewModel
import com.estate.manager.ui.theme.EstateTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val settingsVm:   SettingsViewModel   by viewModels()
    private val bluetoothVm:  BluetoothViewModel  by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Start RNS foreground service
        val svcIntent = Intent(this, RnsService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(svcIntent)
        } else {
            startService(svcIntent)
        }

        // Watch BT bridge state â€” start RNS once bridge goes ACTIVE
        lifecycleScope.launch {
            bluetoothVm.bridgeState.collect { state ->
                if (state == BridgeState.ACTIVE) {
                    // Give the bridge 500ms to stabilise before Python connects
                    delay(500)
                    startRns()
                }
            }
        }

        // Also try RNS start immediately in case BT was already connected
        // (e.g. app restart while T-Beam is still paired and bridge persists)
        lifecycleScope.launch(Dispatchers.IO) {
            delay(1000)
            if (bluetoothVm.bridgeState.value == BridgeState.ACTIVE) {
                startRns()
            }
        }

        setContent {
            EstateTheme {
                MainScaffold(settingsVm, bluetoothVm)
            }
        }
    }

    private fun startRns() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val prefs    = getSharedPreferences("estate_prefs", MODE_PRIVATE)
                val nickname = prefs.getString("manager_nickname", "Manager:Unknown") ?: "Manager:Unknown"
                val py       = com.chaquo.python.Python.getInstance()
                val rns      = py.getModule("rns_backend")
                // inject_rnode will connect to 127.0.0.1:7633 â€” the BT bridge TCP server
                val hash = rns.callAttr("start_rns", filesDir.absolutePath, null, nickname).toString()
                val freq = prefs.getLong("rnode_freq", 865_000_000L)
                val bw   = prefs.getInt("rnode_bw",   125_000)
                val tx   = prefs.getInt("rnode_tx",   17)
                val sf   = prefs.getInt("rnode_sf",   9)
                val cr   = prefs.getInt("rnode_cr",   5)
                rns.callAttr("inject_rnode", freq, bw, tx, sf, cr)
                rns.callAttr("announce_now")
                if (hash.isNotEmpty()) {
                    launch(Dispatchers.Main) { settingsVm.onRnsStarted(hash) }
                }
                android.util.Log.i("MainActivity", "RNS started hash=$hash")
            } catch (e: Exception) {
                android.util.Log.e("MainActivity", "RNS start error: ${e.message}")
            }
        }
    }
}

@Composable
fun MainScaffold(
    settingsVm:  SettingsViewModel,
    bluetoothVm: BluetoothViewModel
) {
    var selectedTab by remember { mutableIntStateOf(0) }

    // Show BT tab badge when not connected
    val bridgeState by bluetoothVm.bridgeState.collectAsState()

    val tabs = listOf(
        Triple("Harvest",   Icons.Default.Agriculture, 0),
        Triple("Pest",      Icons.Default.BugReport,   1),
        Triple("Fert",      Icons.Default.Grass,       2),
        Triple("BT/RNode",  Icons.Default.Bluetooth,   3),
        Triple("Settings",  Icons.Default.Settings,    4)
    )

    Scaffold(
        bottomBar = {
            NavigationBar {
                tabs.forEach { (label, icon, idx) ->
                    NavigationBarItem(
                        selected  = selectedTab == idx,
                        onClick   = { selectedTab = idx },
                        icon      = {
                            // Show badge on BT tab when disconnected
                            if (idx == 3 && bridgeState != BridgeState.ACTIVE) {
                                BadgedBox(badge = {
                                    Badge { Text("!") }
                                }) {
                                    Icon(icon, contentDescription = label)
                                }
                            } else {
                                Icon(icon, contentDescription = label)
                            }
                        },
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
                3 -> BluetoothScreen(bluetoothVm)
                4 -> SettingsScreen(settingsVm)
            }
        }
    }
}
