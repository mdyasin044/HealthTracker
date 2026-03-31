package com.example.phonereceiver

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.phonereceiver.notification.BloodGlucoseViewController
import com.example.phonereceiver.nutritionlog.LogMealViewController
import com.example.phonereceiver.watchdata.WatchSensorManager
import com.example.phonereceiver.watchdata.WatchSensorViewController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private lateinit var watchView: WatchSensorViewController
    private lateinit var watchManager: WatchSensorManager

    private var permissionsGranted = false
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        permissionsGranted = results.values.all { it }
        if (permissionsGranted) {
            watchView.setStatus("Permissions granted. Tap Connect.")
        } else {
            watchView.setStatus("Permissions denied. No data available.")
            watchView.dimData(true)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setTodayStatus()

        LogMealViewController(this)
        BloodGlucoseViewController(this)
        initWatchSensorSection()
    }

    private fun setTodayStatus() {
        val today = SimpleDateFormat("EEEE, MMM dd", Locale.getDefault()).format(Date())
        findViewById<TextView>(R.id.tvToday).text = today
    }

    // ── watch init ─────────────────────────────────────────────────────────────

    private fun initWatchSensorSection() {
        watchView = WatchSensorViewController(this)
        watchManager = WatchSensorManager(
            scope           = scope,
            onStatusChange  = { msg -> runOnUiThread { watchView.setStatus(msg) } },
            onDataReceived  = { data ->
                watchView.setData(data)
                watchView.dimData(false)
            },
            onConnectionLost = {
                runOnUiThread {
                    watchView.dimData(true)
                    watchView.setConnectEnabled(true)
                }
            },
        )

        watchView.dimData(true)

        val perms = arrayOf(
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH_SCAN,
        )
        val missing = perms.filter { checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED }
        if (missing.isEmpty()) {
            permissionsGranted = true
            watchView.setStatus("Tap Connect to find watch.")
            watchManager.connect()
        } else {
            watchView.setStatus("Requesting Bluetooth permissions...")
            permissionLauncher.launch(missing.toTypedArray())
        }

        watchView.btnConnect.setOnClickListener {
            if (permissionsGranted) {
                watchView.setConnectEnabled(false)
                watchManager.connect()
            } else {
                watchView.setStatus("Bluetooth permissions are required.")
            }
        }
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
        watchManager.close()
    }
}
