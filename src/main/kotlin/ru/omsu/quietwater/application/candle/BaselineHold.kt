package ru.omsu.quietwater.application.candle

import ru.omsu.quietwater.domain.signal.Signal

data class BaselineHold(
    val signal: Signal,
    val confidence: Double,
    val suggestedQuantity: Int,
    val dataQuality: String,
    val reason: String
)
