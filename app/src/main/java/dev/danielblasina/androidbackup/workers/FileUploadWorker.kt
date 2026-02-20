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
import dev.danielblasina.androidbackup.database.FileState
import dev.danielblasina.androidbackup.files.FileManager
import java.io.File
import java.nio.file.Files
import java.nio.file.attribute.BasicFileAttributes
import java.util.logging.Logger

// check what is in queue, upload and update FileState
class FileUploadWorker(
    appContext: Context,
    workerParams: WorkerParameters,
) : Worker(appContext, workerParams) {
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
            val fileToUpload = fileChangeQueueDao.peek() ?: break
            logger.info(fileToUpload.filePath)
            val hash = FileManager(File(fileToUpload.filePath)).upload()
            fileChangeQueueDao.delete(fileToUpload)
            val fileAttr =
                Files.readAttributes(
                    File(fileToUpload.filePath).toPath(),
                    BasicFileAttributes::class.java,
                )

            val fileState =
                FileState(
                    filePath = fileToUpload.filePath,
                    hash = hash,
                    size = fileAttr.size(),
                    lastModifiedTime = fileAttr.lastModifiedTime().toInstant(),
                    creationTime = fileAttr.creationTime().toInstant(),
                )
            if (fileToUpload.actionType == FileActionType.REMOVE) {
                fileStataDao.delete(fileState)
            } else {
                fileStataDao.add(fileState)
            }
        }
        logger.info { "uploaded all files" }
        return Result.success()
    }

    companion object {
        fun start(applicationContext: Context) {
            val uploadWorkRequest = OneTimeWorkRequestBuilder<FileUploadWorker>().build()
            WorkManager
                .getInstance(applicationContext)
                .enqueueUniqueWork(
                    this::class.java.name,
                    ExistingWorkPolicy.REPLACE,
                    uploadWorkRequest,
                )
        }
    }
}
