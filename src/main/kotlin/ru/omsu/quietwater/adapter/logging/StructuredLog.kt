package ru.omsu.quietwater.adapter.logging

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.slf4j.Logger

class StructuredLog(private val logger: Logger) {

    fun info(event: String, fields: Map<String, String> = emptyMap()) {
        logger.info(render(event, fields))
    }

    fun warn(event: String, fields: Map<String, String> = emptyMap(), error: Throwable? = null) {
        val line = render(event, fields)
        logger.warn(line)
        if (error != null && logger.isDebugEnabled) {
            logger.debug(line, error)
        }
    }

    fun safeReason(error: Throwable, secret: String?): String {
        val raw = error.message ?: error.javaClass.simpleName
        if (secret.isNullOrBlank()) {
            return raw
        }
        return raw.replace(secret, SECRET_MASK)
    }

    private fun render(event: String, fields: Map<String, String>): String {
        val body: JsonObject = buildJsonObject {
            put("event", event)
            put(
                "fields",
                buildJsonObject {
                    fields.forEach { (key, value) -> put(key, value) }
                }
            )
        }
        return body.toString()
    }

    companion object {
        const val SECRET_MASK: String = "***"
    }
}
