package ru.omsu.quietwater.adapter.persistence

import ru.omsu.quietwater.application.port.outbound.InstrumentWindowStore
import ru.omsu.quietwater.domain.candle.Candle
import ru.omsu.quietwater.domain.candle.Instrument

class InMemoryInstrumentWindowStore(
    private val maxCandlesPerInstrument: Int
) : InstrumentWindowStore {

    private val windows = mutableMapOf<String, ArrayDeque<Candle>>()

    override fun append(candle: Candle) {
        val deque = windows.getOrPut(candle.instrument.key) { ArrayDeque() }
        val last = deque.lastOrNull()
        if (last != null && last.runId == candle.runId && last.sequenceNumber == candle.sequenceNumber) {
            return
        }
        deque.addLast(candle)
        while (deque.size > maxCandlesPerInstrument) {
            deque.removeFirst()
        }
    }

    override fun window(instrument: Instrument): List<Candle> {
        return windows[instrument.key]?.toList().orEmpty()
    }
}
