package com.estate.manager.ui.harvest

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import com.estate.manager.data.models.Alert
import com.estate.manager.data.models.BlockSummary
import com.estate.manager.data.models.ChatMessage
import com.estate.manager.ui.map.MapManager
import org.osmdroid.views.MapView

@Composable
fun HarvestScreen(vm: HarvestViewModel = viewModel()) {
    val summaries by vm.blockSummaries.collectAsState()
    val bunches   by vm.allBunchRecords.collectAsState()
    val tractors  by vm.tractorPositions.collectAsState()
    val alerts    by vm.alerts.collectAsState()
    val messages  by vm.chatMessages.collectAsState()

    var mapViewRef  by remember { mutableStateOf<MapView?>(null) }
    var chatInput   by remember { mutableStateOf("") }
    var showAlerts  by remember { mutableStateOf(false) }

    // Supervisor dest hash would come from Settings/Peer list in production
    val supervisorHash = remember { "" }

    // Re-render map overlays when data changes
    LaunchedEffect(bunches, tractors) {
        mapViewRef?.let { mv ->
            MapManager.renderBunchMarkers(mv, bunches)
            MapManager.renderTractorMarkers(mv, tractors)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {

        // ── Alert banner ──────────────────────────────────────────
        if (alerts.isNotEmpty()) {
            AlertBanner(count = alerts.size, onClick = { showAlerts = true })
        }

        // ── Block summary cards ───────────────────────────────────
        Text(
            text  = "Block Summary",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 4.dp)
        )
        LazyColumn(modifier = Modifier.weight(0.35f)) {
            items(summaries) { s -> BlockSummaryCard(s) }
            if (summaries.isEmpty()) {
                item {
                    Text(
                        "No bunch data received yet.",
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // ── Map ───────────────────────────────────────────────────
        Text(
            text  = "Live Map",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 4.dp)
        )
        AndroidView(
            factory = { ctx ->
                MapManager.init(ctx).also { mapViewRef = it }
            },
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.35f)
        )

        // ── Chat with Harvest Supervisor ──────────────────────────
        Text(
            text  = "Harvest Supervisor Chat",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 4.dp)
        )
        LazyColumn(
            modifier      = Modifier.weight(0.20f),
            reverseLayout = true
        ) {
            items(messages.reversed()) { m -> ChatBubble(m) }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value         = chatInput,
                onValueChange = { chatInput = it },
                modifier      = Modifier.weight(1f),
                placeholder   = { Text("Message supervisor…") },
                singleLine    = true
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = {
                    if (chatInput.isNotBlank()) {
                        vm.sendChat(supervisorHash, chatInput.trim())
                        chatInput = ""
                    }
                }
            ) {
                Icon(Icons.Default.Send, contentDescription = "Send")
            }
        }
    }

    // ── Alerts bottom sheet ───────────────────────────────────────
    if (showAlerts) {
        AlertsSheet(
            alerts    = alerts,
            onAck     = { vm.acknowledgeAlert(it) },
            onDismiss = { showAlerts = false }
        )
    }
}

// ─────────────────────────────────────────────────────────────────
@Composable
fun AlertBanner(count: Int, onClick: () -> Unit) {
    Surface(
        color    = MaterialTheme.colorScheme.errorContainer,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Warning, contentDescription = null,
                 tint = MaterialTheme.colorScheme.error)
            Spacer(Modifier.width(8.dp))
            Text(
                "$count unacknowledged alert${if (count > 1) "s" else ""} — tap to view",
                color = MaterialTheme.colorScheme.onErrorContainer
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────
@Composable
fun BlockSummaryCard(s: BlockSummary) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Block ${s.blockId}", style = MaterialTheme.typography.titleSmall)
                Text(
                    "${s.activeHarvesters} harvester${if (s.activeHarvesters != 1) "s" else ""}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                StatChip("Ripe",    s.totalRipe,    Color(0xFF2E7D32))
                StatChip("Unripe",  s.totalUnripe,  Color(0xFFF9A825))
                StatChip("Rotten",  s.totalRotten,  Color(0xFFB71C1C))
                StatChip("Empty",   s.totalEmpty,   Color(0xFF546E7A))
                StatChip("Damaged", s.totalDamaged, Color(0xFF6A1B9A))
            }
        }
    }
}

@Composable
fun StatChip(label: String, value: Int, color: Color) {
    Surface(
        color = color.copy(alpha = 0.12f),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                value.toString(),
                color = color,
                style = MaterialTheme.typography.titleSmall
            )
            Text(label, style = MaterialTheme.typography.labelSmall)
        }
    }
}

// ─────────────────────────────────────────────────────────────────
@Composable
fun ChatBubble(msg: ChatMessage) {
    val isOut  = msg.isOutgoing
    val bg     = if (isOut) Color(0xFF1565C0) else Color(0xFF37474F)
    val align  = if (isOut) Arrangement.End   else Arrangement.Start

    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 2.dp),
        horizontalArrangement = align
    ) {
        Column(horizontalAlignment = if (isOut) Alignment.End else Alignment.Start) {
            Surface(color = bg, shape = RoundedCornerShape(12.dp)) {
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                    if (!isOut) {
                        Text(
                            msg.senderRole,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }
                    Text(msg.text, color = Color.White)
                }
            }
            if (isOut) {
                Text(
                    if (msg.delivered) "✓ Delivered" else "Pending",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlertsSheet(
    alerts:    List<Alert>,
    onAck:     (String) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Text(
            "Active Alerts",
            style    = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(16.dp)
        )
        LazyColumn(modifier = Modifier.padding(bottom = 32.dp)) {
            items(alerts) { a ->
                ListItem(
                    headlineContent = { Text("${a.type} — Block ${a.blockId}") },
                    supportingContent = { Text("Harvester: ${a.harvesterId}") },
                    trailingContent = {
                        TextButton(onClick = { onAck(a.alertId) }) {
                            Text("ACK")
                        }
                    },
                    leadingContent = {
                        Icon(Icons.Default.Warning,
                             contentDescription = null,
                             tint = MaterialTheme.colorScheme.error)
                    }
                )
                HorizontalDivider()
            }
        }
    }
}
