package ru.omsu.quietwater.domain.walkforward

import ru.omsu.quietwater.domain.candle.Candle
import ru.omsu.quietwater.domain.score.SuccessRate
import ru.omsu.quietwater.domain.setup.RsiReversalSetup
import ru.omsu.quietwater.domain.signal.Signal

data class WalkForwardParameters(
    val learnSize: Int,
    val examSize: Int,
    val horizon: Int,
    val scoreWindow: Int,
    val passThreshold: Double,
    val rsiPeriod: Int,
    val rsiOversold: Int,
    val rsiOverbought: Int
) {
    init {
        require(learnSize > 0 && examSize > 0 && horizon > 0 && scoreWindow > 0) {
            "Окна N/K/M/W должны быть положительными"
        }
        require(passThreshold in 0.0..1.0) { "Порог T должен быть в [0, 1]" }
    }
}

data class WalkForwardDecision(
    val phase: Phase,
    val signal: Signal,
    val successRate: SuccessRate,
    val examPassed: Boolean,
    val dataQuality: String,
    val reason: String
)

class WalkForwardPolicy(
    private val parameters: WalkForwardParameters,
    private val setup: RsiReversalSetup = RsiReversalSetup(
        period = parameters.rsiPeriod,
        oversold = parameters.rsiOversold,
        overbought = parameters.rsiOverbought
    )
) {

    private val examPassedByInstrument = mutableMapOf<String, Boolean>()
    private val haltRemainingByInstrument = mutableMapOf<String, Int>()

    @Suppress("KotlinConstantConditions")
    fun decide(window: List<Candle>): WalkForwardDecision {
        if (window.isEmpty()) {
            return WalkForwardDecision(
                phase = Phase.LEARN,
                signal = Signal.HOLD,
                successRate = SuccessRate(0.0),
                examPassed = false,
                dataQuality = QUALITY_INSUFFICIENT,
                reason = "окно инструмента пусто"
            )
        }
        val instrumentKey = window.last().instrument.key
        val dataQuality = if (isMonotonic(window)) QUALITY_OK else QUALITY_GAP
        val outcomes = resolvedOutcomes(window)
        val directionalRate = { side: Signal ->
            SuccessRate.from(
                outcomes = outcomes.filter { it.side == side }.map { it.success },
                window = parameters.scoreWindow
            )
        }
        val count = window.size
        val haltLeft = haltRemainingByInstrument[instrumentKey] ?: 0
        val examPassed = examPassedByInstrument[instrumentKey] ?: evaluateExam(window)
        examPassedByInstrument[instrumentKey] = examPassed

        val phase = when {
            haltLeft > 0 -> Phase.HALT
            count <= parameters.learnSize -> Phase.LEARN
            count <= parameters.learnSize + parameters.examSize -> Phase.EXAM
            examPassed -> Phase.TRADE
            else -> Phase.HALT
        }

        val setupSide = setup.detect(window)
        val rateForSetup = if (setupSide == Signal.HOLD) {
            SuccessRate(0.0)
        } else {
            directionalRate(setupSide)
        }

        val canTrade = phase == Phase.TRADE &&
            setupSide != Signal.HOLD &&
            rateForSetup.passes(parameters.passThreshold) &&
            examPassed &&
            dataQuality == QUALITY_OK

        if (phase == Phase.TRADE && setupSide != Signal.HOLD && !rateForSetup.passes(parameters.passThreshold)) {
            haltRemainingByInstrument[instrumentKey] = parameters.examSize
        } else if (haltLeft > 0) {
            haltRemainingByInstrument[instrumentKey] = haltLeft - 1
        }

        val signal = if (canTrade) setupSide else Signal.HOLD
        val reason = buildReason(phase, setupSide, rateForSetup, examPassed, canTrade)
        return WalkForwardDecision(
            phase = phase,
            signal = signal,
            successRate = rateForSetup,
            examPassed = examPassed,
            dataQuality = dataQuality,
            reason = reason
        )
    }

    private fun evaluateExam(window: List<Candle>): Boolean {
        val examStart = parameters.learnSize
        val examEndExclusive = parameters.learnSize + parameters.examSize
        if (window.size < examEndExclusive) {
            return false
        }
        val examOutcomes = resolvedOutcomes(window).filter { outcome ->
            outcome.index in examStart until (examEndExclusive - parameters.horizon)
        }
        if (examOutcomes.isEmpty()) {
            return false
        }
        val hitRate = examOutcomes.count { it.success }.toDouble() / examOutcomes.size.toDouble()
        return hitRate >= parameters.passThreshold
    }

    @Suppress("KotlinConstantConditions")
    private fun resolvedOutcomes(window: List<Candle>): List<SetupOutcome> {
        val result = mutableListOf<SetupOutcome>()
        for (index in window.indices) {
            val prefix = window.subList(0, index + 1)
            val side = setup.detect(prefix)
            if (side == Signal.HOLD) {
                continue
            }
            val resolveAt = index + parameters.horizon
            if (resolveAt >= window.size) {
                continue
            }
            val entry = window[index].close
            val future = window[resolveAt].close
            val success = when (side) {
                Signal.BUY -> future > entry
                Signal.SELL -> future < entry
                Signal.HOLD -> false
            }
            result += SetupOutcome(index = index, side = side, success = success)
        }
        return result
    }

    private fun isMonotonic(window: List<Candle>): Boolean {
        return window.zipWithNext().all { (previous, current) ->
            current.sequenceNumber == previous.sequenceNumber + 1
        }
    }

    private fun buildReason(
        phase: Phase,
        setupSide: Signal,
        rate: SuccessRate,
        examPassed: Boolean,
        canTrade: Boolean
    ): String {
        return when {
            canTrade -> "фаза $phase, сетап $setupSide, score=${rate.value}"
            phase != Phase.TRADE -> "фаза $phase, наружу только HOLD"
            setupSide == Signal.HOLD -> "нет сетапа RSI"
            !examPassed -> "экзамен не сдан"
            else -> "score=${rate.value} ниже порога ${parameters.passThreshold}"
        }
    }

    private data class SetupOutcome(
        val index: Int,
        val side: Signal,
        val success: Boolean
    )

    companion object {
        const val QUALITY_OK: String = "OK"
        const val QUALITY_GAP: String = "SEQUENCE_GAP"
        const val QUALITY_INSUFFICIENT: String = "INSUFFICIENT"
    }
}
