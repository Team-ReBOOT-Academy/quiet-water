package ru.omsu.quietwater.adapter.health

import ru.omsu.quietwater.application.port.outbound.ComponentStatus
import ru.omsu.quietwater.application.port.outbound.HealthProbe
import ru.omsu.quietwater.application.port.outbound.ProbeResult

class CompositeHealthProbe(
    private val probes: List<HealthProbe>
) : HealthProbe {

    override val componentName: String = COMPONENT_NAME

    override fun inspect(): ProbeResult {
        val results = probes.map { it.inspect() }
        val allUp = results.all { it.status == ComponentStatus.UP }
        return ProbeResult(
            status = if (allUp) ComponentStatus.UP else ComponentStatus.DOWN,
            details = results.joinToString(separator = "; ") { result -> result.details }
        )
    }

    companion object {
        const val COMPONENT_NAME: String = "composite"
    }
}
