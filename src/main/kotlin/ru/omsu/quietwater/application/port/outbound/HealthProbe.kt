package ru.omsu.quietwater.application.port.outbound

interface HealthProbe {
    val componentName: String
    fun inspect(): ProbeResult
}

enum class ComponentStatus {
    UP,
    DOWN
}

enum class OverallStatus {
    UP,
    DEGRADED
}

data class ProbeResult(
    val status: ComponentStatus,
    val details: String
)
