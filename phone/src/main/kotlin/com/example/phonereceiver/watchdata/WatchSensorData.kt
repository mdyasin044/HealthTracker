package com.example.phonereceiver.watchdata

data class WatchSensorData(
    val heartRate: String,
    val steps: String,
    val accelX: String,
    val accelY: String,
    val accelZ: String,
    val lastSync: String,
)
