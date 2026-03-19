package com.example.phonereceiver

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.*
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.UUID

class MainActivity : AppCompatActivity() {

    companion object {
        val APP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    }

    private lateinit var tvStatus:  TextView
    private lateinit var tvHeart:   TextView
    private lateinit var tvSteps:   TextView
    private lateinit var tvAccelX:  TextView
    private lateinit var tvAccelY:  TextView
    private lateinit var tvAccelZ:  TextView
    private lateinit var btnConnect: Button

    private var btSocket: BluetoothSocket? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var permissionsGranted = false

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        permissionsGranted = results.values.all { it }
        if (permissionsGranted) {
            setStatus("Permissions granted. Tap Connect.")
        } else {
            setStatus("Permissions denied. No data available.")
            dimData(true)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvStatus   = findViewById(R.id.tvStatus)
        tvHeart    = findViewById(R.id.tvHeart)
        tvSteps    = findViewById(R.id.tvSteps)
        tvAccelX   = findViewById(R.id.tvAccelX)
        tvAccelY   = findViewById(R.id.tvAccelY)
        tvAccelZ   = findViewById(R.id.tvAccelZ)
        btnConnect = findViewById(R.id.btnConnect)

        dimData(true)

        val perms = arrayOf(
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH_SCAN
        )
        val missing = perms.filter { checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED }
        if (missing.isEmpty()) {
            permissionsGranted = true
            setStatus("Tap Connect to find watch.")
        } else {
            setStatus("Requesting Bluetooth permissions...")
            permissionLauncher.launch(missing.toTypedArray())
        }

        btnConnect.setOnClickListener {
            if (permissionsGranted) connectToWatch()
            else setStatus("Bluetooth permissions are required.")
        }
    }

    private fun connectToWatch() {
        setStatus("Scanning paired devices...")
        btnConnect.isEnabled = false

        scope.launch {
            try {
                val adapter = BluetoothAdapter.getDefaultAdapter()
                    ?: run { updateStatus("Bluetooth not available."); enableBtn(); return@launch }

                val watch: BluetoothDevice? = adapter.bondedDevices.firstOrNull { device ->
                    device.name?.contains("Galaxy Watch", ignoreCase = true) == true ||
                    device.name?.contains("Watch", ignoreCase = true) == true
                }

                if (watch == null) {
                    updateStatus("Galaxy Watch not found in paired devices.\nPair it in Bluetooth settings first.")
                    enableBtn(); return@launch
                }

                updateStatus("Connecting to ${watch.name}...")
                btSocket?.close()
                btSocket = watch.createRfcommSocketToServiceRecord(APP_UUID)
                btSocket!!.connect()
                updateStatus("Connected to ${watch.name} ✓")
                dimDataMain(false)
                readLoop()

            } catch (e: Exception) {
                updateStatus("Connection failed: ${e.message}\n\nMake sure the Watch app is running first.")
                enableBtn()
            }
        }
    }

    private suspend fun readLoop() {
        try {
            val reader = BufferedReader(InputStreamReader(btSocket?.inputStream))
            while (true) {
                val line = reader.readLine() ?: break
                val parts = line.split(",")
                if (parts.size == 5) {
                    val hr = parts[0].trim()
                    val st = parts[1].trim()
                    val ax = parts[2].trim().toFloatOrNull()?.let { "%.2f".format(it) } ?: "--"
                    val ay = parts[3].trim().toFloatOrNull()?.let { "%.2f".format(it) } ?: "--"
                    val az = parts[4].trim().toFloatOrNull()?.let { "%.2f".format(it) } ?: "--"
                    withContext(Dispatchers.Main) {
                        tvHeart.text  = "$hr bpm"
                        tvSteps.text  = st
                        tvAccelX.text = "X: $ax"
                        tvAccelY.text = "Y: $ay"
                        tvAccelZ.text = "Z: $az"
                    }
                }
            }
        } catch (e: Exception) {
            updateStatus("Disconnected. Tap Connect to retry.")
            dimDataMain(true)
            enableBtn()
        }
    }

    private fun dimData(dim: Boolean) = runOnUiThread { dimDataMain(dim) }
    private fun dimDataMain(dim: Boolean) {
        val a = if (dim) 0.3f else 1f
        listOf(tvHeart, tvSteps, tvAccelX, tvAccelY, tvAccelZ).forEach { it.alpha = a }
    }

    private fun setStatus(msg: String) { tvStatus.text = msg }
    private fun updateStatus(msg: String) = runOnUiThread { tvStatus.text = msg }
    private fun enableBtn() = runOnUiThread { btnConnect.isEnabled = true }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
        btSocket?.close()
    }
}
