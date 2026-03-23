package com.example.phonereceiver.notification

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.TextView

class MyNotificationListener : NotificationListenerService() {

    private val targetPackage = "com.dexcom.g6"

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val extras = sbn.notification.extras
        var title = extras.getString("android.title") ?: "No Title"
        var text = extras.getCharSequence("android.text")?.toString() ?: "No Content"
        val packageName = sbn.packageName
        Log.d("TAG_HEALTH", "Notification received: $title, $text, $packageName")
        // Filter out other notifications
        if (packageName != targetPackage) {
            // For test purpose: send dummy data
//            val notification = NotificationItem(
//                title = "140 mg/dL",
//                text = "",
//                packageName = packageName,
//                timestamp = sbn.postTime
//            )
//
//            NotificationRepository.add(notification)
            return
        }

        // Extract title from customView
        if (title == "No Title" || text == "No Content") {
            title = extractTitle(sbn)
            text = ""
        }
        else {
            return
        }

        val notification = NotificationItem(
            title = title,
            text = text,
            packageName = packageName,
            timestamp = sbn.postTime
        )

        NotificationRepository.add(notification)
    }

    private fun extractTitle(sbn: StatusBarNotification): String {
        val contentView = sbn.notification.contentView
            ?: sbn.notification.bigContentView
            ?: sbn.notification.headsUpContentView

        if (contentView != null) {
            try {
                val context = createPackageContext(
                    targetPackage,
                    CONTEXT_IGNORE_SECURITY
                )
                val view = contentView.apply(context, null)

                // Walk all TextViews inside the custom view
                val texts = mutableListOf<String>()
                extractTextViews(view, texts)
                Log.d("TAG_HEALTH", "Extracted texts: $texts")
                val merged = texts.joinToString(" ")
                return merged
            } catch (e: Exception) {
                Log.e("TAG_HEALTH", "Failed to read custom view: ${e.message}")
            }
        }
        else {
            Log.e("TAG_HEALTH", "ContentView is null")
        }
        return ""
    }

    private fun extractTextViews(view: View, texts: MutableList<String>) {
        if (view is TextView) {
            val text = view.text?.toString()
            if (!text.isNullOrBlank()) texts.add(text)
        } else if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                extractTextViews(view.getChildAt(i), texts)
            }
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        NotificationRepository.remove(sbn.key)
    }
}