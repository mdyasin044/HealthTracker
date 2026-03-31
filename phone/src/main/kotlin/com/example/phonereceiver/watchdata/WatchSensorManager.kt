package com.example.phonereceiver.watchdata

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothSocket
import android.util.Log
import kotlinx.coroutines.*
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.UUID

class WatchSensorManager(
    private val scope: CoroutineScope,
    private val onStatusChange: (String) -> Unit,
    private val onDataReceived: (WatchSensorData) -> Unit,
    private val onConnectionLost: () -> Unit,
) {
    companion object {
        val APP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    }

    private var btSocket: BluetoothSocket? = null

    fun connect() {
        onStatusChange("Scanning paired devices...")

        scope.launch {
            try {
                val adapter = BluetoothAdapter.getDefaultAdapter()
                    ?: run { onStatusChange("Bluetooth not available."); return@launch }

                val watch = adapter.bondedDevices.firstOrNull { device ->
                    device.name?.contains("Galaxy Watch", ignoreCase = true) == true ||
                    device.name?.contains("Watch", ignoreCase = true) == true
                } ?: run {
                    onStatusChange("Galaxy Watch not found in paired devices.\nPair it in Bluetooth settings first.")
                    return@launch
                }

                onStatusChange("Connecting to ${watch.name}...")
                btSocket?.close()
                btSocket = watch.createRfcommSocketToServiceRecord(APP_UUID)
                btSocket!!.connect()
                onStatusChange("● Connected to ${watch.name}")
                readLoop()

            } catch (e: Exception) {
                onStatusChange("Connection failed: ${e.message}\n\nMake sure the Watch app is running first.")
                onConnectionLost()
            }
        }
    }

    private suspend fun readLoop() {
        try {
            val reader = BufferedReader(InputStreamReader(btSocket?.inputStream))
            while (true) {
                Log.d("TAG_HEALTH", "Reading starts...")
                val line = reader.readLine() ?: break
                Log.d("TAG_HEALTH", "Received: $line")
                val parts = line.split(",")
                if (parts.size >= 6) {
                    val data = WatchSensorData(
                        heartRate = parts[0].trim(),
                        steps     = parts[1].trim(),
                        accelX    = parts[2].trim().toFloatOrNull()?.let { "%.2f".format(it) } ?: "--",
                        accelY    = parts[3].trim().toFloatOrNull()?.let { "%.2f".format(it) } ?: "--",
                        accelZ    = parts[4].trim().toFloatOrNull()?.let { "%.2f".format(it) } ?: "--",
                        lastSync  = parts[5].trim(),
                    )
                    withContext(Dispatchers.Main) { onDataReceived(data) }
                }
            }
        } catch (e: Exception) {
            onStatusChange("Disconnected. Retrying...")
            onConnectionLost()
            connect()
        }
    }

    fun close() {
        btSocket?.close()
    }
}
