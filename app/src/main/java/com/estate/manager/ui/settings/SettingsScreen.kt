package com.estate.manager.ui.settings

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter

@Composable
fun SettingsScreen() {
    val ctx    = LocalContext.current
    val prefs  = remember { ctx.getSharedPreferences("estate_prefs", Context.MODE_PRIVATE) }

    var managerNick by remember { mutableStateOf(prefs.getString("manager_nickname", "Manager:Unknown") ?: "") }
    var myHash      by remember { mutableStateOf(prefs.getString("my_dest_hash", "") ?: "") }
    var freq        by remember { mutableStateOf(prefs.getLong("rnode_freq", 865_000_000L).toString()) }
    var bw          by remember { mutableStateOf(prefs.getInt("rnode_bw", 125_000).toString()) }
    var tx          by remember { mutableStateOf(prefs.getInt("rnode_tx", 17).toString()) }
    var sf          by remember { mutableStateOf(prefs.getInt("rnode_sf", 9).toString()) }
    var cr          by remember { mutableStateOf(prefs.getInt("rnode_cr", 5).toString()) }
    var saveStatus  by remember { mutableStateOf("") }

    val qrBitmap: Bitmap? = remember(myHash) {
        if (myHash.isNotEmpty()) generateQr(myHash) else null
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        // ── Identity ──────────────────────────────────────────────
        Text("Node Identity", style = MaterialTheme.typography.titleMedium)

        OutlinedTextField(
            value         = managerNick,
            onValueChange = { managerNick = it },
            label         = { Text("Display Name (ROLE:Name)") },
            placeholder   = { Text("Manager:Ahmad") },
            modifier      = Modifier.fillMaxWidth(),
            singleLine    = true
        )

        if (myHash.isNotEmpty()) {
            Text(
                myHash,
                fontFamily = FontFamily.Monospace,
                style      = MaterialTheme.typography.bodySmall
            )
            qrBitmap?.let {
                Image(
                    bitmap             = it.asImageBitmap(),
                    contentDescription = "My destination QR",
                    modifier           = Modifier.size(200.dp).align(Alignment.CenterHorizontally)
                )
            }
        } else {
            Text(
                "Start RNS to generate identity",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        HorizontalDivider()

        // ── RNode radio parameters ────────────────────────────────
        Text("RNode Radio Parameters", style = MaterialTheme.typography.titleMedium)

        RnodeField("Frequency (Hz)", freq, KeyboardType.Number) { freq = it }
        RnodeField("Bandwidth (Hz)", bw,   KeyboardType.Number) { bw   = it }
        RnodeField("TX Power (dBm)", tx,   KeyboardType.Number) { tx   = it }
        RnodeField("Spreading Factor (7–12)", sf, KeyboardType.Number) { sf = it }
        RnodeField("Coding Rate (5–8)",       cr, KeyboardType.Number) { cr = it }

        Button(
            onClick = {
                with(prefs.edit()) {
                    putString("manager_nickname", managerNick)
                    putLong  ("rnode_freq",       freq.toLongOrNull() ?: 865_000_000L)
                    putInt   ("rnode_bw",          bw.toIntOrNull()   ?: 125_000)
                    putInt   ("rnode_tx",          tx.toIntOrNull()   ?: 17)
                    putInt   ("rnode_sf",          sf.toIntOrNull()   ?: 9)
                    putInt   ("rnode_cr",          cr.toIntOrNull()   ?: 5)
                    apply()
                }
                saveStatus = "Saved — restart app to apply radio changes"
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Save Settings") }

        if (saveStatus.isNotEmpty()) {
            Text(saveStatus, style = MaterialTheme.typography.bodySmall,
                 color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun RnodeField(
    label:    String,
    value:    String,
    kbType:   KeyboardType,
    onChange: (String) -> Unit
) {
    OutlinedTextField(
        value         = value,
        onValueChange = onChange,
        label         = { Text(label) },
        modifier      = Modifier.fillMaxWidth(),
        singleLine    = true,
        keyboardOptions = KeyboardOptions(keyboardType = kbType)
    )
}

private fun generateQr(content: String, size: Int = 400): Bitmap? {
    return try {
        val bits = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, size, size)
        val bmp  = Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565)
        for (x in 0 until size)
            for (y in 0 until size)
                bmp.setPixel(x, y, if (bits[x, y]) Color.BLACK else Color.WHITE)
        bmp
    } catch (e: Exception) { null }
}

// Stub activity required by manifest USB filter entry
class UsbPermissionActivity : android.app.Activity()
