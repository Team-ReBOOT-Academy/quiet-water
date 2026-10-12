package ru.omsu.quietwater.application

import ru.omsu.quietwater.application.health.GetHealthStatus
import ru.omsu.quietwater.application.port.outbound.ComponentStatus
import ru.omsu.quietwater.application.port.outbound.HealthProbe
import ru.omsu.quietwater.application.port.outbound.OverallStatus
import ru.omsu.quietwater.application.port.outbound.ProbeResult
import kotlin.test.Test
import kotlin.test.assertEquals

class GetHealthStatusTest {

    @Test
    fun downComponentMakesProcessDegraded() {
        val useCase = GetHealthStatus(
            serviceName = "quiet-water",
            serviceVersion = "1.0.0",
            probes = listOf(
                object : HealthProbe {
                    override val componentName: String = "kafka"
                    override fun inspect(): ProbeResult {
                        return ProbeResult(ComponentStatus.DOWN, "broker is not configured")
                    }
                }
            )
        )

        val snapshot = useCase.execute()

        assertEquals(OverallStatus.DEGRADED, snapshot.status)
        assertEquals("quiet-water", snapshot.service)
        assertEquals(ComponentStatus.DOWN, snapshot.components.getValue("kafka").status)
    }
}
