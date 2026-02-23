package dev.danielblasina.androidbackup.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import java.time.Instant

@Dao
interface FileStateDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun add(fileStates: List<FileState>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun add(fileState: FileState)

    @Query("SELECT * FROM FileState ORDER BY filePath")
    fun listAll(): List<FileState>

    @Query(
        "SELECT * FROM FileState " +
            "LEFT JOIN FileChangeQueue ON FileChangeQueue.filePath == FileState.filePath " +
            "WHERE FileState.lastHashCheck < :from " +
            "ORDER BY FileState.lastHashCheck ASC " +
            "LIMIT 1",
    )
    fun getNextHashCheck(from: Instant): FileState?

    @Query("UPDATE FileState SET lastHashCheck = :instant WHERE filePath = :filePath")
    fun setHashCheck(filePath: String, instant: Instant)

    @Query(
        "SELECT * FROM FileState " +
            "LEFT JOIN FileChangeQueue ON FileChangeQueue.filePath == FileState.filePath " +
            "WHERE FileState.lastServerCheck < :from " +
            "ORDER BY FileState.lastServerCheck ASC " +
            "LIMIT :limit",
    )
    fun getNextServerCheck(from: Instant, limit: Int = 1): Array<FileState>

    @Query("UPDATE FileState SET lastServerCheck = :instant WHERE filePath IN (:filePath)")
    fun setServerCheck(filePath: List<String>, instant: Instant)

    @Query("DELETE FROM FileState WHERE filePath = :filePath")
    fun delete(filePath: String)
}
