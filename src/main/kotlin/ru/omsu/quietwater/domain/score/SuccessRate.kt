package ru.omsu.quietwater.domain.score

data class SuccessRate(val value: Double) {

    init {
        require(value in 0.0..1.0) { "successRate должен быть в диапазоне [0, 1]" }
    }

    fun passes(threshold: Double): Boolean = value >= threshold

    companion object {
        fun from(outcomes: List<Boolean>, window: Int): SuccessRate {
            require(window > 0) { "Окно W должно быть положительным" }
            val recent = outcomes.takeLast(window)
            val successes = recent.count { it }
            return SuccessRate(successes.toDouble() / window.toDouble())
        }
    }
}
