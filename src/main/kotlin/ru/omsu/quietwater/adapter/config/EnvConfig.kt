package ru.omsu.quietwater.adapter.config

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import ru.omsu.quietwater.domain.walkforward.WalkForwardParameters

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

    val kafkaBootstrapServers: String? = env("KAFKA_BOOTSTRAP_SERVERS")
    val kafkaUsername: String? = env("KAFKA_USERNAME")
    val kafkaPassword: String? = env("KAFKA_PASSWORD")
    val kafkaTopic: String? = env("KAFKA_TOPIC")
    val kafkaGroupId: String? = env("KAFKA_GROUP_ID")
    val kafkaUnconfiguredDetails: String = config.getString("quiet-water.kafka.unconfigured-details")
    val kafkaConfiguredDetails: String = config.getString("quiet-water.kafka.configured-details")

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
        return !kafkaBootstrapServers.isNullOrBlank() &&
            !kafkaUsername.isNullOrBlank() &&
            !kafkaTopic.isNullOrBlank()
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
        fun load(): EnvConfig {
            return EnvConfig(
                config = ConfigFactory.load().resolve(),
                environment = System.getenv()
            )
        }
    }
}
