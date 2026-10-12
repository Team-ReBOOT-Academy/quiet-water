package ru.omsu.quietwater.domain.candle

data class Candle(
    val instrument: Instrument,
    val sequenceNumber: Long,
    val open: Double,
    val high: Double,
    val low: Double,
    val close: Double,
    val volume: Double,
    val runId: String
) {
    init {
        require(sequenceNumber >= 0) { "sequenceNumber не может быть отрицательным" }
        require(high >= low) { "high не может быть ниже low" }
    }
}
