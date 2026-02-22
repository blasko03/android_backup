package dev.danielblasina.androidbackup.files

import androidx.work.NetworkType

//      server: address, credentials
//      folders: list[File]
//      constraints: requiredNetworkType, requiresCharging, requiresDeviceIdle, requiresBatteryNotLow
//      schedule: frequency / time

data class SyncTask(
    val requiredNetworkType: NetworkType,
    val requiresCharging: Boolean,
    val requiresDeviceIdle: Boolean,
    val requiresBatteryNotLow: Boolean,
)
