package dev.danielblasina.androidbackup.files


import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest
import java.util.Base64
import java.util.logging.Logger


const val CHUNK_SIZE = 1024 * 10
class FileManager(val file: File) {
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
                }.isNotEmpty()) {
                chunks.add(uploadChunk(chunk))
                fileDigest.update(chunk)
            }
        }
        val checksum = fileDigest.digest()
        fileUploadService.fileUpload(file.toPath(), fileDigest.digest(), chunks)
        return checksum
    }

    fun uploadChunk(chunk: ByteArray): ByteArray {
        val chunkDigest = MessageDigest.getInstance("SHA-256").digest(chunk)
        val chunkDigestHex = Base64.getUrlEncoder().encodeToString(chunkDigest)

        if (fileUploadService.chunkPresent(chunkDigestHex))
            return chunkDigest
        fileUploadService.chunkUpload(chunkDigestHex, chunk).use { response ->
            if (!response.isSuccessful) logger.severe("Failed http request")
            logger.info(chunkDigestHex)
        }
        return chunkDigest
    }
}