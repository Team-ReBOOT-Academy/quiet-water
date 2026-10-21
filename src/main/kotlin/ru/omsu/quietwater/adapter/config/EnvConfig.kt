package ru.omsu.quietwater.adapter.config

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import ru.omsu.quietwater.domain.walkforward.WalkForwardParameters

data class PlatformEventTypes(
    val schemaVersion: String,
    val registrationRequest: String,
    val registrationAccepted: String,
    val registrationRejected: String,
    val candleClosed: String,
    val recommendationCreated: String
)

class EnvConfig(
    private val config: Config,
    private val environment: Map<String, String>
) {

    val serviceName: String = envOr("SERVICE_NAME", "quiet-water.service.name")
    val serviceVersion: String = envOr("SERVICE_VERSION", "quiet-water.service.version")
    val httpHost: String = envOr("HTTP_HOST", "ktor.deployment.host")
    val httpPort: Int = envOrInt("HTTP_PORT", "ktor.deployment.port")
    val shutdownGraceMillis: Long = config.getLong("quiet-water.shutdown.grace-millis")
    val shutdownTimeoutMillis: Long = config.getLong("quiet-water.shutdown.timeout-millis")

    val studentId: String = envOr("STUDENT_ID", "quiet-water.identity.student-id")
    val strategyId: String = envOr("STRATEGY_ID", "quiet-water.identity.strategy-id")
    val displayName: String = envOr("DISPLAY_NAME", "quiet-water.identity.display-name")

    val events: PlatformEventTypes = PlatformEventTypes(
        schemaVersion = config.getString("quiet-water.events.schema-version"),
        registrationRequest = config.getString("quiet-water.events.registration-request"),
        registrationAccepted = config.getString("quiet-water.events.registration-accepted"),
        registrationRejected = config.getString("quiet-water.events.registration-rejected"),
        candleClosed = config.getString("quiet-water.events.candle-closed"),
        recommendationCreated = config.getString("quiet-water.events.recommendation-created")
    )

    val registrationRejectedFallback: String = config.getString("quiet-water.registration.rejected-without-reason")
    val registrationPendingReason: String = config.getString("quiet-water.registration.pending-reason")

    val kafkaBootstrapServers: String? = env("KAFKA_BOOTSTRAP_SERVERS")
    val kafkaUsername: String? = env("KAFKA_USERNAME")
    val kafkaPassword: String? = env("KAFKA_PASSWORD")
    val kafkaTopic: String? = env("KAFKA_TOPIC")
    val kafkaOutboundTopic: String? = env("KAFKA_OUTBOUND_TOPIC") ?: kafkaTopic
    val kafkaGroupId: String = envOr("KAFKA_GROUP_ID", "quiet-water.kafka.group-id")
    val kafkaSecurityProtocol: String = envOr("KAFKA_SECURITY_PROTOCOL", "quiet-water.kafka.security-protocol")
    val kafkaSaslMechanism: String = envOr("KAFKA_SASL_MECHANISM", "quiet-water.kafka.sasl-mechanism")
    val kafkaAutoOffsetReset: String = envOr("KAFKA_AUTO_OFFSET_RESET", "quiet-water.kafka.auto-offset-reset")
    val kafkaPollMillis: Long = config.getLong("quiet-water.kafka.poll-millis")
    val kafkaReconnectInitialMillis: Long = config.getLong("quiet-water.kafka.reconnect-initial-millis")
    val kafkaReconnectMaxMillis: Long = config.getLong("quiet-water.kafka.reconnect-max-millis")
    val kafkaSendTimeoutMillis: Long = config.getLong("quiet-water.kafka.send-timeout-millis")
    val kafkaCloseMillis: Long = config.getLong("quiet-water.kafka.close-millis")
    val kafkaDedupCapacity: Int = config.getInt("quiet-water.kafka.dedup-capacity")
    val kafkaUnconfiguredDetails: String = config.getString("quiet-water.kafka.unconfigured-details")
    val kafkaUnavailableDetails: String = config.getString("quiet-water.kafka.unavailable-details")
    val kafkaUpDetails: String = config.getString("quiet-water.kafka.up-details")

    val walkForwardParameters: WalkForwardParameters = WalkForwardParameters(
        learnSize = envOrInt("N", "quiet-water.strategy.n"),
        examSize = envOrInt("K", "quiet-water.strategy.k"),
        horizon = envOrInt("M", "quiet-water.strategy.m"),
        scoreWindow = envOrInt("W", "quiet-water.strategy.w"),
        passThreshold = envOrDouble("T", "quiet-water.strategy.t"),
        rsiPeriod = envOrInt("RSI_PERIOD", "quiet-water.strategy.rsi-period"),
        rsiOversold = envOrInt("RSI_OVERSOLD", "quiet-water.strategy.rsi-oversold"),
        rsiOverbought = envOrInt("RSI_OVERBOUGHT", "quiet-water.strategy.rsi-overbought")
    )

    fun isKafkaConfigured(): Boolean {
        if (kafkaBootstrapServers.isNullOrBlank() || kafkaTopic.isNullOrBlank() || kafkaGroupId.isBlank()) {
            return false
        }
        if (kafkaSecurityProtocol.equals(PLAINTEXT, ignoreCase = true)) {
            return true
        }
        return !kafkaUsername.isNullOrBlank() && !kafkaPassword.isNullOrBlank()
    }

    fun inboundTopic(): String {
        return kafkaTopic ?: error("Входящий топик Kafka не задан")
    }

    fun outboundTopic(): String {
        return kafkaOutboundTopic ?: error("Исходящий топик Kafka не задан")
    }

    private fun env(name: String): String? {
        return environment[name]?.trim()?.takeIf { it.isNotEmpty() }
    }

    private fun envOr(name: String, configPath: String): String {
        return env(name) ?: config.getString(configPath)
    }

    private fun envOrInt(name: String, configPath: String): Int {
        return env(name)?.toInt() ?: config.getInt(configPath)
    }

    private fun envOrDouble(name: String, configPath: String): Double {
        return env(name)?.toDouble() ?: config.getDouble(configPath)
    }

    companion object {
        const val PLAINTEXT: String = "PLAINTEXT"

        fun load(): EnvConfig {
            return EnvConfig(
                config = ConfigFactory.load().resolve(),
                environment = System.getenv()
            )
        }
    }
}
