package com.example.phonereceiver.watchdata

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import com.example.phonereceiver.MainActivity
import com.example.phonereceiver.R
import android.content.BroadcastReceiver
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.IntentFilter
/**
 * Foreground service that keeps the Bluetooth connection to the watch alive
 * even when MainActivity is not in the foreground.
 *
 * Lifecycle:
 *  - Started by MainActivity via startForegroundService()
 *  - Posts all updates through SensorLiveData (no direct Activity reference)
 *  - Stopped explicitly via stopSelf() or Context.stopService()
 */
class WatchSensorService : Service() {

    companion object {
        const val CHANNEL_ID   = "watch_sensor_channel"
        const val NOTIFICATION_ID = 1
        const val ACTION_STOP  = "ACTION_STOP_WATCH_SERVICE"
    }

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private lateinit var watchManager: WatchSensorManager
    private var isConnected = false

    // ── Service lifecycle ─────────────────────────────────────────────────────

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        registerBluetoothReceiver()

        watchManager = WatchSensorManager(
            scope            = scope,
            onStatusChange   = { msg  -> SensorLiveData.status.postValue(msg) },
            onDataReceived   = { data ->
                isConnected = true
                SensorLiveData.sensorData.postValue(data)
            },
            onConnectionLost = {
                isConnected = false
                SensorLiveData.status.postValue("Disconnected. Retrying…")
            },
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }

        startForeground(NOTIFICATION_ID, buildNotification())

        // Service is already running and connected — app was just reopened.
        // Don't touch the live socket; the Activity will receive current state
        // via SensorLiveData on its next observe() call.
        if (!isConnected) {
            watchManager.connect()
        }

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(bluetoothReceiver)
        scope.cancel()
        watchManager.close()
    }

    // Services don't bind to Activities in this design — LiveData is the bridge
    override fun onBind(intent: Intent?): IBinder? = null

    // ── Bluetooth status change ──────────────────────────────────────────────────────────
    private val bluetoothReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action != BluetoothAdapter.ACTION_STATE_CHANGED) return
            val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
            when (state) {
                BluetoothAdapter.STATE_ON -> {
                    // BT just turned back on — reconnect
                    isConnected = false
                    SensorLiveData.status.postValue("Bluetooth on. Reconnecting…")
                    watchManager.connect()
                }
                BluetoothAdapter.STATE_OFF -> {
                    isConnected = false
                    SensorLiveData.status.postValue("Bluetooth turned off.")
                }
            }
        }
    }

    private fun registerBluetoothReceiver() {
        val filter = IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
        registerReceiver(bluetoothReceiver, filter)
    }

    // ── Notification ──────────────────────────────────────────────────────────

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Watch sensor",
            NotificationManager.IMPORTANCE_LOW,     // silent, no sound
        ).apply {
            description = "Shows while the watch sensor service is running"
        }
        getSystemService(NotificationManager::class.java)
            .createNotificationChannel(channel)
    }

    private fun buildNotification() = NotificationCompat.Builder(this, CHANNEL_ID)
        .setContentTitle("Watch sensor active")
        .setContentText("Collecting heart rate, steps and accelerometer data")
        .setSmallIcon(R.drawable.ic_sync)
        .setOngoing(true)
        .setContentIntent(openAppPendingIntent())
        .addAction(
            android.R.drawable.ic_menu_close_clear_cancel,
            "Stop",
            stopServicePendingIntent(),
        )
        .build()

    private fun openAppPendingIntent(): PendingIntent {
        val intent = Intent(this, MainActivity::class.java)
        return PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }

    private fun stopServicePendingIntent(): PendingIntent {
        val intent = Intent(this, WatchSensorService::class.java).apply {
            action = ACTION_STOP
        }
        return PendingIntent.getService(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }
}