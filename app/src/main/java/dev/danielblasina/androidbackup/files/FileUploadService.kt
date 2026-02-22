package dev.danielblasina.androidbackup.files

import android.net.Uri
import dev.danielblasina.androidbackup.utils.FailedRequestError
import dev.danielblasina.androidbackup.utils.JsonParse
import dev.danielblasina.androidbackup.utils.executeWithRescue
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.net.HttpURLConnection
import java.net.URI
import java.nio.file.Path

const val MEDIA_TYPE_JSON = "application/json"

class FileUploadService {
    data class UploadFile(val name: Path, val chunks: ArrayList<ByteArray>, val checksum: ByteArray)

    val client = OkHttpClient()
    val uri = URI("http:/192.168.1.126:8080/")

    fun chunkUpload(filename: String, chunk: ByteArray): Result<Response> {
        val requestBody =
            MultipartBody
                .Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("file", filename, chunk.toRequestBody())
                .build()
        Uri.Builder()
        val request: Request =
            Request
                .Builder()
                .url(uri.resolve("chunk").toURL())
                .post(requestBody)
                .build()
        return client.newCall(request).executeWithRescue()
    }

    fun chunkPresent(filename: String): Result<Response> {
        val request: Request =
            Request
                .Builder()
                .url(uri.resolve("chunk/").resolve(filename).toURL())
                .build()
        val res = client.newCall(request).executeWithRescue()

        return res.onSuccess { response ->
            when (response.code) {
                HttpURLConnection.HTTP_OK -> Result.success(response)
                else -> Result.failure(FailedRequestError(response))
            }
        }
    }

    fun fileUpload(
        filename: Path,
        checksum: ByteArray,
        chunks: ArrayList<ByteArray>,
    ): Result<Response> {
        val json = JsonParse.objectToJsonString(UploadFile(filename, chunks, checksum))
        val request: Request =
            Request
                .Builder()
                .url(uri.resolve("file").toURL())
                .post(json.toRequestBody(MEDIA_TYPE_JSON.toMediaType()))
                .build()

        return client.newCall(request).executeWithRescue()
    }
}
