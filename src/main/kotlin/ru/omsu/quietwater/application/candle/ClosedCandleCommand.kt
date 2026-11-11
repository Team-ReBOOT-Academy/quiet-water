package ru.omsu.quietwater.application.candle

import ru.omsu.quietwater.domain.candle.Candle

data class ClosedCandleCommand(
    val sourceEventId: String,
    val correlationId: String?,
    val candle: Candle
)
