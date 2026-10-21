package ru.omsu.quietwater.application.candle

import ru.omsu.quietwater.application.port.outbound.InstrumentWindowStore
import ru.omsu.quietwater.application.port.outbound.RecommendationPublisher
import ru.omsu.quietwater.domain.recommendation.Recommendation
import ru.omsu.quietwater.domain.signal.Signal
import ru.omsu.quietwater.domain.walkforward.WalkForwardPolicy

class ProcessClosedCandle(
    private val store: InstrumentWindowStore,
    private val policy: WalkForwardPolicy,
    private val publisher: RecommendationPublisher
) {

    fun handle(command: ClosedCandleCommand): Recommendation {
        val candle = command.candle
        store.append(candle)
        val decision = policy.decide(store.window(candle.instrument))
        val recommendation = Recommendation(
            sourceEventId = command.sourceEventId,
            instrument = candle.instrument,
            runId = candle.runId,
            sequenceNumber = candle.sequenceNumber,
            signal = decision.signal,
            confidence = if (decision.signal == Signal.HOLD) 0.0 else decision.successRate.value,
            suggestedQuantity = if (decision.signal == Signal.HOLD) 0 else 1,
            dataQuality = decision.dataQuality,
            reason = decision.reason,
            phase = decision.phase
        )
        publisher.publish(recommendation)
        return recommendation
    }
}
