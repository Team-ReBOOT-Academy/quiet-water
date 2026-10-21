package ru.omsu.quietwater.adapter.kafka.outbound

import ru.omsu.quietwater.adapter.config.EnvConfig
import ru.omsu.quietwater.adapter.kafka.PlatformEventCodec
import ru.omsu.quietwater.adapter.kafka.dto.RecommendationPayloadDto
import ru.omsu.quietwater.adapter.logging.StructuredLog
import ru.omsu.quietwater.application.port.outbound.OutboundSender
import ru.omsu.quietwater.application.port.outbound.RecommendationPublisher
import ru.omsu.quietwater.application.registration.RegistrationPhase
import ru.omsu.quietwater.application.registration.RegistrationSession
import ru.omsu.quietwater.domain.recommendation.Recommendation
import java.time.Instant
import java.util.UUID

class KafkaRecommendationPublisher(
    private val config: EnvConfig,
    private val session: RegistrationSession,
    private val sender: OutboundSender,
    private val codec: PlatformEventCodec,
    private val log: StructuredLog,
    private val ids: () -> String = { UUID.randomUUID().toString() },
    private val clock: () -> Instant = Instant::now
) : RecommendationPublisher {

    override fun publish(recommendation: Recommendation) {
        val credentials = session.credentials()
        if (!session.canPublish() || credentials == null) {
            log.warn(
                "recommendation.withheld",
                mapOf(
                    "reason" to withholdReason(),
                    "instrument" to recommendation.instrument.key,
                    "runId" to recommendation.runId,
                    "sequenceNumber" to recommendation.sequenceNumber.toString(),
                    "signal" to recommendation.signal.name,
                    "sourceEventId" to recommendation.sourceEventId
                )
            )
            return
        }
        val eventId = ids()
        val envelope = codec.envelope(
            eventType = config.events.recommendationCreated,
            producer = config.serviceName,
            schemaVersion = config.events.schemaVersion,
            payload = RecommendationPayloadDto(
                recommendationId = eventId,
                sourceEventId = recommendation.sourceEventId,
                runId = recommendation.runId,
                secretKey = credentials.secretKey,
                strategyId = config.strategyId,
                accountId = credentials.accountId,
                groupId = credentials.groupId,
                sessionId = credentials.sessionId,
                instrument = recommendation.instrument.key,
                sequenceNumber = recommendation.sequenceNumber,
                signal = recommendation.signal.name,
                confidence = recommendation.confidence,
                suggestedQuantity = recommendation.suggestedQuantity,
                dataQuality = recommendation.dataQuality
            ),
            serializer = RecommendationPayloadDto.serializer(),
            eventId = eventId,
            occurredAt = clock().toString(),
            correlationId = recommendation.runId,
            causationId = recommendation.sourceEventId
        )
        sender.send(config.outboundTopic(), recommendation.instrument.key, codec.encode(envelope))
        log.info(
            "recommendation.published",
            mapOf(
                "recommendationId" to eventId,
                "sourceEventId" to recommendation.sourceEventId,
                "instrument" to recommendation.instrument.key,
                "runId" to recommendation.runId,
                "sequenceNumber" to recommendation.sequenceNumber.toString(),
                "signal" to recommendation.signal.name
            )
        )
    }

    private fun withholdReason(): String {
        return when (session.phase()) {
            RegistrationPhase.REJECTED -> session.rejectionReason() ?: config.registrationRejectedFallback
            RegistrationPhase.PENDING -> config.registrationPendingReason
            RegistrationPhase.ACCEPTED -> config.registrationPendingReason
        }
    }
}
