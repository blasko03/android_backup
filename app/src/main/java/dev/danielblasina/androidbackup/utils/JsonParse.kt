package dev.danielblasina.androidbackup.utils

import com.fasterxml.jackson.databind.ObjectMapper


internal object JsonParser {
    fun <T>parse(requestBody: ByteArray, classType: Class<T>): T {
        val objectMapper = ObjectMapper()
        return objectMapper.readValue(requestBody, classType)
    }

    fun <T>objectToJsonString(obj: T): String {
        val objectMapper = ObjectMapper()
        return objectMapper.writeValueAsString(obj)
    }
}