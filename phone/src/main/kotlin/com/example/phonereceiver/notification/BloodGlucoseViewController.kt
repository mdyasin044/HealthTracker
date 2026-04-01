package com.example.phonereceiver.notification

import android.content.Intent
import android.provider.Settings
import android.widget.GridLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.phonereceiver.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class BloodGlucoseViewController(private val activity: AppCompatActivity) {

    private val glucoseGrid: GridLayout = activity.findViewById(R.id.glucoseGrid)

    init {
        if (!checkNotificationListenerPermission()) {
            activity.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }

        NotificationRepository.listeners.add {
            activity.runOnUiThread {
                updateGlucoseGrid(NotificationRepository.notifications)
            }
        }
    }

    private fun updateGlucoseGrid(glucoseReadings: List<NotificationItem>) {
        glucoseGrid.removeAllViews()
        glucoseGrid.columnCount = 4

        glucoseReadings.forEachIndexed { index, item: NotificationItem ->
            val isLatest = index == 0
            val value    = item.title.filter { it.isDigit() || it == '.' }.toInt()
            val isHigh   = value > 140

            val cell = activity.layoutInflater.inflate(R.layout.item_glucose_cell, glucoseGrid, false)
            cell.findViewById<TextView>(R.id.tvGlucoseTime).text  =
                SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(item.timestamp))
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
        val enabled = Settings.Secure.getString(
            activity.contentResolver, "enabled_notification_listeners"
        )
        return enabled != null && enabled.contains(activity.packageName)
    }
}