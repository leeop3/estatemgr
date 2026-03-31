package com.estate.manager.rns

import android.content.Context
import android.util.Log
import com.chaquo.python.Python
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Thin Kotlin wrapper around rns_backend.py.
 * Every public method maps 1-to-1 to a Python function.
 * DO NOT add logic here — keep it a pure bridge.
 */
class RnsManager(private val context: Context) {

    private val TAG = "RnsManager"
    private val py   by lazy { Python.getInstance() }
    private val rns  by lazy { py.getModule("rns_backend") }

    /**
     * Maps to: start_rns(storage_path, callback_obj, nickname)
     * Returns the local destination hash as hex string.
     */
    suspend fun start(callback: RnsCallback, nickname: String): String =
        withContext(Dispatchers.IO) {
            val storagePath = context.filesDir.absolutePath
            val result = rns.callAttr("start_rns", storagePath, callback, nickname)
            val hash = result.toString()
            Log.i(TAG, "RNS started — local hash: $hash")
            hash
        }

    /**
     * Maps to: inject_rnode(freq, bw, tx, sf, cr)
     * Returns "ONLINE" on success or an error string.
     * Default params tuned for 868 MHz EU / 915 MHz AS band field deployment.
     */
    suspend fun injectRnode(
        freq:    Long = 865_000_000L,
        bw:      Int  = 125_000,
        txPower: Int  = 17,
        sf:      Int  = 9,
        cr:      Int  = 5
    ): String = withContext(Dispatchers.IO) {
        val result = rns.callAttr("inject_rnode", freq, bw, txPower, sf, cr).toString()
        Log.i(TAG, "inject_rnode → $result")
        result
    }

    /**
     * Maps to: send_text(dest_hex, text)
     * All estate message types (BR:, TL:, GT:, CM:, AL:) go through here.
     * Returns the LXMessage hash hex string, or "" on failure.
     */
    suspend fun sendPacket(destHex: String, payload: String): String =
        withContext(Dispatchers.IO) {
            val hash = rns.callAttr("send_text", destHex, payload).toString()
            Log.d(TAG, "send_text → msgHash=$hash")
            hash
        }

    /**
     * Maps to: send_image(dest_hex, path)
     * Encodes file as base64 and sends with IMG: prefix via send_text.
     */
    suspend fun sendImage(destHex: String, localPath: String): String =
        withContext(Dispatchers.IO) {
            val hash = rns.callAttr("send_image", destHex, localPath).toString()
            Log.d(TAG, "send_image → msgHash=$hash")
            hash
        }

    /**
     * Maps to: announce_now()
     * Broadcasts this node's identity + display_name as app_data over RNS.
     */
    suspend fun announce() = withContext(Dispatchers.IO) {
        rns.callAttr("announce_now")
        Log.i(TAG, "Announce broadcast sent")
    }
}
