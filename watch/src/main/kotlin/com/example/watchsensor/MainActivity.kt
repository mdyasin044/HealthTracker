package com.example.watchsensor

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts

class MainActivity : ComponentActivity() {

    private lateinit var tvStatus: TextView
    private lateinit var tvHeart: TextView
    private lateinit var tvSteps: TextView
    private lateinit var tvAccel: TextView

    private val requiredPermissions = arrayOf(
        Manifest.permission.BODY_SENSORS,
        Manifest.permission.ACTIVITY_RECOGNITION,
        Manifest.permission.BLUETOOTH_CONNECT,
        Manifest.permission.BLUETOOTH_SCAN,
        Manifest.permission.BLUETOOTH_ADVERTISE
    )

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        if (results.values.all { it }) {
            startSensorService()
            tvStatus.text = "Running ✓"
        } else {
            tvStatus.text = "Permissions denied.\nNo data available."
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvStatus = findViewById(R.id.tvStatus)
        tvHeart  = findViewById(R.id.tvHeart)
        tvSteps  = findViewById(R.id.tvSteps)
        tvAccel  = findViewById(R.id.tvAccel)

        val missing = requiredPermissions.filter {
            checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED
        }

        if (missing.isEmpty()) {
            startSensorService()
            tvStatus.text = "Running ✓"
        } else {
            tvStatus.text = "Requesting permissions..."
            permissionLauncher.launch(missing.toTypedArray())
        }

        SensorService.liveData.observe(this) { data ->
            tvHeart.text = "❤️ ${data.heartRate} bpm"
            tvSteps.text = "👟 ${data.steps} steps"
            tvAccel.text = "📡 X:${"%.1f".format(data.accelX)} Y:${"%.1f".format(data.accelY)} Z:${"%.1f".format(data.accelZ)}"
        }
    }

    private fun startSensorService() {
        startForegroundService(Intent(this, SensorService::class.java))
    }
}
