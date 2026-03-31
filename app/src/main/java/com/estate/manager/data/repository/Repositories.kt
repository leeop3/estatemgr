package com.estate.manager.data.repository

import com.estate.manager.data.db.*
import com.estate.manager.data.models.*
import kotlinx.coroutines.flow.Flow

class BunchRepository(private val dao: BunchDao) {
    suspend fun insert(r: BunchRecord) = dao.insert(r)
    fun all(): Flow<List<BunchRecord>> = dao.all()
    fun forBlock(id: String) = dao.forBlock(id)
    fun blockSummary() = dao.blockSummary()
    suspend fun attachPhoto(senderHash: String, path: String) = dao.attachPhoto(senderHash, path)
}

class TractorRepository(private val dao: TractorDao) {
    suspend fun upsert(loc: TractorLocation) = dao.upsert(loc)
    fun latestPositions(): Flow<List<TractorLocation>> = dao.latestPositions()
    fun trackForTractor(id: String) = dao.trackForTractor(id)
}

class GangRepository(private val dao: GangDao) {
    suspend fun insert(t: GangTrack) = dao.insert(t)
    fun tracksForType(type: String) = dao.tracksForType(type)
    fun all(): Flow<List<GangTrack>> = dao.all()
}

class ChatRepository(private val dao: ChatDao) {
    suspend fun insert(m: ChatMessage) = dao.insert(m)
    fun messagesForChannel(channel: String) = dao.messagesForChannel(channel)
    suspend fun markDelivered(msgId: String) = dao.markDelivered(msgId)
    fun pendingCount(channel: String) = dao.pendingCount(channel)
}

class AlertRepository(private val dao: AlertDao) {
    suspend fun insert(a: Alert) = dao.insert(a)
    fun unacknowledged(): Flow<List<Alert>> = dao.unacknowledged()
    fun all(): Flow<List<Alert>> = dao.all()
    suspend fun acknowledge(id: String) = dao.acknowledge(id)
}

class PeerRepository(private val dao: PeerDao) {
    suspend fun upsert(p: Peer) = dao.upsert(p)
    fun all(): Flow<List<Peer>> = dao.all()
    fun byRole(role: String) = dao.byRole(role)
    suspend fun findByHash(hash: String) = dao.findByHash(hash)
}