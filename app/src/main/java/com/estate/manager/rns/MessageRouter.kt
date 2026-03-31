package com.estate.manager.rns

import android.util.Log
import com.estate.manager.data.models.Alert
import com.estate.manager.data.models.Peer
import com.estate.manager.data.repository.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Implements RnsCallback and routes every incoming packet to the right repository.
 *
 * Routing table (based on payload prefix from rns_backend.py):
 *   BR:  → BunchRepository.insert()        + auto-generate UNRIPE alert
 *   TL:  → TractorRepository.upsert()
 *   GT:  → GangRepository.insert()
 *   CM:  → ChatRepository.insert()
 *   AL:  → AlertRepository.insert()
 *   IMG  → BunchRepository.attachPhoto()   (isImage=true, content = local path)
 *   ACK  → ChatRepository.markDelivered()  (handled by onMessageDelivered)
 *   peer → PeerRepository.upsert()         (onAnnounceReceived)
 */
class MessageRouter(
    private val bunchRepo:   BunchRepository,
    private val tractorRepo: TractorRepository,
    private val gangRepo:    GangRepository,
    private val chatRepo:    ChatRepository,
    private val alertRepo:   AlertRepository,
    private val peerRepo:    PeerRepository,
    private val scope:       CoroutineScope
) : RnsCallback {

    private val TAG = "MessageRouter"

    // ─────────────────────────────────────────────────────────────
    // Called by Python: kotlin_callback.onNewMessage(...)
    // ─────────────────────────────────────────────────────────────
    override fun onNewMessage(
        senderHash:  String,
        content:     String,
        timestampMs: Long,
        isImage:     Boolean,
        isOutgoing:  Boolean,
        msgHash:     String
    ) {
        scope.launch(Dispatchers.IO) {
            try {
                when {
                    isImage -> {
                        // content = local file path set by rns_backend.py's on_lxmf
                        Log.d(TAG, "IMG from $senderHash → $content")
                        bunchRepo.attachPhoto(senderHash, content)
                    }
                    content.startsWith("BR:") -> {
                        val record = PacketSerializer.decodeBunchRecord(content)
                        Log.d(TAG, "BR from $senderHash block=${record.blockId}")
                        bunchRepo.insert(record)
                        // Auto-alert for unripe bunches
                        if (record.unripe > 0) {
                            alertRepo.insert(
                                Alert(
                                    alertId       = "${record.id}_unripe",
                                    type          = "UNRIPE",
                                    blockId       = record.blockId,
                                    harvesterId   = record.harvesterId,
                                    bunchRecordId = record.id,
                                    timestamp     = record.timestamp
                                )
                            )
                        }
                    }
                    content.startsWith("TL:") -> {
                        val loc = PacketSerializer.decodeTractorLocation(content)
                        Log.d(TAG, "TL tractor=${loc.tractorId}")
                        tractorRepo.upsert(loc)
                    }
                    content.startsWith("GT:") -> {
                        val track = PacketSerializer.decodeGangTrack(content)
                        Log.d(TAG, "GT gang=${track.gangId} type=${track.gangType}")
                        gangRepo.insert(track)
                    }
                    content.startsWith("CM:") -> {
                        val msg = PacketSerializer.decodeChatMessage(content, senderHash)
                        Log.d(TAG, "CM channel=${msg.channel} from=$senderHash")
                        chatRepo.insert(msg)
                    }
                    content.startsWith("AL:") -> {
                        val alert = PacketSerializer.decodeAlert(content)
                        Log.d(TAG, "AL type=${alert.type} block=${alert.blockId}")
                        alertRepo.insert(alert)
                    }
                    else -> Log.w(TAG, "Unknown packet prefix from $senderHash: ${content.take(10)}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error routing message from $senderHash", e)
            }
        }
    }

    // ─────────────────────────────────────────────────────────────
    // Called by Python: kotlin_callback.onMessageDelivered(msgHash)
    // ─────────────────────────────────────────────────────────────
    override fun onMessageDelivered(msgHash: String) {
        scope.launch(Dispatchers.IO) {
            Log.d(TAG, "ACK received for msgHash=$msgHash")
            chatRepo.markDelivered(msgHash)
        }
    }

    // ─────────────────────────────────────────────────────────────
    // Called by Python: kotlin_callback.onAnnounceReceived(destHash, displayName)
    // Display name convention from field devices: "ROLE:Name"  e.g. "CHECKER:Ali"
    // ─────────────────────────────────────────────────────────────
    override fun onAnnounceReceived(destHash: String, displayName: String) {
        scope.launch(Dispatchers.IO) {
            val parts = displayName.split(":", limit = 2)
            val role  = if (parts.size == 2) parts[0] else "UNKNOWN"
            val name  = if (parts.size == 2) parts[1] else displayName
            Log.i(TAG, "PEER discovered hash=$destHash role=$role name=$name")
            peerRepo.upsert(
                Peer(
                    destHash    = destHash,
                    displayName = name,
                    role        = role,
                    lastSeen    = System.currentTimeMillis()
                )
            )
        }
    }
}
