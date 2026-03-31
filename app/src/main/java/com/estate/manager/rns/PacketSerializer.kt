package com.estate.manager.rns

import com.estate.manager.data.models.*
import org.json.JSONArray
import org.json.JSONObject

/**
 * Serialises / deserialises all estate message types.
 *
 * Design goals:
 *  - Compact JSON with short keys  →  minimise LoRa payload bytes
 *  - Prefix routing so MessageRouter can switch without parsing full JSON
 *
 * Prefix table:
 *   BR:  BunchRecord
 *   TL:  TractorLocation
 *   GT:  GangTrack
 *   CM:  ChatMessage
 *   AL:  Alert
 *   IMG: (handled by rns_backend.py natively)
 *   ACK: (handled by rns_backend.py natively)
 */
object PacketSerializer {

    // ─────────────────────────────────────────────────────────────
    // BUNCH RECORD  (prefix "BR:")
    // ─────────────────────────────────────────────────────────────

    fun encodeBunchRecord(r: BunchRecord): String {
        val j = JSONObject().apply {
            put("id", r.id)
            put("ci", r.checkerId)      // checker_id
            put("hi", r.harvesterId)    // harvester_id
            put("bi", r.blockId)        // block_id
            put("la", r.lat)
            put("lo", r.lon)
            put("ts", r.timestamp)
            put("ri", r.ripe)
            put("un", r.unripe)
            put("em", r.empty)
            put("ro", r.rotten)
            put("da", r.damaged)
            r.remarks?.let { put("rm", it) }
        }
        return "BR:$j"
    }

    fun decodeBunchRecord(raw: String): BunchRecord {
        val j = JSONObject(raw.removePrefix("BR:"))
        return BunchRecord(
            id          = j.getString("id"),
            checkerId   = j.getString("ci"),
            harvesterId = j.getString("hi"),
            blockId     = j.getString("bi"),
            lat         = j.getDouble("la"),
            lon         = j.getDouble("lo"),
            timestamp   = j.getLong("ts"),
            ripe        = j.getInt("ri"),
            unripe      = j.getInt("un"),
            empty       = j.getInt("em"),
            rotten      = j.getInt("ro"),
            damaged     = j.getInt("da"),
            photoPath   = null,
            remarks     = j.optString("rm").ifEmpty { null },
            synced      = false
        )
    }

    // ─────────────────────────────────────────────────────────────
    // TRACTOR LOCATION  (prefix "TL:")
    // ─────────────────────────────────────────────────────────────

    fun encodeTractorLocation(t: TractorLocation): String {
        val j = JSONObject().apply {
            put("ti", t.tractorId)
            put("di", t.driverId)
            put("la", t.lat)
            put("lo", t.lon)
            put("ts", t.timestamp)
        }
        return "TL:$j"
    }

    fun decodeTractorLocation(raw: String): TractorLocation {
        val j = JSONObject(raw.removePrefix("TL:"))
        return TractorLocation(
            tractorId = j.getString("ti"),
            driverId  = j.getString("di"),
            lat       = j.getDouble("la"),
            lon       = j.getDouble("lo"),
            timestamp = j.getLong("ts")
        )
    }

    // ─────────────────────────────────────────────────────────────
    // GANG TRACK  (prefix "GT:")
    // Path stored as JSON array: [{la:x,lo:y,ts:z}, ...]
    // ─────────────────────────────────────────────────────────────

    fun encodeGangTrack(g: GangTrack): String {
        val j = JSONObject().apply {
            put("gi", g.gangId)
            put("gt", g.gangType)   // "PEST" | "FERT"
            put("pa", JSONArray(g.pathJson))
            put("ts", g.timestamp)
        }
        return "GT:$j"
    }

    fun decodeGangTrack(raw: String): GangTrack {
        val j = JSONObject(raw.removePrefix("GT:"))
        return GangTrack(
            gangId   = j.getString("gi"),
            gangType = j.getString("gt"),
            pathJson = j.getJSONArray("pa").toString(),
            timestamp = j.getLong("ts")
        )
    }

    // ─────────────────────────────────────────────────────────────
    // CHAT MESSAGE  (prefix "CM:")
    // ─────────────────────────────────────────────────────────────

    fun encodeChatMessage(m: ChatMessage): String {
        val j = JSONObject().apply {
            put("id", m.msgId)
            put("ch", m.channel)    // "HARVEST"|"PEST"|"FERT"
            put("sr", m.senderRole)
            put("tx", m.text)
            put("ts", m.timestamp)
        }
        return "CM:$j"
    }

    fun decodeChatMessage(raw: String, senderHash: String): ChatMessage {
        val j = JSONObject(raw.removePrefix("CM:"))
        return ChatMessage(
            msgId      = j.getString("id"),
            channel    = j.getString("ch"),
            senderHash = senderHash,
            senderRole = j.getString("sr"),
            text       = j.getString("tx"),
            timestamp  = j.getLong("ts"),
            delivered  = false,
            isOutgoing = false
        )
    }

    // ─────────────────────────────────────────────────────────────
    // ALERT  (prefix "AL:")
    // ─────────────────────────────────────────────────────────────

    fun encodeAlert(a: Alert): String {
        val j = JSONObject().apply {
            put("id", a.alertId)
            put("ty", a.type)
            put("bi", a.blockId)
            put("hi", a.harvesterId)
            put("ri", a.bunchRecordId)
            put("ts", a.timestamp)
        }
        return "AL:$j"
    }

    fun decodeAlert(raw: String): Alert {
        val j = JSONObject(raw.removePrefix("AL:"))
        return Alert(
            alertId       = j.getString("id"),
            type          = j.getString("ty"),
            blockId       = j.getString("bi"),
            harvesterId   = j.getString("hi"),
            bunchRecordId = j.getString("ri"),
            timestamp     = j.getLong("ts")
        )
    }

    // ─────────────────────────────────────────────────────────────
    // Routing helpers
    // ─────────────────────────────────────────────────────────────

    fun prefixOf(raw: String): String =
        if (raw.length >= 3) raw.substring(0, 3) else ""
}
