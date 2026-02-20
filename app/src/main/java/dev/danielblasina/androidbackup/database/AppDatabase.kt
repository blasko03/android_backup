package dev.danielblasina.androidbackup.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

const val DATABASE_NAME = "backups"

@Database(entities = [FileChangeQueue::class, FileState::class], version = 1)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun fileChangeQueueDao(): FileChangeQueueDao

    abstract fun fileStateDao(): FileStateDao
}
