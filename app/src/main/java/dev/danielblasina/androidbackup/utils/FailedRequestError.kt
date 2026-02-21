package dev.danielblasina.androidbackup.utils

import okhttp3.Response

class FailedRequestError(
    response: Response,
) : Throwable()
