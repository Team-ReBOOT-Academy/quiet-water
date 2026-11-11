package ru.omsu.quietwater.domain.recommendation

import ru.omsu.quietwater.domain.candle.Instrument
import ru.omsu.quietwater.domain.signal.Signal

data class Recommendation(
    val sourceEventId: String,
    val correlationId: String,
    val instrument: Instrument,
    val runId: String,
    val sequenceNumber: Long,
    val signal: Signal,
    val confidence: Double,
    val suggestedQuantity: Int,
    val dataQuality: String,
    val reason: String,
    val processingTimeMs: Long
)
