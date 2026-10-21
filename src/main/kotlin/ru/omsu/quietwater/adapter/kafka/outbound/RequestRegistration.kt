package ru.omsu.quietwater.adapter.kafka.outbound

import ru.omsu.quietwater.adapter.config.EnvConfig
import ru.omsu.quietwater.adapter.kafka.PlatformEventCodec
import ru.omsu.quietwater.adapter.kafka.dto.RegistrationRequestPayload
import ru.omsu.quietwater.adapter.logging.StructuredLog
import ru.omsu.quietwater.application.port.outbound.OutboundSender
import ru.omsu.quietwater.application.registration.RegistrationPhase
import ru.omsu.quietwater.application.registration.RegistrationSession
import java.time.Instant
import java.util.UUID

class RequestRegistration(
    private val config: EnvConfig,
    private val session: RegistrationSession,
    private val sender: OutboundSender,
    private val codec: PlatformEventCodec,
    private val log: StructuredLog,
    private val ids: () -> String = { UUID.randomUUID().toString() },
    private val clock: () -> Instant = Instant::now
) {

    fun sendIfNeeded() {
        if (session.phase() != RegistrationPhase.PENDING) {
            return
        }
        val eventId = ids()
        val envelope = codec.envelope(
            eventType = config.events.registrationRequest,
            producer = config.serviceName,
            schemaVersion = config.events.schemaVersion,
            payload = RegistrationRequestPayload(
                name = config.displayName,
                studentId = config.studentId,
                strategyId = config.strategyId,
                serviceName = config.serviceName
            ),
            serializer = RegistrationRequestPayload.serializer(),
            eventId = eventId,
            occurredAt = clock().toString()
        )
        sender.send(config.outboundTopic(), config.studentId, codec.encode(envelope))
        log.info(
            "registration.request",
            mapOf(
                "name" to config.displayName,
                "studentId" to config.studentId,
                "strategyId" to config.strategyId,
                "service" to config.serviceName,
                "eventId" to eventId
            )
        )
    }
}
