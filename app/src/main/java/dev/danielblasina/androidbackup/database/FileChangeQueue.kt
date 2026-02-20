package dev.danielblasina.androidbackup.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant

@Entity
data class FileChangeQueue(
    @PrimaryKey val filePath: String,
    val enqueuedAt: Instant,
    val actionType: FileActionType,
)
