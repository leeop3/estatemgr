package com.estate.manager.rns

import android.app.*
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.estate.manager.R
import com.estate.manager.data.AppDatabase
import com.estate.manager.data.repository.*
import kotlinx.coroutines.*

/**
 * Foreground service that keeps RNS running while the app is in the background.
 * Started from MainActivity on first launch.
 * Holds the MessageRouter and RnsManager references for the process lifetime.
 */
class RnsService : Service() {

    private val TAG = "RnsService"
    private val NOTIF_CHANNEL = "rns_channel"
    private val NOTIF_ID      = 1001

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    lateinit var rnsManager:    RnsManager
    lateinit var messageRouter: MessageRouter

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIF_ID, buildNotification("RNS starting…"))

        val db = AppDatabase.get(applicationContext)
        messageRouter = MessageRouter(
            bunchRepo   = BunchRepository(db.bunchDao()),
            tractorRepo = TractorRepository(db.tractorDao()),
            gangRepo    = GangRepository(db.gangDao()),
            chatRepo    = ChatRepository(db.chatDao()),
            alertRepo   = AlertRepository(db.alertDao()),
            peerRepo    = PeerRepository(db.peerDao()),
            scope       = serviceScope
        )
        rnsManager = RnsManager(applicationContext)

        serviceScope.launch {
            try {
                val prefs   = applicationContext.getSharedPreferences("estate_prefs", MODE_PRIVATE)
                val nick    = prefs.getString("manager_nickname", "Manager:Unknown") ?: "Manager:Unknown"
                val hash    = rnsManager.start(messageRouter, nick)
                updateNotification("RNS online · $hash")
                Log.i(TAG, "RNS started, hash=$hash")

                // Inject RNode with persisted or default params
                val freq = prefs.getLong("rnode_freq", 865_000_000L)
                val bw   = prefs.getInt ("rnode_bw",   125_000)
                val tx   = prefs.getInt ("rnode_tx",   17)
                val sf   = prefs.getInt ("rnode_sf",   9)
                val cr   = prefs.getInt ("rnode_cr",   5)
                val status = rnsManager.injectRnode(freq, bw, tx, sf, cr)
                Log.i(TAG, "RNode status: $status")
                updateNotification("LoRa $status · $hash")

                rnsManager.announce()
            } catch (e: Exception) {
                Log.e(TAG, "RNS startup error", e)
                updateNotification("RNS error: ${e.message}")
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int =
        START_STICKY   // restart if killed by OS

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = NotificationChannel(
                NOTIF_CHANNEL,
                "Estate RNS Radio",
                NotificationManager.IMPORTANCE_LOW
            ).apply { description = "Keeps LoRa/RNS radio link active" }
            getSystemService(NotificationManager::class.java).createNotificationChannel(ch)
        }
    }

    private fun buildNotification(text: String): Notification {
        val pi = PendingIntent.getActivity(
            this, 0,
            packageManager.getLaunchIntentForPackage(packageName),
            PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, NOTIF_CHANNEL)
            .setContentTitle("Estate Manager")
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_radio)
            .setContentIntent(pi)
            .setOngoing(true)
            .build()
    }

    private fun updateNotification(text: String) {
        val nm = getSystemService(NotificationManager::class.java)
        nm.notify(NOTIF_ID, buildNotification(text))
    }
}
