package ru.omsu.quietwater

import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import ru.omsu.quietwater.adapter.config.EnvConfig
import ru.omsu.quietwater.adapter.health.KafkaHealthProbe
import ru.omsu.quietwater.adapter.kafka.inbound.KafkaCandleListener
import ru.omsu.quietwater.adapter.kafka.outbound.KafkaRecommendationPublisher
import ru.omsu.quietwater.adapter.persistence.InMemoryInstrumentWindowStore
import ru.omsu.quietwater.api.health.HealthRoutes
import ru.omsu.quietwater.application.candle.ProcessClosedCandle
import ru.omsu.quietwater.application.health.GetHealthStatus
import ru.omsu.quietwater.application.port.inbound.CandleListener
import ru.omsu.quietwater.domain.walkforward.WalkForwardPolicy

class QuietWaterApplication(
    private val config: EnvConfig
) {

    private val logger = LoggerFactory.getLogger(QuietWaterApplication::class.java)

    private val store = InMemoryInstrumentWindowStore(
        maxCandlesPerInstrument = config.walkForwardParameters.learnSize +
            config.walkForwardParameters.examSize +
            config.walkForwardParameters.horizon +
            config.walkForwardParameters.scoreWindow
    )
    private val processClosedCandle = ProcessClosedCandle(
        store = store,
        policy = WalkForwardPolicy(config.walkForwardParameters),
        publisher = KafkaRecommendationPublisher(config)
    )
    private val candleListener: CandleListener = KafkaCandleListener(config, processClosedCandle)
    private val getHealthStatus = GetHealthStatus(
        serviceName = config.serviceName,
        serviceVersion = config.serviceVersion,
        probes = listOf(KafkaHealthProbe(config))
    )
    private val healthRoutes = HealthRoutes(getHealthStatus)

    fun module(): Application.() -> Unit = {
        install(ContentNegotiation) {
            json(
                Json {
                    prettyPrint = true
                    encodeDefaults = true
                }
            )
        }
        healthRoutes.register(this)
    }

    fun start() {
        val engine = embeddedServer(
            factory = Netty,
            port = config.httpPort,
            host = config.httpHost,
            module = module()
        )
        candleListener.start()
        Runtime.getRuntime().addShutdownHook(
            Thread {
                logger.info("Получен сигнал завершения")
                candleListener.stop()
                engine.stop(config.shutdownGraceMillis, config.shutdownTimeoutMillis)
            }
        )
        logger.info("quiet-water слушает {}:{}", config.httpHost, config.httpPort)
        engine.start(wait = true)
    }
}

fun main() {
    QuietWaterApplication(EnvConfig.load()).start()
}
