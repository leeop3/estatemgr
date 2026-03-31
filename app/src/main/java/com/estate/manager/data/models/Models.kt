package com.estate.manager.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey

// ─────────────────────────────────────────────────────────────────────────────
// BunchRecord  — from Bunch Checker field device via BR: packet
// ─────────────────────────────────────────────────────────────────────────────
@Entity(tableName = "bunch_records")
data class BunchRecord(
    @PrimaryKey val id: String,          // UUID assigned by checker device
    val checkerId:   String,
    val harvesterId: String,
    val blockId:     String,
    val lat:         Double,
    val lon:         Double,
    val timestamp:   Long,               // Unix epoch ms
    val ripe:        Int,
    val unripe:      Int,
    val empty:       Int,
    val rotten:      Int,
    val damaged:     Int,
    val photoPath:   String? = null,     // local path after IMG: received
    val remarks:     String? = null,
    val synced:      Boolean = false
)

// ─────────────────────────────────────────────────────────────────────────────
// TractorLocation  — from Tractor Driver device via TL: packet
// ─────────────────────────────────────────────────────────────────────────────
@Entity(tableName = "tractor_locations")
data class TractorLocation(
    @PrimaryKey(autoGenerate = true) val dbId: Int = 0,
    val tractorId: String,
    val driverId:  String,
    val lat:       Double,
    val lon:       Double,
    val timestamp: Long
)

// ─────────────────────────────────────────────────────────────────────────────
// GangTrack  — from Pest/Disease or Fertilizing gang device via GT: packet
// pathJson: compact JSON array of {la, lo, ts} waypoints
// ─────────────────────────────────────────────────────────────────────────────
@Entity(tableName = "gang_tracks")
data class GangTrack(
    @PrimaryKey(autoGenerate = true) val dbId: Int = 0,
    val gangId:   String,
    val gangType: String,    // "PEST" | "FERT"
    val pathJson: String,    // "[{la:x,lo:y,ts:z},...]"
    val timestamp: Long
)

// ─────────────────────────────────────────────────────────────────────────────
// ChatMessage  — manager ↔ supervisor messaging via CM: packet
// ─────────────────────────────────────────────────────────────────────────────
@Entity(tableName = "chat_messages")
data class ChatMessage(
    @PrimaryKey val msgId: String,
    val channel:    String,   // "HARVEST" | "PEST" | "FERT"
    val senderHash: String,   // dest hash or "local" for outgoing
    val senderRole: String,   // "Manager" | "Supervisor"
    val text:       String,
    val timestamp:  Long,
    val delivered:  Boolean = false,
    val isOutgoing: Boolean = false
)

// ─────────────────────────────────────────────────────────────────────────────
// Alert  — auto-generated (unripe) or received via AL: packet
// ─────────────────────────────────────────────────────────────────────────────
@Entity(tableName = "alerts")
data class Alert(
    @PrimaryKey val alertId: String,
    val type:          String,   // "UNRIPE" | "ROTTEN" | "DAMAGED"
    val blockId:       String,
    val harvesterId:   String,
    val bunchRecordId: String,
    val timestamp:     Long,
    val acknowledged:  Boolean = false
)

// ─────────────────────────────────────────────────────────────────────────────
// Peer  — discovered via RNS announce
// displayName convention: "ROLE:Name"  e.g. "CHECKER:Ali"
// ─────────────────────────────────────────────────────────────────────────────
@Entity(tableName = "peers")
data class Peer(
    @PrimaryKey val destHash:    String,
    val displayName: String,
    val role:        String,   // "CHECKER" | "TRACTOR" | "PEST" | "FERT"
    val lastSeen:    Long
)

// ─────────────────────────────────────────────────────────────────────────────
// BlockSummary  — non-entity, produced by BunchDao aggregate query
// ─────────────────────────────────────────────────────────────────────────────
data class BlockSummary(
    val blockId:         String,
    val totalBunches:    Int,
    val totalRipe:       Int,
    val totalUnripe:     Int,
    val totalEmpty:      Int,
    val totalRotten:     Int,
    val totalDamaged:    Int,
    val activeHarvesters: Int
)
