package com.example.phonereceiver.notification
data class NotificationItem(
    val title: String,
    val text: String,
    val packageName: String,
    val timestamp: Long,
    val key: String = "$packageName-$timestamp"
)