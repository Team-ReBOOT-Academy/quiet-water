package ru.omsu.quietwater.adapter.health

import ru.omsu.quietwater.application.port.outbound.ComponentStatus
import ru.omsu.quietwater.application.port.outbound.ProbeResult
import java.util.concurrent.atomic.AtomicReference

class KafkaLink {

    private val snapshot = AtomicReference(
        ProbeResult(status = ComponentStatus.DOWN, details = "")
    )

    fun markNotConfigured(details: String) {
        snapshot.set(ProbeResult(ComponentStatus.DOWN, details))
    }

    fun markUnavailable(details: String) {
        snapshot.set(ProbeResult(ComponentStatus.DOWN, details))
    }

    fun markUp(details: String) {
        snapshot.set(ProbeResult(ComponentStatus.UP, details))
    }

    fun inspect(): ProbeResult = snapshot.get()
}
