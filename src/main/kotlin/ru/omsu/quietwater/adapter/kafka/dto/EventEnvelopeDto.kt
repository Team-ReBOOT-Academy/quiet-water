package ru.omsu.quietwater.adapter.kafka.dto

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class EventEnvelopeDto(
    val eventId: String,
    val eventType: String,
    val schemaVersion: String,
    val occurredAt: String,
    val correlationId: String? = null,
    val causationId: String? = null,
    val producer: String,
    val payload: JsonElement
)
