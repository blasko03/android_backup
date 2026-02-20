package dev.danielblasina.androidbackup.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query


@Dao
interface FileStateDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun add(fileStates: List<FileState>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun add(fileState: FileState)

    @Query("SELECT * FROM FileState ORDER BY filePath")
    fun listAll(): List<FileState>

    @Delete
    fun delete(fileState: FileState)
}