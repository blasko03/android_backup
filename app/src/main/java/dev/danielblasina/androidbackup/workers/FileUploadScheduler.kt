package dev.danielblasina.androidbackup.workers

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit
import java.util.logging.Logger

class FileUploadScheduler(appContext: Context, workerParams: WorkerParameters) : Worker(appContext, workerParams) {
    val logger: Logger = Logger.getLogger(this.javaClass.name)

    override fun doWork(): Result {
        logger.info { "Started ${this.javaClass.name}" }
        FileUploadWorker.start(applicationContext)
        return Result.success()
    }

    companion object {
        fun start(applicationContext: Context) {
            val work = PeriodicWorkRequestBuilder<FileUploadScheduler>(15, TimeUnit.MINUTES)
                .build()
            WorkManager.getInstance(applicationContext)
                .enqueueUniquePeriodicWork(
                    this::class.java.name,
                    ExistingPeriodicWorkPolicy.UPDATE,
                    work,
                )
        }
    }
}
