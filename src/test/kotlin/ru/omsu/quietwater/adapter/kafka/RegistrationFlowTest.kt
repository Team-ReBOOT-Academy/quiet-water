package ru.omsu.quietwater.adapter.kafka

import com.typesafe.config.ConfigFactory
import org.slf4j.LoggerFactory
import ru.omsu.quietwater.adapter.config.EnvConfig
import ru.omsu.quietwater.adapter.kafka.inbound.IncomingEventHandler
import ru.omsu.quietwater.adapter.kafka.outbound.KafkaRecommendationPublisher
import ru.omsu.quietwater.adapter.kafka.outbound.RequestRegistration
import ru.omsu.quietwater.adapter.logging.StructuredLog
import ru.omsu.quietwater.adapter.persistence.InMemoryInstrumentWindowStore
import ru.omsu.quietwater.application.candle.ProcessClosedCandle
import ru.omsu.quietwater.application.port.outbound.OutboundSender
import ru.omsu.quietwater.application.registration.RegistrationPhase
import ru.omsu.quietwater.application.registration.RegistrationSession
import ru.omsu.quietwater.domain.walkforward.WalkForwardPolicy
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RegistrationFlowTest {

    @Test
    fun startupRequestUsesStableIdsAndDoesNotContainBrokerPassword() {
        val fixture = fixture()
        fixture.registration.sendIfNeeded()

        assertEquals(1, fixture.sender.sent.size)
        val body = fixture.sender.sent.single().value
        assertTrue(body.contains("account.registration.request"))
        assertTrue(body.contains("llnightll"))
        assertTrue(body.contains("quiet-water"))
        assertTrue(body.contains("Quiet Water"))
        assertFalse(body.contains("test-secret"))
    }

    @Test
    fun acceptedRegistrationPublishesBufferedCandle() {
        val fixture = fixture()
        fixture.handler.handle(candleEvent(eventId = "candle-1"))
        assertTrue(fixture.sender.sent.isEmpty())

        fixture.handler.handle(acceptedEvent())

        assertEquals(1, fixture.sender.sent.size)
        val body = fixture.sender.sent.single().value
        assertTrue(body.contains("strategy.recommendation.created"))
        assertTrue(body.contains("HOLD"))
        assertTrue(body.contains("candle-1"))
        assertTrue(body.contains("\"secretKey\":\"sek\""))
        assertFalse(body.contains("test-secret"))
        assertEquals(RegistrationPhase.ACCEPTED, fixture.session.phase())
    }

    @Test
    fun rejectionKeepsReasonAndDoesNotPublish() {
        val fixture = fixture()
        fixture.handler.handle(candleEvent(eventId = "candle-2"))
        fixture.handler.handle(rejectedEvent("unknown student"))

        assertTrue(fixture.sender.sent.isEmpty())
        assertEquals(RegistrationPhase.REJECTED, fixture.session.phase())
        assertEquals("unknown student", fixture.session.rejectionReason())
    }

    private fun fixture(): Fixture {
        val config = EnvConfig(
            config = ConfigFactory.load().resolve(),
            environment = mapOf(
                "KAFKA_BOOTSTRAP_SERVERS" to "localhost:9093",
                "KAFKA_USERNAME" to "student",
                "KAFKA_PASSWORD" to "test-secret",
                "KAFKA_TOPIC" to "market.candle.internal",
                "KAFKA_GROUP_ID" to "quiet-water-test"
            )
        )
        val log = StructuredLog(LoggerFactory.getLogger("test.registration"))
        val codec = PlatformEventCodec()
        val session = RegistrationSession()
        val sender = RecordingSender()
        val publisher = KafkaRecommendationPublisher(
            config = config,
            session = session,
            sender = sender,
            codec = codec,
            log = log,
            ids = { "recommendation-1" },
            clock = { Instant.parse("2026-10-22T00:00:00Z") }
        )
        val handler = IncomingEventHandler(
            config = config,
            session = session,
            processClosedCandle = ProcessClosedCandle(
                store = InMemoryInstrumentWindowStore(maxCandlesPerInstrument = 32),
                policy = WalkForwardPolicy(config.walkForwardParameters),
                publisher = publisher
            ),
            mapper = CandleMessageMapper(),
            codec = codec,
            processed = ProcessedEventIds(capacity = 100),
            log = log
        )
        val registration = RequestRegistration(
            config = config,
            session = session,
            sender = sender,
            codec = codec,
            log = log,
            ids = { "registration-1" },
            clock = { Instant.parse("2026-10-22T00:00:00Z") }
        )
        return Fixture(session, sender, handler, registration)
    }

    private fun candleEvent(eventId: String): String {
        return """
            {
              "eventId": "$eventId",
              "eventType": "market.candle.closed",
              "schemaVersion": "1.0",
              "occurredAt": "2026-10-22T00:00:01Z",
              "producer": "platform",
              "payload": {
                "ticker": "SBER",
                "runId": "run-1",
                "sequenceNumber": 1,
                "open": 10.0,
                "high": 11.0,
                "low": 9.0,
                "close": 10.5
              }
            }
        """.trimIndent()
    }

    private fun acceptedEvent(): String {
        return """
            {
              "eventId": "accepted-1",
              "eventType": "account.registration.accepted",
              "schemaVersion": "1.0",
              "occurredAt": "2026-10-22T00:00:02Z",
              "producer": "platform",
              "payload": {
                "accountId": "acc",
                "groupId": "grp",
                "sessionId": "ses",
                "secretKey": "sek"
              }
            }
        """.trimIndent()
    }

    private fun rejectedEvent(reason: String): String {
        return """
            {
              "eventId": "rejected-1",
              "eventType": "account.registration.rejected",
              "schemaVersion": "1.0",
              "occurredAt": "2026-10-22T00:00:02Z",
              "producer": "platform",
              "payload": { "reason": "$reason" }
            }
        """.trimIndent()
    }

    private class RecordingSender : OutboundSender {
        val sent = mutableListOf<Sent>()

        override fun send(topic: String, key: String, value: String) {
            sent += Sent(topic, key, value)
        }
    }

    private data class Sent(val topic: String, val key: String, val value: String)

    private data class Fixture(
        val session: RegistrationSession,
        val sender: RecordingSender,
        val handler: IncomingEventHandler,
        val registration: RequestRegistration
    )
}
