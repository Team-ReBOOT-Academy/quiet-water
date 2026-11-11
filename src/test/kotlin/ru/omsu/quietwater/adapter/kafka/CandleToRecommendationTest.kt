package ru.omsu.quietwater.adapter.kafka

import com.typesafe.config.ConfigFactory
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.longOrNull
import org.slf4j.LoggerFactory
import ru.omsu.quietwater.adapter.config.EnvConfig
import ru.omsu.quietwater.adapter.kafka.inbound.IncomingEventHandler
import ru.omsu.quietwater.adapter.kafka.inbound.UnreadableEvent
import ru.omsu.quietwater.adapter.kafka.outbound.KafkaRecommendationPublisher
import ru.omsu.quietwater.adapter.logging.StructuredLog
import ru.omsu.quietwater.adapter.persistence.InMemoryInstrumentWindowStore
import ru.omsu.quietwater.application.candle.ProcessClosedCandle
import ru.omsu.quietwater.application.port.outbound.OutboundSender
import ru.omsu.quietwater.application.registration.RegistrationCredentials
import ru.omsu.quietwater.application.registration.RegistrationSession
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CandleToRecommendationTest {

    @Test
    fun exampleCandleBecomesExactlyOneHoldRecommendation() {
        val raw = exampleMessage()
        val fixture = fixture()
        fixture.session.accept(
            RegistrationCredentials(
                accountId = "acc-1",
                groupId = "МПБ-401",
                sessionId = "ses-1",
                secretKey = "student-key"
            )
        )

        fixture.handler.handle(raw)
        fixture.handler.handle(raw)

        assertEquals(1, fixture.sender.sent.size)
        val body = fixture.sender.sent.single()
        val envelope = PlatformEventCodec().decode(body)
        val payload = envelope.payload.jsonObject

        assertEquals("strategy.recommendation.created", envelope.eventType)
        assertEquals("run-example-1", envelope.correlationId)
        assertEquals("candle-example-1", envelope.causationId)
        assertEquals("candle-example-1", payload.text("sourceEventId"))
        assertEquals("run-example-1", payload.text("runId"))
        assertEquals(1L, payload.long("sequenceNumber"))
        assertEquals("HOLD", payload.text("signal"))
        assertEquals(0.0, payload.number("confidence"))
        assertEquals(0L, payload.long("suggestedQuantity"))
        assertEquals("llnightll", payload.text("studentId"))
        assertEquals("quiet-water", payload.text("strategyId"))
        assertEquals("OK", payload.text("dataQuality"))
        assertEquals("базовая рекомендация HOLD, торговый сигнал ещё не публикуется", payload.text("reason"))
        assertTrue((payload.long("processingTimeMs") ?: -1L) >= 0L)
        assertEquals("student-key", payload.text("secretKey"))
        assertFalse(body.contains("test-secret"))
    }

    @Test
    fun incompleteExampleIsRejectedBeforeRecommendation() {
        val fixture = fixture()
        fixture.session.accept(
            RegistrationCredentials(
                accountId = "acc-1",
                groupId = "МПБ-401",
                sessionId = "ses-1",
                secretKey = "student-key"
            )
        )
        val broken = exampleMessage().replace("\"close\": 304.0,", "")

        val error = assertFailsWith<UnreadableEvent> {
            fixture.handler.handle(broken)
        }

        assertTrue(error.message.orEmpty().contains("close"))
        assertTrue(fixture.sender.sent.isEmpty())
    }

    private fun exampleMessage(): String {
        return checkNotNull(javaClass.classLoader.getResource("messages/market.candle.closed.json"))
            .readText()
    }

    private fun fixture(): Fixture {
        val config = EnvConfig(
            config = ConfigFactory.load().resolve(),
            environment = mapOf(
                "KAFKA_BOOTSTRAP_SERVERS" to "localhost:9094",
                "KAFKA_USERNAME" to "student-key",
                "KAFKA_PASSWORD" to "test-secret",
                "KAFKA_TOPIC" to "market.candle.internal",
                "KAFKA_GROUP_ID" to "quiet-water-test"
            )
        )
        val log = StructuredLog(LoggerFactory.getLogger("test.candle"))
        val codec = PlatformEventCodec()
        val session = RegistrationSession()
        val sender = RecordingSender()
        val handler = IncomingEventHandler(
            config = config,
            session = session,
            processClosedCandle = ProcessClosedCandle(
                store = InMemoryInstrumentWindowStore(maxCandlesPerInstrument = 32),
                baseline = config.baselineHold,
                publisher = KafkaRecommendationPublisher(
                    config = config,
                    session = session,
                    sender = sender,
                    codec = codec,
                    log = log,
                    ids = { "recommendation-example-1" },
                    clock = { Instant.parse("2026-11-12T00:00:01Z") }
                )
            ),
            mapper = CandleMessageMapper(),
            codec = codec,
            processed = ProcessedEventIds(capacity = 100),
            log = log
        )
        return Fixture(session, sender, handler)
    }

    private fun kotlinx.serialization.json.JsonObject.text(name: String): String? {
        return (this[name] as? JsonPrimitive)?.contentOrNull
    }

    private fun kotlinx.serialization.json.JsonObject.number(name: String): Double? {
        return (this[name] as? JsonPrimitive)?.doubleOrNull
    }

    private fun kotlinx.serialization.json.JsonObject.long(name: String): Long? {
        return (this[name] as? JsonPrimitive)?.longOrNull
    }

    private class RecordingSender : OutboundSender {
        val sent = mutableListOf<String>()

        override fun send(topic: String, key: String, value: String) {
            sent += value
        }
    }

    private data class Fixture(
        val session: RegistrationSession,
        val sender: RecordingSender,
        val handler: IncomingEventHandler
    )
}
