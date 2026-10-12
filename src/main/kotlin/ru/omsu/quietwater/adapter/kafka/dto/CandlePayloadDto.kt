package ru.omsu.quietwater.adapter.kafka.dto

import kotlinx.serialization.Serializable

@Serializable
data class CandlePayloadDto(
    val instrument: String,
    val runId: String,
    val sequenceNumber: Long,
    val open: Double,
    val high: Double,
    val low: Double,
    val close: Double,
    val volume: Double = 0.0
)
