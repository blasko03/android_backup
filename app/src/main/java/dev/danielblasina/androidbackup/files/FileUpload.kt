package dev.danielblasina.androidbackup.files

import dev.danielblasina.androidbackup.utils.NotFoundError
import dev.danielblasina.androidbackup.utils.withRetry
import okhttp3.Response
import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest
import java.util.Base64
import java.util.logging.Logger

const val CHUNK_SIZE = 1024 * 10
const val RETRIES = 3
class FileUpload(val file: File) {
    val logger: Logger = Logger.getLogger(this.javaClass.name)
    val fileUploadService = FileUploadService()

    fun upload(): ByteArray {
        logger.info { "Start upload of file ${file.path}" }
        val chunks = ArrayList<ByteArray>()
        val fileDigest = MessageDigest.getInstance("SHA-256")
        FileInputStream(file).use { fis ->
            var chunk: ByteArray
            while (run {
                    chunk = fis.readNBytes(CHUNK_SIZE)
                    chunk
                }.isNotEmpty()
            ) {
                withRetry({ uploadChunk(chunk) }, numberOfRetry = RETRIES)
                    .onSuccess {
                        chunks.add(chunk)
                        fileDigest.update(chunk)
                    }.getOrThrow()
            }
        }
        val checksum = fileDigest.digest()
        withRetry({
            fileUploadService.fileUpload(file.toPath(), fileDigest.digest(), chunks)
        }, numberOfRetry = RETRIES).getOrThrow()

        return checksum
    }

    private fun uploadChunk(chunk: ByteArray): Result<Response> {
        val chunkDigest = MessageDigest.getInstance("SHA-256").digest(chunk)
        val chunkDigestB64 = Base64.getUrlEncoder().encodeToString(chunkDigest)

        fileUploadService
            .chunkPresent(chunkDigestB64)
            .onSuccess { res ->
                return Result.success(res)
            }.onFailure { e ->
                when (e) {
                    is NotFoundError -> {
                        logger.info { "Chunk $chunkDigestB64 was not found, will upload it" }
                    }

                    else -> {
                        logger.severe(e.toString())
                        return Result.failure(e)
                    }
                }
            }

        return fileUploadService
            .chunkUpload(chunkDigestB64, chunk)
    }
}
