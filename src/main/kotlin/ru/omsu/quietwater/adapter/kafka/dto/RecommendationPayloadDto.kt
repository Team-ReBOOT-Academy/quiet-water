package ru.omsu.quietwater.adapter.kafka.dto

import kotlinx.serialization.Serializable

@Serializable
data class RecommendationPayloadDto(
    val recommendationId: String,
    val sourceEventId: String,
    val runId: String,
    val secretKey: String,
    val strategyId: String,
    val accountId: String,
    val groupId: String,
    val sessionId: String,
    val instrument: String,
    val sequenceNumber: Long,
    val signal: String,
    val confidence: Double,
    val suggestedQuantity: Int,
    val dataQuality: String
)
