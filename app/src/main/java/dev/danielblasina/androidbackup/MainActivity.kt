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
import androidx.compose.ui.Modifier
import dev.danielblasina.androidbackup.ui.theme.MyApplicationTheme
import dev.danielblasina.androidbackup.workers.ChecksumChecker
import dev.danielblasina.androidbackup.workers.FileChangeDetector
import dev.danielblasina.androidbackup.workers.FileChangeScheduler
import dev.danielblasina.androidbackup.workers.FileStateReconcile
import dev.danielblasina.androidbackup.workers.FileUploadScheduler
import dev.danielblasina.androidbackup.workers.FileUploadWorker
import java.util.logging.Logger

class MainActivity : ComponentActivity() {
    val logger: Logger = Logger.getLogger(this.javaClass.name)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Environment.isExternalStorageManager()) {
            // Permission granted
        } else {
            val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
            startActivity(intent)
        }

        FileChangeScheduler.start(applicationContext)
        FileUploadScheduler.start(applicationContext)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Column {
                        Button(onClick = {
                            logger.info { "FileChangeDetector was requested to start" }
                            FileChangeDetector.start(applicationContext)
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
                            ChecksumChecker.start(applicationContext)
                        }, modifier = Modifier.padding(innerPadding)) {
                            Text("Start ChecksumChecker")
                        }
                        Button(onClick = {
                            logger.info { "FileStateReconcile was requested to start" }
                            FileStateReconcile.start(applicationContext)
                        }, modifier = Modifier.padding(innerPadding)) {
                            Text("Start FileStateReconcile")
                        }
                    }
                }
            }
        }
    }
}
