package dev.danielblasina.androidbackup.files

import dev.danielblasina.androidbackup.utils.NotFoundError
import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest
import java.util.Base64
import java.util.logging.Logger

const val CHUNK_SIZE = 1024 * 10

class FileUpload(
    val file: File,
) {
    val logger: Logger = Logger.getLogger(this.javaClass.name)
    val fileUploadService = FileUploadService()

    fun upload(): ByteArray {
        logger.info { "uploading file ${file.path}" }
        val chunks = ArrayList<ByteArray>()
        val fileDigest = MessageDigest.getInstance("SHA-256")
        FileInputStream(file).use { fis ->
            var chunk: ByteArray
            while (run {
                    chunk = fis.readNBytes(CHUNK_SIZE)
                    chunk
                }.isNotEmpty()
            ) {
                chunks.add(uploadChunk(chunk).getOrThrow())
                fileDigest.update(chunk)
            }
        }
        val checksum = fileDigest.digest()
        fileUploadService.fileUpload(file.toPath(), fileDigest.digest(), chunks)
        return checksum
    }

    private fun uploadChunk(chunk: ByteArray): Result<ByteArray> {
        val chunkDigest = MessageDigest.getInstance("SHA-256").digest(chunk)
        val chunkDigestHex = Base64.getUrlEncoder().encodeToString(chunkDigest)

        fileUploadService
            .chunkPresent(chunkDigestHex)
            .onSuccess {
                return Result.success(chunkDigest)
            }.onFailure { e ->
                when (e) {
                    is NotFoundError -> {
                        logger.info { "chunk not found" }
                    }

                    else -> {
                        logger.severe(e.toString())
                        throw e
                    }
                }
            }

        fileUploadService
            .chunkUpload(chunkDigestHex, chunk)
            .onSuccess {
                logger.info(chunkDigestHex)
                return Result.success(chunkDigest)
            }.onFailure { e ->
                logger.severe("Failed http request")
                return Result.failure(e)
            }

        return Result.failure(Exception("Unknown error"))
    }
}
