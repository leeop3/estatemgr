package com.estate.manager.ui.fertilize

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

class FertilizeViewModel(app: Application) : AndroidViewModel(app) {
    private val db       = AppDatabase.get(app)
    private val gangRepo = GangRepository(db.gangDao())
    private val chatRepo = ChatRepository(db.chatDao())
    private val rns      = RnsManager(app)

    val fertTracks: StateFlow<List<GangTrack>> =
        gangRepo.tracksForType("FERT")
            .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val chatMessages: StateFlow<List<ChatMessage>> =
        chatRepo.messagesForChannel("FERT")
            .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun sendChat(text: String) {
        val hash = getApplication<Application>()
            .getSharedPreferences("estate_prefs", android.content.Context.MODE_PRIVATE)
            .getString("supervisor_FERT", "") ?: ""
        if (hash.isEmpty()) return
        viewModelScope.launch(Dispatchers.IO) {
            val msg = ChatMessage(
                msgId      = UUID.randomUUID().toString(),
                channel    = "FERT",
                senderHash = "local",
                senderRole = "Manager",
                text       = text,
                timestamp  = System.currentTimeMillis(),
                isOutgoing = true
            )
            chatRepo.insert(msg)
            rns.sendPacket(hash, PacketSerializer.encodeChatMessage(msg))
        }
    }
}