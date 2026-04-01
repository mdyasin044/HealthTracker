package com.example.phonereceiver.watchdata

import androidx.lifecycle.MutableLiveData

/**
 * Singleton LiveData objects that the foreground service posts to.
 * Any Activity / Fragment can observe these without holding a reference
 * to the service itself.
 */
object SensorLiveData {
    val sensorData: MutableLiveData<WatchSensorData> = MutableLiveData()
    val status:     MutableLiveData<String>          = MutableLiveData()
}
