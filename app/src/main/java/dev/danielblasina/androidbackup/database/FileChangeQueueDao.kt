package dev.danielblasina.androidbackup.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface FileChangeQueueDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun add(fileChanges: List<FileChangeQueue>)

    @Query("SELECT COUNT(*) FROM FileChangeQueue")
    fun count(): Int

    @Query("SELECT * FROM FileChangeQueue ORDER BY enqueuedAt ASC LIMIT 1")
    fun peek(): FileChangeQueue?

    @Delete
    fun delete(fileChange: FileChangeQueue)
}
