package com.example.watchsensor

data class SensorData(
    val heartRate: Int = 0,
    val steps: Int = 0,
    val accelX: Float = 0f,
    val accelY: Float = 0f,
    val accelZ: Float = 0f
)
