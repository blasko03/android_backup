package dev.danielblasina.androidbackup.utils

import okhttp3.Response

class NotFoundError(response: Response) : Throwable()
