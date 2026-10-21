package ru.omsu.quietwater.adapter.persistence

import ru.omsu.quietwater.domain.candle.Candle
import ru.omsu.quietwater.domain.candle.Instrument
import kotlin.test.Test
import kotlin.test.assertEquals

class InMemoryInstrumentWindowStoreTest {

    @Test
    fun duplicateSequenceDoesNotGrowTheWindow() {
        val store = InMemoryInstrumentWindowStore(maxCandlesPerInstrument = 8)
        val instrument = Instrument("SBER")
        val candle = Candle(
            instrument = instrument,
            sequenceNumber = 3,
            open = 1.0,
            high = 2.0,
            low = 1.0,
            close = 1.5,
            volume = 1.0,
            runId = "run"
        )

        store.append(candle)
        store.append(candle)

        assertEquals(1, store.window(instrument).size)
    }
}
