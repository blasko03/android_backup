package dev.danielblasina.androidbackup.workers

import android.content.Context
import androidx.room.Room
import androidx.work.BackoffPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.ExperimentalWorkRequestBuilderApi
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import dev.danielblasina.androidbackup.database.AppDatabase
import dev.danielblasina.androidbackup.database.DATABASE_NAME
import dev.danielblasina.androidbackup.database.FileActionType
import dev.danielblasina.androidbackup.database.FileState
import dev.danielblasina.androidbackup.files.FileUpload
import java.io.File
import java.io.FileNotFoundException
import java.nio.file.Files
import java.nio.file.attribute.BasicFileAttributes
import java.time.Instant
import java.util.concurrent.TimeUnit
import java.util.logging.Logger

class FileUploadWorker(appContext: Context, workerParams: WorkerParameters) : Worker(appContext, workerParams) {
    val logger: Logger = Logger.getLogger(this.javaClass.name)
    val db =
        Room
            .databaseBuilder(
                applicationContext,
                AppDatabase::class.java,
                DATABASE_NAME,
            ).build()

    override fun doWork(): Result {
        val fileChangeQueueDao = db.fileChangeQueueDao()
        val fileStataDao = db.fileStateDao()
        while (true) {
            val fileChange = fileChangeQueueDao.peek() ?: break
            try {
                logger.info("Picked from queue ${fileChange.filePath} action: ${fileChange.actionType}")
                if (fileChange.actionType == FileActionType.REMOVE) {
                    fileStataDao.delete(fileChange.filePath)
                } else {
                    val hash = FileUpload(File(fileChange.filePath)).upload()
                    val fileAttr = Files.readAttributes(
                        File(fileChange.filePath).toPath(),
                        BasicFileAttributes::class.java,
                    )

                    val fileState = FileState(
                        filePath = fileChange.filePath,
                        hash = hash,
                        size = fileAttr.size(),
                        lastModifiedTime = fileAttr.lastModifiedTime().toInstant(),
                        creationTime = fileAttr.creationTime().toInstant(),
                        lastHashCheck = Instant.now(),
                        lastServerCheck = Instant.now(),
                    )
                    fileStataDao.add(fileState)
                }
                fileChangeQueueDao.delete(fileChange)
            } catch (e: FileNotFoundException) {
                logger.warning { "Wasn't able to upload file ${fileChange.filePath} with error ${e.message}" }
                fileChangeQueueDao.delete(fileChange)
            }
            logger.info { "queue has ${db.fileChangeQueueDao().count()} elements" }
        }
        logger.info { "uploaded all files" }
        return Result.success()
    }

    companion object {
        @OptIn(ExperimentalWorkRequestBuilderApi::class)
        fun start(applicationContext: Context) {
            val work = OneTimeWorkRequestBuilder<FileUploadWorker>()
                .setBackoffCriteria(BackoffPolicy.LINEAR, 1, TimeUnit.MINUTES)
                .setBackoffForSystemInterruptions()
                .build()
            WorkManager.getInstance(applicationContext)
                .enqueueUniqueWork(
                    this::class.java.name,
                    ExistingWorkPolicy.KEEP,
                    work,
                )
        }
    }
}
