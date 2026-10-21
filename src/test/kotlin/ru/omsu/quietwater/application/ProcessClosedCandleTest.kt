package ru.omsu.quietwater.application

import ru.omsu.quietwater.adapter.persistence.InMemoryInstrumentWindowStore
import ru.omsu.quietwater.application.candle.ClosedCandleCommand
import ru.omsu.quietwater.application.candle.ProcessClosedCandle
import ru.omsu.quietwater.application.port.outbound.RecommendationPublisher
import ru.omsu.quietwater.domain.candle.Candle
import ru.omsu.quietwater.domain.candle.Instrument
import ru.omsu.quietwater.domain.recommendation.Recommendation
import ru.omsu.quietwater.domain.signal.Signal
import ru.omsu.quietwater.domain.walkforward.Phase
import ru.omsu.quietwater.domain.walkforward.WalkForwardParameters
import ru.omsu.quietwater.domain.walkforward.WalkForwardPolicy
import kotlin.test.Test
import kotlin.test.assertEquals

class ProcessClosedCandleTest {

    @Test
    fun firstCandlesStayOnHoldAndArePublished() {
        val published = mutableListOf<Recommendation>()
        val useCase = ProcessClosedCandle(
            store = InMemoryInstrumentWindowStore(maxCandlesPerInstrument = 32),
            policy = WalkForwardPolicy(
                WalkForwardParameters(
                    learnSize = 8,
                    examSize = 4,
                    horizon = 2,
                    scoreWindow = 4,
                    passThreshold = 0.55,
                    rsiPeriod = 3,
                    rsiOversold = 30,
                    rsiOverbought = 70
                )
            ),
            publisher = object : RecommendationPublisher {
                override fun publish(recommendation: Recommendation) {
                    published += recommendation
                }
            }
        )

        val recommendation = useCase.handle(
            ClosedCandleCommand(
                sourceEventId = "evt-1",
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

        assertEquals("evt-1", recommendation.sourceEventId)
        assertEquals(Signal.HOLD, recommendation.signal)
        assertEquals(Phase.LEARN, recommendation.phase)
        assertEquals(1, published.size)
    }
}
