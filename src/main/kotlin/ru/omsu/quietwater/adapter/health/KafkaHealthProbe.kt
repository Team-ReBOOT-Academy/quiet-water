package ru.omsu.quietwater.adapter.health

import ru.omsu.quietwater.adapter.config.EnvConfig
import ru.omsu.quietwater.application.port.outbound.ComponentStatus
import ru.omsu.quietwater.application.port.outbound.HealthProbe
import ru.omsu.quietwater.application.port.outbound.ProbeResult

class KafkaHealthProbe(
    private val config: EnvConfig
) : HealthProbe {

    override val componentName: String = COMPONENT_NAME

    override fun inspect(): ProbeResult {
        if (!config.isKafkaConfigured()) {
            return ProbeResult(
                status = ComponentStatus.DOWN,
                details = config.kafkaUnconfiguredDetails
            )
        }
        return ProbeResult(
            status = ComponentStatus.UP,
            details = config.kafkaConfiguredDetails
        )
    }

    companion object {
        const val COMPONENT_NAME: String = "kafka"
    }
}
