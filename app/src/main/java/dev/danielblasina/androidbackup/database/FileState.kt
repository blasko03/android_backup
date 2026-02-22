package dev.danielblasina.androidbackup.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant

@Entity
data class FileState(
    @PrimaryKey val filePath: String,
    val hash: ByteArray,
    val size: Long,
    val creationTime: Instant,
    val lastModifiedTime: Instant,
    val lastServerCheck: Instant,
    val lastHashCheck: Instant,
)
