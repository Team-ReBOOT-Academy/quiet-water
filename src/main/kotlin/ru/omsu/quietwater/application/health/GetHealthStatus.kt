package ru.omsu.quietwater.application.health

import ru.omsu.quietwater.application.port.outbound.ComponentStatus
import ru.omsu.quietwater.application.port.outbound.HealthProbe
import ru.omsu.quietwater.application.port.outbound.OverallStatus
import ru.omsu.quietwater.application.port.outbound.ProbeResult

/**
 * Сценарий GET /health: собирает зонды и сводит общий статус процесса.
 */
class GetHealthStatus(
    private val serviceName: String,
    private val serviceVersion: String,
    private val probes: List<HealthProbe>
) {

    fun execute(): HealthSnapshot {
        val components = probes.associate { probe -> probe.componentName to probe.inspect() }
        val overall = if (components.values.all { it.status == ComponentStatus.UP }) {
            OverallStatus.UP
        } else {
            OverallStatus.DEGRADED
        }
        return HealthSnapshot(
            status = overall,
            service = serviceName,
            version = serviceVersion,
            components = components
        )
    }
}

data class HealthSnapshot(
    val status: OverallStatus,
    val service: String,
    val version: String,
    val components: Map<String, ProbeResult>
)
