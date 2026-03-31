package com.example.phonereceiver.watchdata

import android.app.Activity
import android.widget.LinearLayout
import android.widget.TextView
import com.example.phonereceiver.R

class WatchSensorViewController(activity: Activity) {

    private val tvStatus:   TextView    = activity.findViewById(R.id.tvStatus)
    private val tvHeart:    TextView    = activity.findViewById(R.id.tvHeart)
    private val tvSteps:    TextView    = activity.findViewById(R.id.tvSteps)
    private val tvAccelX:   TextView    = activity.findViewById(R.id.tvAccelX)
    private val tvAccelY:   TextView    = activity.findViewById(R.id.tvAccelY)
    private val tvAccelZ:   TextView    = activity.findViewById(R.id.tvAccelZ)
    private val tvLastSync: TextView    = activity.findViewById(R.id.tvLastSync)
    val btnConnect:         LinearLayout = activity.findViewById(R.id.btnConnect)

    fun setStatus(msg: String) {
        tvStatus.text = msg
    }

    fun setData(data: WatchSensorData) {
        tvHeart.text    = "${data.heartRate} bpm"
        tvSteps.text    = data.steps
        tvAccelX.text   = "X: ${data.accelX}"
        tvAccelY.text   = "Y: ${data.accelY}"
        tvAccelZ.text   = "Z: ${data.accelZ}"
        tvLastSync.text = "Last sync: ${data.lastSync}"
    }

    fun dimData(dim: Boolean) {
        val alpha = if (dim) 0.3f else 1f
        listOf(tvHeart, tvSteps, tvAccelX, tvAccelY, tvAccelZ).forEach { it.alpha = alpha }
    }

    fun setConnectEnabled(enabled: Boolean) {
        btnConnect.isEnabled = enabled
    }
}
