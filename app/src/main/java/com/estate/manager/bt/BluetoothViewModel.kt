package com.estate.manager.bt

import android.app.Application
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class BtDeviceInfo(
    val name:    String,
    val address: String
)

enum class BridgeState { IDLE, CONNECTING, ACTIVE, ERROR }

class BluetoothViewModel(app: Application) : AndroidViewModel(app) {

    private val prefs = app.getSharedPreferences("estate_prefs", Context.MODE_PRIVATE)

    // ── Paired device list ────────────────────────────────────────
    private val _pairedDevices = MutableStateFlow<List<BtDeviceInfo>>(emptyList())
    val pairedDevices: StateFlow<List<BtDeviceInfo>> = _pairedDevices.asStateFlow()

    // ── Selected device ───────────────────────────────────────────
    private val _selectedDevice = MutableStateFlow(
        prefs.getString("bt_device_address", "") ?: ""
    )
    val selectedDevice: StateFlow<String> = _selectedDevice.asStateFlow()

    // ── Bridge state ──────────────────────────────────────────────
    private val _bridgeState = MutableStateFlow(BridgeState.IDLE)
    val bridgeState: StateFlow<BridgeState> = _bridgeState.asStateFlow()

    // ── Status text ───────────────────────────────────────────────
    private val _status = MutableStateFlow("Not connected")
    val status: StateFlow<String> = _status.asStateFlow()

    private var bridgeJob: Job? = null

    init { refreshPairedDevices() }

    // ─────────────────────────────────────────────────────────────
    // Scan already-paired devices (no permission needed for paired)
    // ─────────────────────────────────────────────────────────────
    fun refreshPairedDevices() {
        val adapter = BluetoothAdapter.getDefaultAdapter()
        if (adapter == null) {
            _status.value = "Bluetooth not available on this device"
            return
        }
        if (!adapter.isEnabled) {
            _status.value = "Bluetooth is OFF — enable it and retry"
            return
        }
        val paired: Set<BluetoothDevice> = adapter.bondedDevices ?: emptySet()
        _pairedDevices.value = paired.map {
            BtDeviceInfo(name = it.name ?: "Unknown", address = it.address)
        }.sortedBy { it.name }
        _status.value = "${_pairedDevices.value.size} paired device(s) found"
    }

    // ─────────────────────────────────────────────────────────────
    // Select a device and persist choice
    // ─────────────────────────────────────────────────────────────
    fun selectDevice(address: String) {
        _selectedDevice.value = address
        prefs.edit().putString("bt_device_address", address).apply()
        _status.value = "Device selected: $address"
    }

    // ─────────────────────────────────────────────────────────────
    // Start the BT SPP ↔ TCP bridge
    // ─────────────────────────────────────────────────────────────
    fun startBridge() {
        val address = _selectedDevice.value
        if (address.isEmpty()) {
            _status.value = "Select a device first"
            return
        }
        if (BluetoothSppBridge.isRunning()) {
            _status.value = "Bridge already running"
            _bridgeState.value = BridgeState.ACTIVE
            return
        }

        _bridgeState.value = BridgeState.CONNECTING
        bridgeJob = viewModelScope.launch(Dispatchers.IO) {
            BluetoothSppBridge.start(address) { msg ->
                _status.value = msg
                _bridgeState.value = when {
                    msg.contains("active",      ignoreCase = true) -> BridgeState.ACTIVE
                    msg.contains("error",       ignoreCase = true) -> BridgeState.ERROR
                    msg.contains("disconnected",ignoreCase = true) -> BridgeState.IDLE
                    else -> _bridgeState.value
                }
            }
            // Bridge exited
            _bridgeState.value = BridgeState.IDLE
        }
    }

    // ─────────────────────────────────────────────────────────────
    // Stop bridge
    // ─────────────────────────────────────────────────────────────
    fun stopBridge() {
        BluetoothSppBridge.stop()
        bridgeJob?.cancel()
        _bridgeState.value = BridgeState.IDLE
        _status.value = "Bridge stopped"
    }

    override fun onCleared() {
        super.onCleared()
        BluetoothSppBridge.stop()
    }
}
