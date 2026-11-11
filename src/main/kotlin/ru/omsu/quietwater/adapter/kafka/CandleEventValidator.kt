package ru.omsu.quietwater.adapter.kafka

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.longOrNull
import ru.omsu.quietwater.adapter.config.EnvConfig
import ru.omsu.quietwater.adapter.kafka.dto.EventEnvelopeDto

class CandleEventValidator(
    private val config: EnvConfig
) {

    fun problems(envelope: EventEnvelopeDto): List<String> {
        val missing = mutableListOf<String>()
        if (envelope.eventId.isBlank()) {
            missing.add(FIELD_EVENT_ID)
        }
        if (envelope.eventType != config.events.candleClosed) {
            missing.add(FIELD_EVENT_TYPE)
        }
        if (envelope.schemaVersion != config.events.schemaVersion) {
            missing.add(FIELD_SCHEMA_VERSION)
        }
        if (envelope.occurredAt.isBlank()) {
            missing.add(FIELD_OCCURRED_AT)
        }
        if (envelope.producer.isBlank()) {
            missing.add(FIELD_PRODUCER)
        }
        val payload = envelope.payload as? JsonObject
            ?: runCatching { envelope.payload.jsonObject }.getOrNull()
        if (payload == null) {
            missing.add(FIELD_PAYLOAD)
            return missing
        }
        val instrument = payload.text(FIELD_INSTRUMENT) ?: payload.text(FIELD_TICKER)
        if (instrument == null) {
            missing.add(FIELD_INSTRUMENT)
        }
        if (payload.text(FIELD_RUN_ID) == null) {
            missing.add(FIELD_RUN_ID)
        }
        if (payload.long(FIELD_SEQUENCE) == null) {
            missing.add(FIELD_SEQUENCE)
        }
        for (price in PRICE_FIELDS) {
            if (payload.number(price) == null) {
                missing.add(price)
            }
        }
        return missing
    }

    private fun JsonObject.text(name: String): String? {
        val primitive = this[name] as? JsonPrimitive ?: return null
        return primitive.contentOrNull?.trim()?.takeIf { it.isNotEmpty() }
    }

    private fun JsonObject.number(name: String): Double? {
        return (this[name] as? JsonPrimitive)?.doubleOrNull
    }

    private fun JsonObject.long(name: String): Long? {
        return (this[name] as? JsonPrimitive)?.longOrNull
    }

    companion object {
        const val FIELD_EVENT_ID: String = "eventId"
        const val FIELD_EVENT_TYPE: String = "eventType"
        const val FIELD_SCHEMA_VERSION: String = "schemaVersion"
        const val FIELD_OCCURRED_AT: String = "occurredAt"
        const val FIELD_PRODUCER: String = "producer"
        const val FIELD_PAYLOAD: String = "payload"
        const val FIELD_INSTRUMENT: String = "instrument"
        const val FIELD_TICKER: String = "ticker"
        const val FIELD_RUN_ID: String = "runId"
        const val FIELD_SEQUENCE: String = "sequenceNumber"
        val PRICE_FIELDS: List<String> = listOf("open", "high", "low", "close")
    }
}
