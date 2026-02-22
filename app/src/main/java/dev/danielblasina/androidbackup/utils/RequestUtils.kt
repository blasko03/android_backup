package dev.danielblasina.androidbackup.utils

import okhttp3.Call
import okhttp3.Response
import okio.IOException
import java.net.HttpURLConnection
import java.util.logging.Logger

val RETRYABLE_ERRORS = listOf(408, 429, 500, 502, 503, 504)

fun Call.executeWithRescue(successCodes: Array<Int> = arrayOf()): Result<Response> {
    try {
        execute().use { res ->
            if (successCodes.isNotEmpty() && res.code in successCodes) {
                return Result.success(res)
            }
            if (res.isSuccessful) {
                return Result.success(res)
            }
            if (res.code == HttpURLConnection.HTTP_NOT_FOUND) {
                return Result.failure(NotFoundError(res))
            }
            return Result.failure(FailedRequestError(res))
        }
    } catch (e: IOException) {
        return Result.failure(e)
    }
}

fun withRetry(operation: () -> Result<Response>, numberOfRetry: Int): Result<Response> {
    val logger: Logger = Logger.getLogger("withRetry")
    var result: Result<Response> = Result.failure(Exception())
    for (i in (1..numberOfRetry)) {
        result = operation()
            .onSuccess { res ->
                return Result.success(res)
            }.onFailure { e ->
                when (isRetryableError(e)) {
                    false -> {
                        return Result.failure(e)
                    }

                    true -> {
                        logger.warning { "Failed http request after $i attempts" }
                        Thread.sleep(1000)
                    }
                }
            }
    }
    logger.warning { "Failed http request will after $numberOfRetry attempts" }
    return result
}

fun isRetryableError(error: Throwable): Boolean {
    if (error is FailedRequestError && RETRYABLE_ERRORS.contains(error.response.code)) {
        return true
    }
    if (error is java.io.IOException) {
        return true
    }
    return false
}
