package com.estate.manager.ui.pest

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.estate.manager.data.AppDatabase
import com.estate.manager.data.models.ChatMessage
import com.estate.manager.data.models.GangTrack
import com.estate.manager.data.repository.ChatRepository
import com.estate.manager.data.repository.GangRepository
import com.estate.manager.rns.PacketSerializer
import com.estate.manager.rns.RnsManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

class PestViewModel(app: Application) : AndroidViewModel(app) {

    private val db       = AppDatabase.get(app)
    private val gangRepo = GangRepository(db.gangDao())
    private val chatRepo = ChatRepository(db.chatDao())
    private val rns      = RnsManager(app)

    val pestTracks: StateFlow<List<GangTrack>> =
        gangRepo.tracksForType("PEST")
            .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val chatMessages: StateFlow<List<ChatMessage>> =
        chatRepo.messagesForChannel("PEST")
            .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun sendChat(supervisorDestHash: String, text: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val msg = ChatMessage(
                msgId      = UUID.randomUUID().toString(),
                channel    = "PEST",
                senderHash = "local",
                senderRole = "Manager",
                text       = text,
                timestamp  = System.currentTimeMillis(),
                isOutgoing = true
            )
            chatRepo.insert(msg)
            rns.sendPacket(supervisorDestHash, PacketSerializer.encodeChatMessage(msg))
        }
    }
}
