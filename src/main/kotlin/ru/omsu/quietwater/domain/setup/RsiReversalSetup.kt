package ru.omsu.quietwater.domain.setup

import ru.omsu.quietwater.domain.candle.Candle
import ru.omsu.quietwater.domain.signal.Signal

class RsiReversalSetup(
    private val period: Int,
    private val oversold: Int,
    private val overbought: Int
) {

    init {
        require(period > 1) { "Период RSI должен быть больше 1" }
        require(oversold in 1 until overbought) { "Зоны RSI заданы некорректно" }
        require(overbought < 100) { "Перекупленность должна быть ниже 100" }
    }

    fun detect(candles: List<Candle>): Signal {
        val closes = candles.map { it.close }
        if (closes.size < period + 2) {
            return Signal.HOLD
        }
        val currentRsi = wilderRsi(closes, period) ?: return Signal.HOLD
        val previousRsi = wilderRsi(closes.dropLast(1), period) ?: return Signal.HOLD
        val currentClose = closes.last()
        val previousClose = closes[closes.lastIndex - 1]
        return when {
            previousRsi < oversold && currentClose > previousClose -> Signal.BUY
            previousRsi > overbought && currentClose < previousClose -> Signal.SELL
            else -> Signal.HOLD
        }
    }

    private fun wilderRsi(closes: List<Double>, period: Int): Double? {
        if (closes.size < period + 1) {
            return null
        }
        val changes = closes.zipWithNext { previous, current -> current - previous }
        val window = changes.takeLast(period)
        val avgGain = window.map { change -> if (change > 0) change else 0.0 }.average()
        val avgLoss = window.map { change -> if (change < 0) -change else 0.0 }.average()
        if (avgLoss == 0.0) {
            return if (avgGain == 0.0) 50.0 else 100.0
        }
        val relativeStrength = avgGain / avgLoss
        return 100.0 - (100.0 / (1.0 + relativeStrength))
    }
}
