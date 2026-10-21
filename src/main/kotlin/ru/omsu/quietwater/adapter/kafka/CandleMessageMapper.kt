package ru.omsu.quietwater.adapter.kafka

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.longOrNull
import ru.omsu.quietwater.adapter.kafka.dto.EventEnvelopeDto
import ru.omsu.quietwater.application.candle.ClosedCandleCommand
import ru.omsu.quietwater.domain.candle.Candle
import ru.omsu.quietwater.domain.candle.Instrument

class CandleMessageMapper {

    fun map(envelope: EventEnvelopeDto): ClosedCandleCommand? {
        val payload = envelope.payload as? JsonObject 
            ?: runCatching { envelope.payload.jsonObject }.getOrNull()
            ?: return null
        if (envelope.eventId.isBlank()) {
            return null
        }
        val instrument = payload.text(FIELD_INSTRUMENT) ?: payload.text(FIELD_TICKER) ?: return null
        val runId = payload.text(FIELD_RUN_ID) ?: return null
        val sequence = payload.long(FIELD_SEQUENCE) ?: return null
        val open = payload.number(FIELD_OPEN) ?: return null
        val high = payload.number(FIELD_HIGH) ?: return null
        val low = payload.number(FIELD_LOW) ?: return null
        val close = payload.number(FIELD_CLOSE) ?: return null
        val volume = payload.number(FIELD_VOLUME) ?: 0.0
        return ClosedCandleCommand(
            sourceEventId = envelope.eventId,
            candle = Candle(
                instrument = Instrument(instrument),
                sequenceNumber = sequence,
                open = open,
                high = high,
                low = low,
                close = close,
                volume = volume,
                runId = runId
            )
        )
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
        const val FIELD_INSTRUMENT: String = "instrument"
        const val FIELD_TICKER: String = "ticker"
        const val FIELD_RUN_ID: String = "runId"
        const val FIELD_SEQUENCE: String = "sequenceNumber"
        const val FIELD_OPEN: String = "open"
        const val FIELD_HIGH: String = "high"
        const val FIELD_LOW: String = "low"
        const val FIELD_CLOSE: String = "close"
        const val FIELD_VOLUME: String = "volume"
    }
}
