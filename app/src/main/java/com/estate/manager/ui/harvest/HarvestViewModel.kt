package com.estate.manager.ui.harvest

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.estate.manager.data.AppDatabase
import com.estate.manager.data.models.BlockSummary
import com.estate.manager.data.models.Alert
import com.estate.manager.data.models.BunchRecord
import com.estate.manager.data.models.ChatMessage
import com.estate.manager.data.models.TractorLocation
import com.estate.manager.data.repository.*
import com.estate.manager.rns.PacketSerializer
import com.estate.manager.rns.RnsManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

class HarvestViewModel(app: Application) : AndroidViewModel(app) {

    private val db          = AppDatabase.get(app)
    private val bunchRepo   = BunchRepository(db.bunchDao())
    private val tractorRepo = TractorRepository(db.tractorDao())
    private val alertRepo   = AlertRepository(db.alertDao())
    private val chatRepo    = ChatRepository(db.chatDao())
    private val rns         = RnsManager(app)

    val blockSummaries: StateFlow<List<BlockSummary>> =
        bunchRepo.blockSummary()
            .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val allBunchRecords: StateFlow<List<BunchRecord>> =
        bunchRepo.all()
            .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val tractorPositions: StateFlow<List<TractorLocation>> =
        tractorRepo.latestPositions()
            .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val alerts: StateFlow<List<Alert>> =
        alertRepo.unacknowledged()
            .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val chatMessages: StateFlow<List<ChatMessage>> =
        chatRepo.messagesForChannel("HARVEST")
            .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun acknowledgeAlert(alertId: String) {
        viewModelScope.launch { alertRepo.acknowledge(alertId) }
    }

    /**
     * Manager sends a chat message to the Harvest Supervisor.
     * 1. Persists locally as outgoing
     * 2. Encodes as CM: packet
     * 3. Sends via RNS send_text
     */
    fun sendChat(supervisorDestHash: String, text: String) {
        val prefs     = getApplication<Application>()
            .getSharedPreferences("estate_prefs", android.content.Context.MODE_PRIVATE)
        val managerId = prefs.getString("manager_id", "Manager") ?: "Manager"

        viewModelScope.launch(Dispatchers.IO) {
            val msg = ChatMessage(
                msgId      = UUID.randomUUID().toString(),
                channel    = "HARVEST",
                senderHash = "local",
                senderRole = "Manager",
                text       = text,
                timestamp  = System.currentTimeMillis(),
                isOutgoing = true
            )
            chatRepo.insert(msg)
            val payload = PacketSerializer.encodeChatMessage(msg)
            rns.sendPacket(supervisorDestHash, payload)
        }
    }
}
