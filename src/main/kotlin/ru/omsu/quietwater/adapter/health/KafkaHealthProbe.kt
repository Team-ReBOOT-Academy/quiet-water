package ru.omsu.quietwater.adapter.health

import ru.omsu.quietwater.application.port.outbound.HealthProbe
import ru.omsu.quietwater.application.port.outbound.ProbeResult

class KafkaHealthProbe(
    private val link: KafkaLink
) : HealthProbe {

    override val componentName: String = COMPONENT_NAME

    override fun inspect(): ProbeResult = link.inspect()

    companion object {
        const val COMPONENT_NAME: String = "kafka"
    }
}
