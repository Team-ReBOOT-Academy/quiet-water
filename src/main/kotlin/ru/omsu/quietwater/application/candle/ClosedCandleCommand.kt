package ru.omsu.quietwater.application.candle

import ru.omsu.quietwater.domain.candle.Candle

data class ClosedCandleCommand(
    val sourceEventId: String,
    val candle: Candle
)
