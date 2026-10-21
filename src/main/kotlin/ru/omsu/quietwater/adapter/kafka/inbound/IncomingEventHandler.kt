package ru.omsu.quietwater.adapter.kafka.inbound

import ru.omsu.quietwater.adapter.config.EnvConfig
import ru.omsu.quietwater.adapter.kafka.CandleMessageMapper
import ru.omsu.quietwater.adapter.kafka.PlatformEventCodec
import ru.omsu.quietwater.adapter.kafka.ProcessedEventIds
import ru.omsu.quietwater.adapter.kafka.dto.RegistrationResultPayload
import ru.omsu.quietwater.adapter.logging.StructuredLog
import ru.omsu.quietwater.application.candle.ProcessClosedCandle
import ru.omsu.quietwater.application.registration.RegistrationCredentials
import ru.omsu.quietwater.application.registration.RegistrationSession

class UnreadableEvent(message: String) : IllegalArgumentException(message)

class IncomingEventHandler(
    private val config: EnvConfig,
    private val session: RegistrationSession,
    private val processClosedCandle: ProcessClosedCandle,
    private val mapper: CandleMessageMapper,
    private val codec: PlatformEventCodec,
    private val processed: ProcessedEventIds,
    private val log: StructuredLog
) {

    private val pendingCandles = ArrayDeque<ru.omsu.quietwater.application.candle.ClosedCandleCommand>()

    fun handle(raw: String) {
        val envelope = try {
            codec.decode(raw)
        } catch (error: Exception) {
            throw UnreadableEvent(error.message ?: "unreadable event")
        }
        if (envelope.eventId.isBlank()) {
            throw UnreadableEvent("eventId is blank")
        }
        if (processed.alreadySeen(envelope.eventId)) {
            return
        }
        when (envelope.eventType) {
            config.events.registrationAccepted -> onAccepted(envelope.eventId, rawPayload(envelope))
            config.events.registrationRejected -> onRejected(envelope.eventId, rawPayload(envelope))
            config.events.candleClosed -> onCandle(envelope)
            else -> log.info(
                "event.ignored",
                mapOf("eventType" to envelope.eventType, "sourceEventId" to envelope.eventId)
            )
        }
        processed.remember(envelope.eventId)
    }

    private fun rawPayload(envelope: ru.omsu.quietwater.adapter.kafka.dto.EventEnvelopeDto): RegistrationResultPayload {
        return try {
            codec.payload(envelope, RegistrationResultPayload.serializer())
        } catch (error: Exception) {
            throw UnreadableEvent(error.message ?: "unreadable registration payload")
        }
    }

    private fun onAccepted(eventId: String, payload: RegistrationResultPayload) {
        if (payload.accepted == false) {
            onRejected(eventId, payload)
            return
        }
        val accountId = payload.accountId?.trim().orEmpty()
        val groupId = payload.groupId?.trim().orEmpty()
        val sessionId = payload.sessionId?.trim().orEmpty()
        val secretKey = payload.secretKey?.trim().orEmpty()
        val missing = buildList {
            if (accountId.isEmpty()) add("accountId")
            if (groupId.isEmpty()) add("groupId")
            if (sessionId.isEmpty()) add("sessionId")
            if (secretKey.isEmpty()) add("secretKey")
        }
        if (missing.isNotEmpty()) {
            val reason = "registration accepted without ${missing.joinToString(", ")}"
            val withheld = pendingCandles.size
            pendingCandles.clear()
            session.reject(reason)
            log.warn(
                "registration.rejected",
                identityFields(eventId) + mapOf(
                    "reason" to reason,
                    "withheldCandles" to withheld.toString()
                )
            )
            return
        }
        session.accept(
            RegistrationCredentials(
                accountId = accountId,
                groupId = groupId,
                sessionId = sessionId,
                secretKey = secretKey
            )
        )
        log.info(
            "registration.accepted",
            identityFields(eventId) + mapOf("accountId" to accountId, "groupId" to groupId, "sessionId" to sessionId)
        )
        flushPendingCandles()
    }

    private fun onRejected(eventId: String, payload: RegistrationResultPayload) {
        val reason = payload.reason?.trim()?.takeIf { it.isNotEmpty() } ?: config.registrationRejectedFallback
        val withheld = pendingCandles.size
        pendingCandles.clear()
        session.reject(reason)
        log.warn(
            "registration.rejected",
            identityFields(eventId) + mapOf(
                "reason" to reason,
                "withheldCandles" to withheld.toString()
            )
        )
    }

    private fun onCandle(envelope: ru.omsu.quietwater.adapter.kafka.dto.EventEnvelopeDto) {
        val command = mapper.map(envelope) ?: throw UnreadableEvent("candle payload is incomplete")
        log.info(
            "candle.received",
            mapOf(
                "instrument" to command.candle.instrument.key,
                "runId" to command.candle.runId,
                "sequenceNumber" to command.candle.sequenceNumber.toString(),
                "sourceEventId" to command.sourceEventId
            )
        )
        if (session.canPublish()) {
            processClosedCandle.handle(command)
            return
        }
        if (session.phase() == ru.omsu.quietwater.application.registration.RegistrationPhase.REJECTED) {
            processClosedCandle.handle(command)
            return
        }
        pendingCandles.addLast(command)
    }

    private fun flushPendingCandles() {
        while (pendingCandles.isNotEmpty()) {
            processClosedCandle.handle(pendingCandles.removeFirst())
        }
    }

    private fun identityFields(eventId: String): Map<String, String> {
        return mapOf(
            "studentId" to config.studentId,
            "strategyId" to config.strategyId,
            "service" to config.serviceName,
            "sourceEventId" to eventId
        )
    }
}
