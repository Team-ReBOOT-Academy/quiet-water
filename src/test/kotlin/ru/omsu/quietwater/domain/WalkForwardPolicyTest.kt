package ru.omsu.quietwater.domain

import ru.omsu.quietwater.domain.candle.Candle
import ru.omsu.quietwater.domain.candle.Instrument
import ru.omsu.quietwater.domain.signal.Signal
import ru.omsu.quietwater.domain.walkforward.Phase
import ru.omsu.quietwater.domain.walkforward.WalkForwardParameters
import ru.omsu.quietwater.domain.walkforward.WalkForwardPolicy
import kotlin.test.Test
import kotlin.test.assertEquals

class WalkForwardPolicyTest {

    private val instrument = Instrument("GAZP")
    private val policy = WalkForwardPolicy(
        WalkForwardParameters(
            learnSize = 4,
            examSize = 3,
            horizon = 1,
            scoreWindow = 4,
            passThreshold = 0.55,
            rsiPeriod = 3,
            rsiOversold = 30,
            rsiOverbought = 70
        )
    )

    @Test
    fun learnPhaseAlwaysHolds() {
        val decision = policy.decide(candles(count = 3, startPrice = 10.0))
        assertEquals(Phase.LEARN, decision.phase)
        assertEquals(Signal.HOLD, decision.signal)
    }

    @Test
    fun examPhaseAlwaysHolds() {
        val decision = policy.decide(candles(count = 6, startPrice = 10.0))
        assertEquals(Phase.EXAM, decision.phase)
        assertEquals(Signal.HOLD, decision.signal)
    }

    private fun candles(count: Int, startPrice: Double): List<Candle> {
        return (0 until count).map { index ->
            val close = startPrice - index
            Candle(
                instrument = instrument,
                sequenceNumber = index.toLong(),
                open = close,
                high = close,
                low = close,
                close = close,
                volume = 1.0,
                runId = "run"
            )
        }
    }
}
