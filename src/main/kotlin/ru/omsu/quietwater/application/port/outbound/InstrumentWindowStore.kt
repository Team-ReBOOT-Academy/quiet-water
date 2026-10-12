package ru.omsu.quietwater.application.port.outbound

import ru.omsu.quietwater.domain.candle.Candle
import ru.omsu.quietwater.domain.candle.Instrument

interface InstrumentWindowStore {
    fun append(candle: Candle)
    fun window(instrument: Instrument): List<Candle>
}
