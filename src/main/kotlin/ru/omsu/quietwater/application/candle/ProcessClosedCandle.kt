package ru.omsu.quietwater.application.candle

import ru.omsu.quietwater.application.port.outbound.InstrumentWindowStore
import ru.omsu.quietwater.application.port.outbound.RecommendationPublisher
import ru.omsu.quietwater.domain.recommendation.Recommendation

class ProcessClosedCandle(
    private val store: InstrumentWindowStore,
    private val baseline: BaselineHold,
    private val publisher: RecommendationPublisher,
    private val nanoTime: () -> Long = System::nanoTime
) {

    fun handle(command: ClosedCandleCommand): Recommendation {
        val startedAt = nanoTime()
        store.append(command.candle)
        val processingTimeMs = ((nanoTime() - startedAt).coerceAtLeast(0L)) / NANOS_IN_MILLISECOND
        val recommendation = Recommendation(
            sourceEventId = command.sourceEventId,
            correlationId = command.correlationId?.takeIf { it.isNotBlank() } ?: command.candle.runId,
            instrument = command.candle.instrument,
            runId = command.candle.runId,
            sequenceNumber = command.candle.sequenceNumber,
            signal = baseline.signal,
            confidence = baseline.confidence,
            suggestedQuantity = baseline.suggestedQuantity,
            dataQuality = baseline.dataQuality,
            reason = baseline.reason,
            processingTimeMs = processingTimeMs
        )
        publisher.publish(recommendation)
        return recommendation
    }

    private companion object {
        const val NANOS_IN_MILLISECOND: Long = 1_000_000L
    }
}
