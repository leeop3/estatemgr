package com.estate.manager.bt

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.ServerSocket
import java.net.Socket
import java.util.UUID

/**
 * Bluetooth SPP ↔ TCP bridge.
 *
 * The LilyGO T-Beam / T3 running RNode firmware exposes a classic BT
 * Serial Port Profile (SPP) RFCOMM channel.
 *
 * rns_backend.py → inject_rnode() connects to tcp://127.0.0.1:7633.
 *
 * This bridge:
 *   1. Accepts one TCP connection on 127.0.0.1:7633  (from Python)
 *   2. Opens RFCOMM BT socket to the T-Beam
 *   3. Pipes bytes bidirectionally in background threads
 *
 * Lifecycle: start once after BT pairing, keep running as long as
 * RnsService is alive.
 */
object BluetoothSppBridge {

    private val TAG = "BtSppBridge"

    // Standard SPP UUID — same on all RNode-compatible firmware
    private val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    // TCP port rns_backend.py inject_rnode() connects to
    private const val TCP_PORT = 7633

    @Volatile private var running      = false
    @Volatile private var btSocket:    BluetoothSocket? = null
    @Volatile private var tcpServer:   ServerSocket?    = null
    @Volatile private var tcpClient:   Socket?          = null

    // ─────────────────────────────────────────────────────────────
    // Public API
    // ─────────────────────────────────────────────────────────────

    /**
     * Start the bridge for the given paired BT device address.
     * Runs blocking — call from a background coroutine / thread.
     *
     * @param deviceAddress  MAC address of the T-Beam e.g. "AA:BB:CC:DD:EE:FF"
     * @param onStatus       callback for UI status messages
     */
    suspend fun start(deviceAddress: String, onStatus: (String) -> Unit) =
        withContext(Dispatchers.IO) {
            if (running) { onStatus("Bridge already running"); return@withContext }
            running = true

            try {
                // ── Step 1: Connect BT RFCOMM socket to T-Beam ───────────
                onStatus("Connecting to T-Beam ($deviceAddress)...")
                val adapter = BluetoothAdapter.getDefaultAdapter()
                    ?: throw IOException("No Bluetooth adapter found")

                val device: BluetoothDevice = adapter.getRemoteDevice(deviceAddress)
                adapter.cancelDiscovery()   // must cancel discovery before RFCOMM connect

                val socket = device.createRfcommSocketToServiceRecord(SPP_UUID)
                socket.connect()            // blocks ~5 s
                btSocket = socket
                onStatus("T-Beam connected via BT SPP")
                Log.i(TAG, "BT RFCOMM connected to $deviceAddress")

                val btIn:  InputStream  = socket.inputStream
                val btOut: OutputStream = socket.outputStream

                // ── Step 2: Open local TCP server for Python ──────────────
                onStatus("Opening TCP bridge on port $TCP_PORT...")
                val server = ServerSocket(TCP_PORT)
                tcpServer  = server
                onStatus("Waiting for RNS Python to connect on port $TCP_PORT...")
                Log.i(TAG, "TCP server listening on $TCP_PORT")

                val tcp = server.accept()   // blocks until inject_rnode() calls
                tcpClient = tcp
                val tcpIn:  InputStream  = tcp.inputStream
                val tcpOut: OutputStream = tcp.outputStream
                onStatus("RNS connected — bridge active (BT ↔ TCP)")
                Log.i(TAG, "Bridge active")

                // ── Step 3: Pipe bytes bidirectionally ────────────────────
                val buf = ByteArray(4096)

                // BT → TCP  (RNode → Python)
                val btToTcp = Thread {
                    try {
                        while (running) {
                            val n = btIn.read(buf)
                            if (n < 0) break
                            tcpOut.write(buf, 0, n)
                            tcpOut.flush()
                        }
                    } catch (e: Exception) {
                        if (running) Log.w(TAG, "BT→TCP pipe closed: ${e.message}")
                    }
                }.also { it.isDaemon = true; it.start() }

                // TCP → BT  (Python → RNode)
                val tcpToBt = Thread {
                    try {
                        while (running) {
                            val n = tcpIn.read(buf)
                            if (n < 0) break
                            btOut.write(buf, 0, n)
                            btOut.flush()
                        }
                    } catch (e: Exception) {
                        if (running) Log.w(TAG, "TCP→BT pipe closed: ${e.message}")
                    }
                }.also { it.isDaemon = true; it.start() }

                // Wait for either pipe to die
                btToTcp.join()
                tcpToBt.join()

                onStatus("Bridge disconnected")
                Log.i(TAG, "Bridge pipes closed")

            } catch (e: Exception) {
                onStatus("Bridge error: ${e.message}")
                Log.e(TAG, "Bridge error", e)
            } finally {
                stop()
            }
        }

    fun stop() {
        running = false
        runCatching { tcpClient?.close() }
        runCatching { tcpServer?.close() }
        runCatching { btSocket?.close() }
        tcpClient = null
        tcpServer = null
        btSocket  = null
        Log.i(TAG, "Bridge stopped")
    }

    fun isRunning() = running
}
