package dev.danielblasina.androidbackup.utils

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import java.io.InputStream

object JsonParse {
    inline fun <reified T> parse(requestBody: InputStream): T {
        val objectMapper = getObjectMapper()
        return objectMapper.readValue(requestBody, object : TypeReference<T>() {})
    }
    fun <T> objectToJsonString(obj: T): String {
        val objectMapper = getObjectMapper()
        return objectMapper.writeValueAsString(obj)
    }

    fun getObjectMapper(): ObjectMapper = ObjectMapper().registerModule(KotlinModule.Builder().build())
}
