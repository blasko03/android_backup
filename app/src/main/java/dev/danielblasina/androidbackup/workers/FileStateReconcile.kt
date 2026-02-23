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
import dev.danielblasina.androidbackup.files.UploadedFile
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
            val fileStates = fileStateDao.getNextServerCheck(
                from = Instant.now().minus(1, ChronoUnit.MINUTES),
                limit = 1000,
            )
            if (fileStates.isEmpty()) {
                logger.info { "Completed reconciliation check for all files" }
                return Result.success()
            }
            val fileState = fileStates.first()
            val filesToCheck = fileStates.map { f -> UploadedFile(name = File(f.filePath).toPath(), hash = f.hash) }
            val filesToCheckResult = FileUploadService().filesPresent(filesToCheck.toList()).getOrThrow()
            filesToCheckResult.filter { f -> !f.present }
                .forEach { notFoundFile ->
                    logger.info { "Detected file not found on server for ${fileState.filePath} adding to FileChangeQueue" }
                    val change = FileChangeQueue(
                        filePath = fileState.filePath,
                        enqueuedAt = Instant.now(),
                        actionType = FileActionType.CHANGE,
                    )
                    db.fileChangeQueueDao().add(change)
                }
            fileStateDao.setServerCheck(
                filePath = fileStates.map { fs -> fs.filePath }.toList(),
                instant = Instant.now(),
            )
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
