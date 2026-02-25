package dev.danielblasina.androidbackup

import android.content.Intent
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.danielblasina.androidbackup.database.AppDatabase
import dev.danielblasina.androidbackup.ui.theme.MyApplicationTheme
import dev.danielblasina.androidbackup.workers.ChecksumCheckScheduler
import dev.danielblasina.androidbackup.workers.ChecksumCheckWorker
import dev.danielblasina.androidbackup.workers.FileChangeScheduler
import dev.danielblasina.androidbackup.workers.FileChangeWorker
import dev.danielblasina.androidbackup.workers.FileStateReconcileScheduler
import dev.danielblasina.androidbackup.workers.FileStateReconcileWorker
import dev.danielblasina.androidbackup.workers.FileUploadScheduler
import dev.danielblasina.androidbackup.workers.FileUploadWorker
import java.time.Instant
import java.util.logging.Logger
import kotlin.concurrent.thread

class MainActivity : ComponentActivity() {
    val logger: Logger = Logger.getLogger(this.javaClass.name)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        var queueCount = 0
        var fileCount = 0
        var countNextServerCheck = 0
        var countNextHashCheck = 0

        thread {
            val db = AppDatabase.getDatabase(applicationContext)
            queueCount = db.fileChangeQueueDao().count()
            fileCount = db.fileStateDao().count()
            countNextServerCheck = db.fileStateDao().countNextServerCheck(Instant.now().minus(FileStateReconcileWorker.checkFrequency))
            countNextHashCheck = db.fileStateDao().countNextHashCheck(Instant.now().minus(ChecksumCheckWorker.checkFrequency))
        }
        if (Environment.isExternalStorageManager()) {
            // Permission granted
        } else {
            val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
            startActivity(intent)
        }

        FileChangeScheduler.start(applicationContext)
        FileUploadScheduler.start(applicationContext)
        FileStateReconcileScheduler.start(applicationContext)
        ChecksumCheckScheduler.start(applicationContext)

        enableEdgeToEdge()
        setContent {
            MainApplication(queueCount = queueCount, filesCount = fileCount, countNextServerCheck = countNextServerCheck, countNextHashCheck = countNextHashCheck)
        }
    }

    @Composable
    private fun MainApplication(queueCount: Int, filesCount: Int, countNextServerCheck: Int, countNextHashCheck: Int) {
        MyApplicationTheme {
            Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                Column {
                    Button(onClick = {
                        logger.info { "FileChangeDetector was requested to start" }
                        FileChangeWorker.start(applicationContext)
                    }, modifier = Modifier.padding(innerPadding)) {
                        Text("Start FileChangeDetector")
                    }
                    Button(onClick = {
                        logger.info { "FileUploadWorker was requested to start" }
                        FileUploadWorker.start(applicationContext)
                    }, modifier = Modifier.padding(innerPadding)) {
                        Text("Start FileUploadWorker")
                    }
                    Button(onClick = {
                        logger.info { "ChecksumChecker was requested to start" }
                        ChecksumCheckWorker.start(applicationContext)
                    }, modifier = Modifier.padding(innerPadding)) {
                        Text("Start ChecksumChecker")
                    }
                    Button(onClick = {
                        logger.info { "FileStateReconcile was requested to start" }
                        FileStateReconcileWorker.start(applicationContext)
                    }, modifier = Modifier.padding(innerPadding)) {
                        Text("Start FileStateReconcile")
                    }
                    Text("Total in queue: $queueCount")
                    Text("Total files: $filesCount")
                    Text("Total reconciliation check: $countNextServerCheck")
                    Text("Total hash check: $countNextHashCheck")
                }
            }
        }
    }
}
