package com.estate.manager.data.db

import androidx.room.*
import com.estate.manager.data.models.*
import kotlinx.coroutines.flow.Flow

// ─────────────────────────────────────────────────────────────────────────────
// BunchDao
// ─────────────────────────────────────────────────────────────────────────────
@Dao
interface BunchDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: BunchRecord)

    @Query("SELECT * FROM bunch_records ORDER BY timestamp DESC")
    fun all(): Flow<List<BunchRecord>>

    @Query("SELECT * FROM bunch_records WHERE blockId = :blockId ORDER BY timestamp DESC")
    fun forBlock(blockId: String): Flow<List<BunchRecord>>

    /** Aggregate per-block summary for the dashboard. */
    @Query("""
        SELECT
            blockId,
            COUNT(*)                    AS totalBunches,
            SUM(ripe)                   AS totalRipe,
            SUM(unripe)                 AS totalUnripe,
            SUM(empty)                  AS totalEmpty,
            SUM(rotten)                 AS totalRotten,
            SUM(damaged)                AS totalDamaged,
            COUNT(DISTINCT harvesterId) AS activeHarvesters
        FROM bunch_records
        GROUP BY blockId
        ORDER BY blockId ASC
    """)
    fun blockSummary(): Flow<List<BlockSummary>>

    /**
     * Attaches the most recent photo for a given checker (sender).
     * Called after IMG: packet is received and the file is saved locally.
     */
    @Query("""
        UPDATE bunch_records
        SET    photoPath = :path
        WHERE  checkerId  = :senderHash
          AND  photoPath  IS NULL
          AND  timestamp  = (
              SELECT MAX(timestamp) FROM bunch_records WHERE checkerId = :senderHash
          )
    """)
    suspend fun attachPhoto(senderHash: String, path: String)
}

// ─────────────────────────────────────────────────────────────────────────────
// TractorDao
// ─────────────────────────────────────────────────────────────────────────────
@Dao
interface TractorDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(loc: TractorLocation)

    /** Latest position per tractor — used for live map markers. */
    @Query("""
        SELECT * FROM tractor_locations t1
        WHERE  timestamp = (
            SELECT MAX(t2.timestamp)
            FROM   tractor_locations t2
            WHERE  t2.tractorId = t1.tractorId
        )
    """)
    fun latestPositions(): Flow<List<TractorLocation>>

    @Query("SELECT * FROM tractor_locations WHERE tractorId = :id ORDER BY timestamp ASC")
    fun trackForTractor(id: String): Flow<List<TractorLocation>>
}

// ─────────────────────────────────────────────────────────────────────────────
// GangDao
// ─────────────────────────────────────────────────────────────────────────────
@Dao
interface GangDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(track: GangTrack)

    @Query("SELECT * FROM gang_tracks WHERE gangType = :type ORDER BY timestamp ASC")
    fun tracksForType(type: String): Flow<List<GangTrack>>

    @Query("SELECT * FROM gang_tracks ORDER BY timestamp DESC")
    fun all(): Flow<List<GangTrack>>
}

// ─────────────────────────────────────────────────────────────────────────────
// ChatDao
// ─────────────────────────────────────────────────────────────────────────────
@Dao
interface ChatDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(msg: ChatMessage)

    @Query("""
        SELECT * FROM chat_messages
        WHERE  channel = :channel
        ORDER  BY timestamp ASC
    """)
    fun messagesForChannel(channel: String): Flow<List<ChatMessage>>

    @Query("UPDATE chat_messages SET delivered = 1 WHERE msgId = :msgId")
    suspend fun markDelivered(msgId: String)

    @Query("SELECT COUNT(*) FROM chat_messages WHERE channel = :channel AND delivered = 0 AND isOutgoing = 1")
    fun pendingCount(channel: String): Flow<Int>
}

// ─────────────────────────────────────────────────────────────────────────────
// AlertDao
// ─────────────────────────────────────────────────────────────────────────────
@Dao
interface AlertDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(alert: Alert)

    @Query("SELECT * FROM alerts WHERE acknowledged = 0 ORDER BY timestamp DESC")
    fun unacknowledged(): Flow<List<Alert>>

    @Query("SELECT * FROM alerts ORDER BY timestamp DESC")
    fun all(): Flow<List<Alert>>

    @Query("UPDATE alerts SET acknowledged = 1 WHERE alertId = :alertId")
    suspend fun acknowledge(alertId: String)
}

// ─────────────────────────────────────────────────────────────────────────────
// PeerDao
// ─────────────────────────────────────────────────────────────────────────────
@Dao
interface PeerDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(peer: Peer)

    @Query("SELECT * FROM peers ORDER BY lastSeen DESC")
    fun all(): Flow<List<Peer>>

    @Query("SELECT * FROM peers WHERE role = :role ORDER BY lastSeen DESC")
    fun byRole(role: String): Flow<List<Peer>>

    @Query("SELECT * FROM peers WHERE destHash = :hash LIMIT 1")
    suspend fun findByHash(hash: String): Peer?
}
