package ru.omsu.quietwater.adapter.kafka

import com.typesafe.config.ConfigFactory
import ru.omsu.quietwater.adapter.config.EnvConfig
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class KafkaClientPropertiesTest {

    @Test
    fun consumerUsesConfiguredBrokerTopicGroupAndEarliestOffset() {
        val properties = KafkaClientProperties(saslConfig()).consumer()

        assertEquals("localhost:9093", properties.getProperty(KafkaClientProperties.BOOTSTRAP_SERVERS))
        assertEquals("quiet-water-test", properties.getProperty(KafkaClientProperties.GROUP_ID))
        assertEquals("earliest", properties.getProperty(KafkaClientProperties.AUTO_OFFSET_RESET))
        assertEquals("false", properties.getProperty(KafkaClientProperties.ENABLE_AUTO_COMMIT))
        assertEquals("SASL_SSL", properties.getProperty(KafkaClientProperties.SECURITY_PROTOCOL))
        assertEquals("SCRAM-SHA-512", properties.getProperty(KafkaClientProperties.SASL_MECHANISM))
        val jaas = properties.getProperty(KafkaClientProperties.SASL_JAAS_CONFIG)
        assertTrue(jaas.contains("ScramLoginModule"))
        assertTrue(jaas.contains("student"))
    }

    @Test
    fun plaintextDoesNotAttachSaslCredentials() {
        val properties = KafkaClientProperties(plaintextConfig()).producer()

        assertEquals("PLAINTEXT", properties.getProperty(KafkaClientProperties.SECURITY_PROTOCOL))
        assertNull(properties.getProperty(KafkaClientProperties.SASL_JAAS_CONFIG))
        assertFalse(properties.getProperty(KafkaClientProperties.CLIENT_ID).isNullOrBlank())
    }

    private fun saslConfig(): EnvConfig {
        return EnvConfig(
            config = ConfigFactory.load().resolve(),
            environment = mapOf(
                "KAFKA_BOOTSTRAP_SERVERS" to "localhost:9093",
                "KAFKA_USERNAME" to "student",
                "KAFKA_PASSWORD" to "test-secret",
                "KAFKA_TOPIC" to "market.candle.internal",
                "KAFKA_GROUP_ID" to "quiet-water-test"
            )
        )
    }

    private fun plaintextConfig(): EnvConfig {
        return EnvConfig(
            config = ConfigFactory.load().resolve(),
            environment = mapOf(
                "KAFKA_BOOTSTRAP_SERVERS" to "localhost:9093",
                "KAFKA_TOPIC" to "market.candle.internal",
                "KAFKA_GROUP_ID" to "quiet-water-test",
                "KAFKA_SECURITY_PROTOCOL" to "PLAINTEXT"
            )
        )
    }
}
