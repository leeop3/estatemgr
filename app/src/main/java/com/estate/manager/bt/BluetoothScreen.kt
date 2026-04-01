package com.estate.manager.bt

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun BluetoothScreen(vm: BluetoothViewModel = viewModel()) {

    val devices       by vm.pairedDevices.collectAsState()
    val selected      by vm.selectedDevice.collectAsState()
    val bridgeState   by vm.bridgeState.collectAsState()
    val status        by vm.status.collectAsState()

    Column(
        modifier            = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {

        // ── Header ────────────────────────────────────────────────
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Bluetooth,
                 contentDescription = null,
                 tint     = MaterialTheme.colorScheme.primary,
                 modifier = Modifier.size(28.dp))
            Spacer(Modifier.width(8.dp))
            Text("RNode Bluetooth (SPP)",
                 style = MaterialTheme.typography.titleLarge)
        }

        // ── Bridge status card ────────────────────────────────────
        BridgeStatusCard(bridgeState, status)

        // ── Connect / Disconnect button ───────────────────────────
        when (bridgeState) {
            BridgeState.IDLE, BridgeState.ERROR -> {
                Button(
                    onClick  = { vm.startBridge() },
                    enabled  = selected.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth(),
                    colors   = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(Icons.Default.BluetoothConnected, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Connect to RNode")
                }
            }
            BridgeState.CONNECTING -> {
                Button(
                    onClick  = { vm.stopBridge() },
                    modifier = Modifier.fillMaxWidth(),
                    colors   = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    CircularProgressIndicator(
                        modifier    = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color       = Color.White
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Connecting... (tap to cancel)")
                }
            }
            BridgeState.ACTIVE -> {
                Button(
                    onClick  = { vm.stopBridge() },
                    modifier = Modifier.fillMaxWidth(),
                    colors   = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(Icons.Default.BluetoothDisabled, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Disconnect RNode")
                }
            }
        }

        HorizontalDivider()

        // ── Paired device list ────────────────────────────────────
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Text("Paired Devices", style = MaterialTheme.typography.titleMedium)
            IconButton(onClick = { vm.refreshPairedDevices() }) {
                Icon(Icons.Default.Refresh, contentDescription = "Refresh")
            }
        }

        if (devices.isEmpty()) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("No paired Bluetooth devices found.",
                         style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(8.dp))
                    Text("To pair your T-Beam:",
                         style = MaterialTheme.typography.labelMedium,
                         color = MaterialTheme.colorScheme.primary)
                    Text("1. Power on the T-Beam with RNode firmware",
                         style = MaterialTheme.typography.bodySmall)
                    Text("2. Go to Android Settings → Bluetooth",
                         style = MaterialTheme.typography.bodySmall)
                    Text("3. Pair with 'RNode' or 'T-Beam' device",
                         style = MaterialTheme.typography.bodySmall)
                    Text("4. Come back here and tap Refresh",
                         style = MaterialTheme.typography.bodySmall)
                }
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(devices) { device ->
                    DeviceCard(
                        device   = device,
                        isSelected = device.address == selected,
                        onClick  = { vm.selectDevice(device.address) }
                    )
                }
            }
        }

        // ── How it works ──────────────────────────────────────────
        HorizontalDivider()
        Text("How it works", style = MaterialTheme.typography.labelMedium,
             color = MaterialTheme.colorScheme.primary)
        Text(
            "The app creates a BT SPP serial connection to your T-Beam, " +
            "then bridges it to a local TCP port (7633) that the RNS Python layer uses. " +
            "Connect BT first, then RNS starts automatically.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ─────────────────────────────────────────────────────────────────
@Composable
fun BridgeStatusCard(state: BridgeState, status: String) {
    val (color, icon, label) = when (state) {
        BridgeState.IDLE       -> Triple(Color(0xFF546E7A), Icons.Default.BluetoothDisabled, "Disconnected")
        BridgeState.CONNECTING -> Triple(Color(0xFFF9A825), Icons.Default.Bluetooth,         "Connecting...")
        BridgeState.ACTIVE     -> Triple(Color(0xFF2E7D32), Icons.Default.BluetoothConnected,"Bridge Active")
        BridgeState.ERROR      -> Triple(Color(0xFFB71C1C), Icons.Default.Error,             "Error")
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        border   = BorderStroke(1.dp, color.copy(alpha = 0.5f))
    ) {
        Row(
            modifier          = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color  = color.copy(alpha = 0.15f),
                shape  = RoundedCornerShape(8.dp)
            ) {
                Icon(icon, contentDescription = null,
                     tint     = color,
                     modifier = Modifier.padding(8.dp).size(24.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text(label, style = MaterialTheme.typography.titleSmall, color = color)
                Text(status,
                     style = MaterialTheme.typography.bodySmall,
                     color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────
@Composable
fun DeviceCard(device: BtDeviceInfo, isSelected: Boolean, onClick: () -> Unit) {
    val borderColor = if (isSelected) MaterialTheme.colorScheme.primary
                      else            MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)

    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        border   = BorderStroke(if (isSelected) 2.dp else 1.dp, borderColor)
    ) {
        Row(
            modifier          = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                if (isSelected) Icons.Default.BluetoothConnected else Icons.Default.Bluetooth,
                contentDescription = null,
                tint = if (isSelected) MaterialTheme.colorScheme.primary
                       else            MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(device.name, style = MaterialTheme.typography.titleSmall)
                Text(device.address,
                     fontFamily = FontFamily.Monospace,
                     style      = MaterialTheme.typography.labelSmall,
                     color      = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (isSelected) {
                Icon(Icons.Default.CheckCircle,
                     contentDescription = "Selected",
                     tint = MaterialTheme.colorScheme.primary)
            }
        }
    }
}
