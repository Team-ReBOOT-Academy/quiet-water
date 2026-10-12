package ru.omsu.quietwater.domain

import ru.omsu.quietwater.domain.score.SuccessRate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SuccessRateTest {

    @Test
    fun incompleteWindowIsPenalizedAgainstW() {
        val rate = SuccessRate.from(outcomes = listOf(true, true), window = 4)
        assertEquals(0.5, rate.value)
        assertFalse(rate.passes(0.55))
    }

    @Test
    fun fullWindowUsesLastWOutcomes() {
        val rate = SuccessRate.from(
            outcomes = listOf(false, true, true, true, true),
            window = 4
        )
        assertEquals(1.0, rate.value)
        assertTrue(rate.passes(0.55))
    }
}
