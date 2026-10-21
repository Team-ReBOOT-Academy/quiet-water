package ru.omsu.quietwater.adapter.kafka

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement
import ru.omsu.quietwater.adapter.kafka.dto.EventEnvelopeDto

class PlatformEventCodec(
    private val json: Json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        explicitNulls = false
    }
) {

    fun encode(envelope: EventEnvelopeDto): String = json.encodeToString(EventEnvelopeDto.serializer(), envelope)

    fun decode(raw: String): EventEnvelopeDto = json.decodeFromString(EventEnvelopeDto.serializer(), raw)

    fun <T> payload(envelope: EventEnvelopeDto, deserializer: DeserializationStrategy<T>): T {
        return json.decodeFromJsonElement(deserializer, envelope.payload)
    }

    fun <T> envelope(
        eventType: String,
        producer: String,
        schemaVersion: String,
        payload: T,
        serializer: SerializationStrategy<T>,
        eventId: String,
        occurredAt: String,
        correlationId: String? = null,
        causationId: String? = null
    ): EventEnvelopeDto {
        val body: JsonElement = json.encodeToJsonElement(serializer, payload)
        return EventEnvelopeDto(
            eventId = eventId,
            eventType = eventType,
            schemaVersion = schemaVersion,
            occurredAt = occurredAt,
            correlationId = correlationId,
            causationId = causationId,
            producer = producer,
            payload = body
        )
    }
}
