package com.example.phonereceiver

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.phonereceiver.notification.BloodGlucoseViewController
import com.example.phonereceiver.nutritionlog.LogMealViewController
import com.example.phonereceiver.watchdata.SensorLiveData
import com.example.phonereceiver.watchdata.WatchSensorViewController
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.content.Intent
import com.example.phonereceiver.watchdata.WatchSensorService

class MainActivity : AppCompatActivity() {
    // Watch sensor UI
    private lateinit var watchView: WatchSensorViewController

    // ── Permission launcher (Bluetooth) ───────────────────────────────────────

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        if (results.values.all { it }) {
            startWatchService()
        } else {
            watchView.setStatus("Bluetooth permissions denied — no sensor data.")
            watchView.dimData(true)
        }
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

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

    override fun onDestroy() {
        super.onDestroy()
        // Service keeps running after Activity is destroyed — that's the point.
        // Call stopService() only if you want to shut it down with the app.
    }

    // ── Watch sensor ──────────────────────────────────────────────────────────

    private fun initWatchSensorSection() {
        watchView = WatchSensorViewController(this)
        watchView.dimData(true)

        // "Connect" button now starts the service (or re-requests perms)
        watchView.btnConnect.setOnClickListener { requestPermissionsAndStart() }

        // Observe LiveData published by the background service
        SensorLiveData.status.observe(this) { msg ->
            watchView.setStatus(msg)
        }
        SensorLiveData.sensorData.observe(this) { data ->
            watchView.setData(data)
            watchView.dimData(false)
        }

        requestPermissionsAndStart()
    }

    private fun requestPermissionsAndStart() {
        val perms = arrayOf(
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH_SCAN,
        )
        val missing = perms.filter { checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED }
        if (missing.isEmpty()) {
            startWatchService()
        } else {
            watchView.setStatus("Requesting Bluetooth permissions…")
            permissionLauncher.launch(missing.toTypedArray())
        }
    }

    private fun startWatchService() {
        val intent = Intent(this, WatchSensorService::class.java)
        startForegroundService(intent)
        // Do NOT set status here — SensorLiveData already holds the current
        // status from the service. Overwriting it would flash a stale message.
    }
}
