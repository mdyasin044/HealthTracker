package com.example.phonereceiver
object NotificationRepository {
    private val _notifications = mutableListOf<NotificationItem>()
    val notifications: List<NotificationItem> get() = _notifications.toList()

    val listeners = mutableListOf<() -> Unit>()

    fun add(item: NotificationItem) {
        _notifications.add(0, item) // newest first
        if (_notifications.size > 20) { // Keep the latest 20 items
            _notifications.removeAt(_notifications.lastIndex)
        }
        listeners.forEach { it() }
    }

    fun remove(key: String) {
        _notifications.removeAll { it.key == key }
        listeners.forEach { it() }
    }
}