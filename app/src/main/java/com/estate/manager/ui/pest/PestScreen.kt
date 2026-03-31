package com.estate.manager.ui.pest

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import com.estate.manager.ui.harvest.ChatBubble
import com.estate.manager.ui.map.MapManager
import org.osmdroid.views.MapView

@Composable
fun PestScreen(vm: PestViewModel = viewModel()) {
    val tracks   by vm.pestTracks.collectAsState()
    val messages by vm.chatMessages.collectAsState()

    var mapRef     by remember { mutableStateOf<MapView?>(null) }
    var chatInput  by remember { mutableStateOf("") }
     // populated from Settings/Peers

    LaunchedEffect(tracks) {
        mapRef?.let { mv ->
            MapManager.clearGangPaths(mv, "PEST")
            tracks.forEach { MapManager.renderGangPath(mv, it) }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {

        Text(
            "Pest & Disease Coverage",
            style    = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 4.dp)
        )
        Text(
            "${tracks.size} path segment(s) recorded",
            style    = MaterialTheme.typography.bodySmall,
            color    = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
        )

        // ── Map with red path polylines ───────────────────────────
        AndroidView(
            factory  = { ctx -> MapManager.init(ctx).also { mapRef = it } },
            modifier = Modifier.fillMaxWidth().weight(0.55f)
        )

        // ── Chat with Pest Supervisor ─────────────────────────────
        Text(
            "Pest & Disease Supervisor Chat",
            style    = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 4.dp)
        )
        LazyColumn(
            modifier      = Modifier.weight(0.30f),
            reverseLayout = true
        ) {
            items(messages.reversed()) { ChatBubble(it) }
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
            Spacer(Modifier.width(8.dp))
            IconButton(onClick = {
                if (chatInput.isNotBlank()) {
                    vm.sendChat(chatInput.trim())
                    chatInput = ""
                }
            }) {
                Icon(Icons.Default.Send, contentDescription = "Send")
            }
        }
    }
}
