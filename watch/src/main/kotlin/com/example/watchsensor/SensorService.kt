package com.example.watchsensor

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.IBinder
import android.util.Log
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.core.app.NotificationCompat
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.*
import java.io.OutputStream
import java.util.UUID
import kotlin.coroutines.coroutineContext

class SensorService : Service(), SensorEventListener {

    companion object {
        val liveData = MutableLiveData<SensorData>()
        val APP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
        private const val CHANNEL_ID = "sensor_channel"
        private const val NOTIF_ID   = 1
    }

    private lateinit var sensorManager: SensorManager
    private var serverSocket: BluetoothServerSocket? = null
    private var clientSocket: BluetoothSocket? = null
    private var outputStream: OutputStream? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private var heartRate = 0
    private var steps     = 0
    private var accelX    = 0f
    private var accelY    = 0f
    private var accelZ    = 0f

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIF_ID, buildNotification("Waiting for phone..."))
        registerSensors()
        scope.launch { acceptLoop() }
        scope.launch { broadcastLoop() }
    }

    private fun registerSensors() {
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        listOf(Sensor.TYPE_HEART_RATE, Sensor.TYPE_STEP_COUNTER, Sensor.TYPE_ACCELEROMETER)
            .forEach { type ->
                sensorManager.getDefaultSensor(type)?.let {
                    sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
                }
            }
    }

    override fun onSensorChanged(event: SensorEvent) {
        when (event.sensor.type) {
            Sensor.TYPE_HEART_RATE   -> heartRate = event.values[0].toInt()
            Sensor.TYPE_STEP_COUNTER -> steps     = event.values[0].toInt()
            Sensor.TYPE_ACCELEROMETER -> {
                accelX = event.values[0]
                accelY = event.values[1]
                accelZ = event.values[2]
            }
        }
        liveData.postValue(SensorData(heartRate, steps, accelX, accelY, accelZ))
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    private suspend fun acceptLoop() {
        while (coroutineContext.isActive) {
            try {
                val adapter = BluetoothAdapter.getDefaultAdapter() ?: return
                serverSocket = adapter.listenUsingRfcommWithServiceRecord("WatchSensor", APP_UUID)
                updateNotification("Waiting for phone...")
                clientSocket = serverSocket!!.accept()
                outputStream = clientSocket!!.outputStream
                serverSocket?.close()
                updateNotification("Phone connected ✓")
            } catch (e: Exception) {
                delay(3000)
            }
        }
    }

    fun getCurrentTime(): String {
        val sdf = SimpleDateFormat("dd-MM-yyyy h:mm:ssa", Locale.getDefault())
        return sdf.format(Date())
    }

    private suspend fun broadcastLoop() {
        while (coroutineContext.isActive) {
            delay(1000)
            val stream = outputStream ?: continue
            try {
                val time = getCurrentTime()
                val msg = "$heartRate,$steps,$accelX,$accelY,$accelZ,$time\n"
                Log.d("TAG_HEALTH", "Sending: $msg")
                stream.write(msg.toByteArray())
            } catch (e: Exception) {
                Log.d("TAG_HEALTH", "Sending failed")
                outputStream = null
                clientSocket?.close()
                scope.launch { acceptLoop() }
            }
        }
    }

    private fun buildNotification(text: String): Notification =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("WatchSensor")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .build()

    private fun updateNotification(text: String) {
        getSystemService(NotificationManager::class.java).notify(NOTIF_ID, buildNotification(text))
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(CHANNEL_ID, "Sensor Service", NotificationManager.IMPORTANCE_LOW)
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        sensorManager.unregisterListener(this)
        scope.cancel()
        clientSocket?.close()
        serverSocket?.close()
    }
}
