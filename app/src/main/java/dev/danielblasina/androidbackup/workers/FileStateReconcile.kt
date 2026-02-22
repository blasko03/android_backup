package dev.danielblasina.androidbackup.workers

import android.content.Context
import androidx.room.Room
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import dev.danielblasina.androidbackup.database.AppDatabase
import dev.danielblasina.androidbackup.database.DATABASE_NAME
import dev.danielblasina.androidbackup.database.FileActionType
import dev.danielblasina.androidbackup.database.FileChangeQueue
import dev.danielblasina.androidbackup.files.FileUploadService
import dev.danielblasina.androidbackup.utils.NotFoundError
import java.io.File
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.logging.Logger

class FileStateReconcile(appContext: Context, workerParams: WorkerParameters) : Worker(appContext, workerParams) {
    val db =
        Room
            .databaseBuilder(
                applicationContext,
                AppDatabase::class.java,
                DATABASE_NAME,
            ).build()
    val logger: Logger = Logger.getLogger(this.javaClass.name)

    override fun doWork(): Result {
        logger.info { "FileStateReconcile started" }
        val fileStateDao = db.fileStateDao()
        var iteration = 0
        while (iteration < MAX_ITERATIONS) {
            iteration++
            val fileState = fileStateDao.getNextServerCheck(from = Instant.now().minus(1, ChronoUnit.DAYS))
            if (fileState == null) {
                logger.info { "Completed reconciliation check for all files" }
                return Result.success()
            }
            logger.info { "Check if ${fileState.filePath} is on server" }
            FileUploadService().filePresent(filePath = File(fileState.filePath).toPath(), hash = fileState.hash)
                .onFailure { e ->
                    when (e) {
                        is NotFoundError -> {
                            logger.info { "Detected file not found on server for ${fileState.filePath} adding to FileChangeQueue" }
                            val change = FileChangeQueue(
                                filePath = fileState.filePath,
                                enqueuedAt = Instant.now(),
                                actionType = FileActionType.CHANGE,
                            )
                            db.fileChangeQueueDao().add(change)
                        }

                        else -> {
                            logger.severe(e.toString())
                            return Result.failure()
                        }
                    }
                }

            fileStateDao.setServerCheck(fileState.filePath, Instant.now())
        }

        return Result.success()
    }
    companion object {
        fun start(applicationContext: Context) {
            val work = OneTimeWorkRequestBuilder<FileStateReconcile>()
                .build()
            WorkManager
                .getInstance(applicationContext)
                .enqueueUniqueWork(
                    this::class.java.name,
                    ExistingWorkPolicy.KEEP,
                    work,
                )
        }
    }
}
