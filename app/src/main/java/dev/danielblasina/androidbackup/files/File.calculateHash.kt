package dev.danielblasina.androidbackup.files

import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest

fun File.calculateHash(): ByteArray {
    FileInputStream(this).use { file ->
        val fileDigest = MessageDigest.getInstance("SHA-256")
        var chunk: ByteArray
        while (run {
                chunk = file.readNBytes(CHUNK_SIZE)
                chunk
            }.isNotEmpty()
        ) {
            fileDigest.update(chunk)
        }
        return fileDigest.digest()
    }
}
