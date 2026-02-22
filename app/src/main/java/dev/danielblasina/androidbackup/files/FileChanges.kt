package dev.danielblasina.androidbackup.files

import dev.danielblasina.androidbackup.database.FileActionType
import dev.danielblasina.androidbackup.database.FileChangeQueue
import dev.danielblasina.androidbackup.database.FileState
import java.io.File
import java.nio.file.Files
import java.nio.file.attribute.BasicFileAttributes
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.logging.Logger

class FileChanges(val directory: File) {
    val logger: Logger = Logger.getLogger(this.javaClass.name)

    fun listFiles(recursive: Boolean = false): List<File> {
        val files =
            directory
                .listFiles()
                ?.filterNotNull()
                ?.flatMap { file ->
                    if ((file.isDirectory && recursive)) {
                        FileChanges(file).listFiles(recursive = true)
                    } else if (file.isDirectory) {
                        listOf()
                    } else {
                        listOf(file)
                    }
                }
                ?: listOf()
        return files
    }

    fun listChanges(
        recursive: Boolean = false,
        uploadedFiles: List<FileState>,
    ): List<FileChangeQueue> {
        val files = listFiles(recursive = recursive).associateBy { it.path }.toMutableMap()
        val uploadedFilesMap = uploadedFiles.associateBy { it.filePath }.toMutableMap()

        val changes = HashMap<String, FileChangeQueue>()

        for (file in files) {
            val uploadedFile = uploadedFilesMap[file.key]
            val action = getAction(file.value, uploadedFile)
            if (action != FileActionType.NONE) {
                logger.info { "file has changed $action, ${file.value.path}" }
                changes[file.key] =
                    FileChangeQueue(
                        filePath = file.value.path,
                        enqueuedAt = Instant.now(),
                        actionType = action,
                    )
            }
            uploadedFilesMap.remove(file.key)
        }

        for (file in uploadedFilesMap) {
            changes[file.key] =
                FileChangeQueue(
                    filePath = file.value.filePath,
                    enqueuedAt = Instant.now(),
                    actionType = FileActionType.REMOVE,
                )
        }

        return changes.map { change -> change.value }
    }

    private fun getAction(file: File, uploadedFile: FileState?): FileActionType {
        if (uploadedFile == null) {
            return FileActionType.ADD
        }

        if (hasChanged(file, uploadedFile)) {
            return FileActionType.CHANGE
        }

        return FileActionType.NONE
    }

    private fun hasChanged(file: File, uploadedFile: FileState): Boolean {
        val fileAttr: BasicFileAttributes =
            Files.readAttributes(
                file.toPath(),
                BasicFileAttributes::class.java,
            )
        if (fileAttr.lastModifiedTime().toInstant().truncatedTo(ChronoUnit.MILLIS) != uploadedFile.lastModifiedTime) {
            return true
        }
        if (fileAttr.creationTime().toInstant().truncatedTo(ChronoUnit.MILLIS) != uploadedFile.creationTime) {
            return true
        }
        if (fileAttr.size() != uploadedFile.size) {
            return true
        }
        return false
    }
}
