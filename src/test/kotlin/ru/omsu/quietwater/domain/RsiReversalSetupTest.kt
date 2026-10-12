package ru.omsu.quietwater.domain

import ru.omsu.quietwater.domain.candle.Candle
import ru.omsu.quietwater.domain.candle.Instrument
import ru.omsu.quietwater.domain.setup.RsiReversalSetup
import ru.omsu.quietwater.domain.signal.Signal
import kotlin.test.Test
import kotlin.test.assertEquals

class RsiReversalSetupTest {

    private val instrument = Instrument("SBER")
    private val setup = RsiReversalSetup(period = 3, oversold = 30, overbought = 70)

    @Test
    fun bounceAfterSelloffProducesBuy() {
        val candles = closes(
            10.0, 9.0, 8.0, 7.0, 6.0, 5.0, 4.0, 4.5
        )
        assertEquals(Signal.BUY, setup.detect(candles))
    }

    @Test
    fun dropAfterRallyProducesSell() {
        val candles = closes(
            4.0, 5.0, 6.0, 7.0, 8.0, 9.0, 10.0, 9.5
        )
        assertEquals(Signal.SELL, setup.detect(candles))
    }

    @Test
    fun tooShortSeriesIsHold() {
        assertEquals(Signal.HOLD, setup.detect(closes(1.0, 1.1)))
    }

    private fun closes(vararg values: Double): List<Candle> {
        return values.mapIndexed { index, close ->
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
