package com.estate.manager.ui.settings

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.chaquo.python.Python
import com.estate.manager.data.AppDatabase
import com.estate.manager.data.models.Peer
import com.estate.manager.data.repository.PeerRepository
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class RnodeConfig(
    val freq:    Long   = 865_000_000L,
    val bw:      Int    = 125_000,
    val txPower: Int    = 17,
    val sf:      Int    = 9,
    val cr:      Int    = 5
)

class SettingsViewModel(app: Application) : AndroidViewModel(app) {

    private val prefs = app.getSharedPreferences("estate_prefs", Context.MODE_PRIVATE)
    private val db    = AppDatabase.get(app)
    private val peerRepo = PeerRepository(db.peerDao())

    // ── Identity ──────────────────────────────────────────────────
    private val _myHash = MutableStateFlow(prefs.getString("my_dest_hash", "") ?: "")
    val myHash: StateFlow<String> = _myHash.asStateFlow()

    private val _nickname = MutableStateFlow(prefs.getString("manager_nickname", "Manager:Unknown") ?: "")
    val nickname: StateFlow<String> = _nickname.asStateFlow()

    private val _qrBitmap = MutableStateFlow<Bitmap?>(null)
    val qrBitmap: StateFlow<Bitmap?> = _qrBitmap.asStateFlow()

    // ── RNode config ──────────────────────────────────────────────
    private val _config = MutableStateFlow(loadConfig())
    val config: StateFlow<RnodeConfig> = _config.asStateFlow()

    // ── Status messages ───────────────────────────────────────────
    private val _status = MutableStateFlow("")
    val status: StateFlow<String> = _status.asStateFlow()

    // ── Peers ─────────────────────────────────────────────────────
    val peers = peerRepo.all()

    init {
        // Load hash from prefs and generate QR on init
        val hash = prefs.getString("my_dest_hash", "") ?: ""
        if (hash.isNotEmpty()) {
            _myHash.value = hash
            generateQr(hash)
        }
    }

    // ─────────────────────────────────────────────────────────────
    // Called after RNS starts — saves hash and generates QR
    // ─────────────────────────────────────────────────────────────
    fun onRnsStarted(hash: String) {
        prefs.edit().putString("my_dest_hash", hash).apply()
        _myHash.value = hash
        generateQr(hash)
    }

    // ─────────────────────────────────────────────────────────────
    // Save nickname + RNode params to SharedPrefs
    // ─────────────────────────────────────────────────────────────
    fun saveSettings(nick: String, cfg: RnodeConfig) {
        with(prefs.edit()) {
            putString("manager_nickname", nick)
            putLong  ("rnode_freq",  cfg.freq)
            putInt   ("rnode_bw",    cfg.bw)
            putInt   ("rnode_tx",    cfg.txPower)
            putInt   ("rnode_sf",    cfg.sf)
            putInt   ("rnode_cr",    cfg.cr)
            apply()
        }
        _nickname.value = nick
        _config.value   = cfg
        _status.value   = "Saved. Restart app to apply radio changes."
    }

    // ─────────────────────────────────────────────────────────────
    // Apply RNode config immediately via Python bridge
    // ─────────────────────────────────────────────────────────────
    fun applyRnodeNow(cfg: RnodeConfig) {
        viewModelScope.launch(Dispatchers.IO) {
            _status.value = "Applying radio config..."
            try {
                val py     = Python.getInstance()
                val rns    = py.getModule("rns_backend")
                val result = rns.callAttr(
                    "inject_rnode",
                    cfg.freq, cfg.bw, cfg.txPower, cfg.sf, cfg.cr
                ).toString()
                _status.value = "Radio: $result"
            } catch (e: Exception) {
                _status.value = "Error: ${e.message}"
            }
        }
    }

    // ─────────────────────────────────────────────────────────────
    // Broadcast RNS announce
    // ─────────────────────────────────────────────────────────────
    fun announce() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val py  = Python.getInstance()
                val rns = py.getModule("rns_backend")
                rns.callAttr("announce_now")
                _status.value = "Announce broadcast sent."
            } catch (e: Exception) {
                _status.value = "Announce error: ${e.message}"
            }
        }
    }

    // ─────────────────────────────────────────────────────────────
    // Add peer manually by hex hash + role + name
    // ─────────────────────────────────────────────────────────────
    fun addPeerManually(hash: String, role: String, name: String) {
        if (hash.length < 16) { _status.value = "Hash too short"; return }
        viewModelScope.launch(Dispatchers.IO) {
            peerRepo.upsert(Peer(
                destHash    = hash.lowercase().trim(),
                displayName = name.trim(),
                role        = role.uppercase().trim(),
                lastSeen    = System.currentTimeMillis()
            ))
            _status.value = "Peer $name added."
        }
    }

    // ─────────────────────────────────────────────────────────────
    // Called after QR scan returns a dest hash string
    // Format expected: "ROLE:Name:hexhash"  or just "hexhash"
    // ─────────────────────────────────────────────────────────────
    fun onQrScanned(raw: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val parts = raw.trim().split(":")
            when {
                // Full format: ROLE:Name:hash
                parts.size >= 3 -> {
                    val role = parts[0].uppercase()
                    val name = parts[1]
                    val hash = parts[2].lowercase()
                    peerRepo.upsert(Peer(hash, name, role, System.currentTimeMillis()))
                    _status.value = "Peer added: $name ($role)"
                }
                // Hash only
                parts.size == 1 && raw.length >= 16 -> {
                    peerRepo.upsert(Peer(raw.lowercase(), "Unknown", "UNKNOWN", System.currentTimeMillis()))
                    _status.value = "Peer added: ${raw.take(12)}..."
                }
                else -> _status.value = "Invalid QR data: $raw"
            }
        }
    }

    // ─────────────────────────────────────────────────────────────
    // Delete a peer
    // ─────────────────────────────────────────────────────────────
    fun deletePeer(peer: Peer) {
        viewModelScope.launch(Dispatchers.IO) {
            db.peerDao().delete(peer)
            _status.value = "Peer ${peer.displayName} removed."
        }
    }

    // ─────────────────────────────────────────────────────────────
    // Set a peer as the active supervisor for a channel
    // ─────────────────────────────────────────────────────────────
    fun setSupervisor(channel: String, destHash: String) {
        prefs.edit().putString("supervisor_$channel", destHash).apply()
        _status.value = "Supervisor set for $channel"
    }

    fun getSupervisor(channel: String): String =
        prefs.getString("supervisor_$channel", "") ?: ""

    // ─────────────────────────────────────────────────────────────
    private fun loadConfig() = RnodeConfig(
        freq    = prefs.getLong("rnode_freq",  865_000_000L),
        bw      = prefs.getInt ("rnode_bw",    125_000),
        txPower = prefs.getInt ("rnode_tx",    17),
        sf      = prefs.getInt ("rnode_sf",    9),
        cr      = prefs.getInt ("rnode_cr",    5)
    )

    private fun generateQr(content: String) {
        viewModelScope.launch(Dispatchers.Default) {
            try {
                val size = 512
                val bits = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, size, size)
                val bmp  = Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565)
                for (x in 0 until size)
                    for (y in 0 until size)
                        bmp.setPixel(x, y, if (bits[x, y]) Color.BLACK else Color.WHITE)
                _qrBitmap.value = bmp
            } catch (e: Exception) {
                _status.value = "QR error: ${e.message}"
            }
        }
    }
}
