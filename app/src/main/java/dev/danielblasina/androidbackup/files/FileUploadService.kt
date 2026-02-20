package dev.danielblasina.androidbackup.files

import android.net.Uri
import dev.danielblasina.androidbackup.utils.JsonParser
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.net.URI
import java.nio.file.Path

const val MEDIA_TYPE_JSON = "application/json"

class FileUploadService {
    data class UploadFile(val name: Path, val chunks: ArrayList<ByteArray>, val checksum: ByteArray){}
    val client = OkHttpClient()
    val uri = URI("https://www.google.it/")
    fun chunkUpload(filename: String, chunk: ByteArray): Response {
        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("file", filename, chunk.toRequestBody())
            .build()
        Uri.Builder()
        val request: Request = Request.Builder()
            .url(uri.resolve("chunk").toURL())
            .post(requestBody)
            .build()

        return client.newCall(request).execute()
    }

    fun chunkPresent(filename: String): Boolean {
        val request: Request = Request.Builder()
            .url(uri.resolve("chunk/").resolve(filename).toURL())
            .build()
        val result = client.newCall(request).execute()

        return result.isSuccessful && result.code == 200
    }

    fun fileUpload(filename: Path, checksum: ByteArray, chunks: ArrayList<ByteArray>): Response {
        val json = JsonParser.objectToJsonString(UploadFile(filename, chunks, checksum))
        val request: Request = Request.Builder()
            .url(uri.resolve("file").toURL())
            .post(json.toRequestBody(MEDIA_TYPE_JSON.toMediaType()))
            .build()
        return client.newCall(request).execute()
    }
}