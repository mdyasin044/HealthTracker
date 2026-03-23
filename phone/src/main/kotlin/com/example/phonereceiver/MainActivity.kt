package com.example.phonereceiver

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import android.widget.GridLayout
import com.example.phonereceiver.notification.NotificationItem
import com.example.phonereceiver.notification.NotificationRepository
import kotlinx.coroutines.*
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.UUID
import java.util.Locale
import java.text.SimpleDateFormat
import java.util.Date
import android.widget.ProgressBar
import android.view.View
import androidx.lifecycle.lifecycleScope
import com.example.phonereceiver.nutritionlog.GeminiService

class MainActivity : AppCompatActivity() {

    // For log meal data ------------------------------------------------------------------------------

    private lateinit var etFood: EditText
    private lateinit var etAmount: EditText
    private lateinit var btnLogMeal:  Button
    private lateinit var progressBar: ProgressBar
    private lateinit var tvNutritionAmount: TextView

    // For watch sensor data ------------------------------------------------------------------------------
    companion object {
        val APP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    }

    private lateinit var tvStatus:  TextView
    private lateinit var tvHeart:   TextView
    private lateinit var tvSteps:   TextView
    private lateinit var tvAccelX:  TextView
    private lateinit var tvAccelY:  TextView
    private lateinit var tvAccelZ:  TextView
    private lateinit var tvLastSync: TextView
    private lateinit var btnConnect: LinearLayout

    private var btSocket: BluetoothSocket? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // For glucose level data ------------------------------------------------------------------------------
    private lateinit var glucoseGrid: GridLayout

    // For permissions ------------------------------------------------------------------------------
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

        val today = SimpleDateFormat("EEEE, MMM dd", Locale.getDefault()).format(Date())
        findViewById<TextView>(R.id.tvToday).text = today

        initLogMealSection()
        initWatchSensorSection()
        initBloodGlucoseSection()
    }

    // For log meal data ------------------------------------------------------------------------------
    private fun initLogMealSection() {
        etFood      = findViewById(R.id.etFood)
        etAmount    = findViewById(R.id.etAmount)
        btnLogMeal = findViewById(R.id.btnLogMeal)
        progressBar = findViewById(R.id.progressBar)
        tvNutritionAmount   = findViewById(R.id.tvNutritionAmount)

        btnLogMeal.setOnClickListener { analyze() }
    }
    private fun analyze() {
        val food   = etFood.text.toString().trim()
        val amount = etAmount.text.toString().trim()

        if (food.isEmpty() || amount.isEmpty()) {
            Toast.makeText(this, "Please enter food and amount", Toast.LENGTH_SHORT).show()
            return
        }

        setLoading(true)

        lifecycleScope.launch {
            try {
                val result = GeminiService.getNutrition(food, amount)
                tvNutritionAmount.text   = "Carbs: ${result.carbs}g, Protein: ${result.protein}g, Fat: ${result.fat}g"
                tvNutritionAmount.visibility = View.VISIBLE
            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, "Something went wrong", Toast.LENGTH_LONG).show()
                Log.e("TAG_HEALTH", "Error: ${e.message}")
            } finally {
                setLoading(false)
            }
        }
    }
    private fun setLoading(loading: Boolean) {
        progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        btnLogMeal.isEnabled   = !loading
        tvNutritionAmount.visibility = if (!loading) View.VISIBLE else View.GONE
    }

    // For glucose level data ------------------------------------------------------------------------------
    private fun initBloodGlucoseSection() {
        glucoseGrid = findViewById(R.id.glucoseGrid)
        findViewById<TextView>(R.id.tvDexcomStatus).text = "● Connected to Dexcom G6 Pro"

        if(!checkNotificationListenerPermission()) {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }

        NotificationRepository.listeners.add {
            runOnUiThread {
                updateGlucoseGrid(NotificationRepository.notifications)
            }
        }
    }

    private fun updateGlucoseGrid(glucoseReadings: List<NotificationItem>) {
        glucoseGrid.removeAllViews()
        glucoseGrid.columnCount = 4

        glucoseReadings.forEachIndexed { index, item: NotificationItem ->
            val isLatest = index == 0
            val value = item.title.filter { it.isDigit() || it == '.' }.toInt()
            val isHigh   = value > 140

            val cell = layoutInflater.inflate(R.layout.item_glucose_cell, glucoseGrid, false)
            cell.findViewById<TextView>(R.id.tvGlucoseTime).text  = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(item.timestamp))
            cell.findViewById<TextView>(R.id.tvGlucoseValue).text = value.toString()

            val bg = when {
                isLatest -> R.drawable.bg_glucose_now
                isHigh   -> R.drawable.bg_glucose_high
                else     -> R.drawable.bg_glucose_normal
            }
            cell.setBackgroundResource(bg)

            glucoseGrid.addView(cell)
        }
    }

    private fun checkNotificationListenerPermission(): Boolean {
        val enabled = Settings.Secure.getString(contentResolver, "enabled_notification_listeners")
        if (enabled == null || !enabled.contains(packageName)) {
            return false
        }
        return true
    }

    // For watch sensor data ------------------------------------------------------------------------------
    private fun initWatchSensorSection() {
        tvStatus   = findViewById(R.id.tvStatus)
        tvHeart    = findViewById(R.id.tvHeart)
        tvSteps    = findViewById(R.id.tvSteps)
        tvAccelX   = findViewById(R.id.tvAccelX)
        tvAccelY   = findViewById(R.id.tvAccelY)
        tvAccelZ   = findViewById(R.id.tvAccelZ)
        tvLastSync = findViewById(R.id.tvLastSync)
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

        // At the beginning, connect to the watch
        tryConnectToWatch()

        // If connection failed or disconnected, connect to the watch again
        btnConnect.setOnClickListener {
            tryConnectToWatch()
        }
    }

    private fun tryConnectToWatch() {
        if (permissionsGranted) connectToWatch()
        else setStatus("Bluetooth permissions are required.")
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
                updateStatus("● Connected to ${watch.name}")
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
                Log.d("TAG_HEALTH", "Reading starts...")
                val line = reader.readLine() ?: break
                Log.d("TAG_HEALTH", "Received: $line")
                val parts = line.split(",")
                if (parts.size >= 6) {
                    val hr = parts[0].trim()
                    val st = parts[1].trim()
                    val ax = parts[2].trim().toFloatOrNull()?.let { "%.2f".format(it) } ?: "--"
                    val ay = parts[3].trim().toFloatOrNull()?.let { "%.2f".format(it) } ?: "--"
                    val az = parts[4].trim().toFloatOrNull()?.let { "%.2f".format(it) } ?: "--"
                    val time = parts[5].trim()
                    withContext(Dispatchers.Main) {
                        tvHeart.text  = "$hr bpm"
                        tvSteps.text  = st
                        tvAccelX.text = "X: $ax"
                        tvAccelY.text = "Y: $ay"
                        tvAccelZ.text = "Z: $az"
                        tvLastSync.text = "Last sync: $time"
                    }
                }
            }
        } catch (e: Exception) {
            updateStatus("Disconnected. Retrying...")
            dimDataMain(true)
            enableBtn()
            tryConnectToWatch()
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
