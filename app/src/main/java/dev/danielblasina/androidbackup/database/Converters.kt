package dev.danielblasina.androidbackup.database

import androidx.room.TypeConverter
import java.time.Instant

class Converters {
    @TypeConverter
    fun toInstant(epoch: Long): Instant {
        return Instant.ofEpochMilli(epoch)
    }
    @TypeConverter
    fun fromInstant(instant: Instant): Long {
        return instant.toEpochMilli()
    }
}