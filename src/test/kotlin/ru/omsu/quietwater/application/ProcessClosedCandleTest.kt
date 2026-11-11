package ru.omsu.quietwater.application

import ru.omsu.quietwater.adapter.persistence.InMemoryInstrumentWindowStore
import ru.omsu.quietwater.application.candle.BaselineHold
import ru.omsu.quietwater.application.candle.ClosedCandleCommand
import ru.omsu.quietwater.application.candle.ProcessClosedCandle
import ru.omsu.quietwater.application.port.outbound.RecommendationPublisher
import ru.omsu.quietwater.domain.candle.Candle
import ru.omsu.quietwater.domain.candle.Instrument
import ru.omsu.quietwater.domain.recommendation.Recommendation
import ru.omsu.quietwater.domain.signal.Signal
import kotlin.test.Test
import kotlin.test.assertEquals

class ProcessClosedCandleTest {

    @Test
    fun acceptedCandleBecomesOneHoldRecommendation() {
        val published = mutableListOf<Recommendation>()
        var ticks = 0L
        val useCase = ProcessClosedCandle(
            store = InMemoryInstrumentWindowStore(maxCandlesPerInstrument = 32),
            baseline = BaselineHold(
                signal = Signal.HOLD,
                confidence = 0.0,
                suggestedQuantity = 0,
                dataQuality = "OK",
                reason = "baseline"
            ),
            publisher = object : RecommendationPublisher {
                override fun publish(recommendation: Recommendation) {
                    published += recommendation
                }
            },
            nanoTime = {
                ticks += 5_000_000L
                ticks
            }
        )

        val recommendation = useCase.handle(
            ClosedCandleCommand(
                sourceEventId = "evt-1",
                correlationId = "run-1",
                candle = Candle(
                    instrument = Instrument("MOEX"),
                    sequenceNumber = 0,
                    open = 10.0,
                    high = 10.0,
                    low = 10.0,
                    close = 10.0,
                    volume = 1.0,
                    runId = "run-1"
                )
            )
        )

        assertEquals(1, published.size)
        assertEquals("evt-1", recommendation.sourceEventId)
        assertEquals("run-1", recommendation.correlationId)
        assertEquals("run-1", recommendation.runId)
        assertEquals(0L, recommendation.sequenceNumber)
        assertEquals(Signal.HOLD, recommendation.signal)
        assertEquals(0.0, recommendation.confidence)
        assertEquals(0, recommendation.suggestedQuantity)
        assertEquals("OK", recommendation.dataQuality)
        assertEquals("baseline", recommendation.reason)
        assertEquals(5L, recommendation.processingTimeMs)
    }
}
