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
import ru.omsu.quietwater.adapter.health.KafkaLink
import ru.omsu.quietwater.adapter.kafka.CandleMessageMapper
import ru.omsu.quietwater.adapter.kafka.KafkaClientProperties
import ru.omsu.quietwater.adapter.kafka.KafkaProducerHandle
import ru.omsu.quietwater.adapter.kafka.PlatformEventCodec
import ru.omsu.quietwater.adapter.kafka.ProcessedEventIds
import ru.omsu.quietwater.adapter.kafka.inbound.IncomingEventHandler
import ru.omsu.quietwater.adapter.kafka.inbound.KafkaCandleListener
import ru.omsu.quietwater.adapter.kafka.outbound.KafkaRecommendationPublisher
import ru.omsu.quietwater.adapter.kafka.outbound.RequestRegistration
import ru.omsu.quietwater.adapter.logging.StructuredLog
import ru.omsu.quietwater.adapter.persistence.InMemoryInstrumentWindowStore
import ru.omsu.quietwater.api.health.HealthRoutes
import ru.omsu.quietwater.application.candle.ProcessClosedCandle
import ru.omsu.quietwater.application.health.GetHealthStatus
import ru.omsu.quietwater.application.port.inbound.CandleListener
import ru.omsu.quietwater.application.registration.RegistrationSession
import ru.omsu.quietwater.domain.walkforward.WalkForwardPolicy

class QuietWaterApplication(
    private val config: EnvConfig
) {
    private val logger = LoggerFactory.getLogger(QuietWaterApplication::class.java)
    private val events = StructuredLog(LoggerFactory.getLogger("ru.omsu.quietwater.events"))
    private val codec = PlatformEventCodec()
    private val session = RegistrationSession()
    private val link = KafkaLink().also { created ->
        if (config.isKafkaConfigured()) {
            created.markUnavailable(config.kafkaUnavailableDetails)
        } else {
            created.markNotConfigured(config.kafkaUnconfiguredDetails)
        }
    }
    private val sender = KafkaProducerHandle(config.kafkaSendTimeoutMillis)
    private val publisher = KafkaRecommendationPublisher(
        config = config,
        session = session,
        sender = sender,
        codec = codec,
        log = events
    )
    private val store = InMemoryInstrumentWindowStore(
        maxCandlesPerInstrument = config.walkForwardParameters.learnSize +
            config.walkForwardParameters.examSize +
            config.walkForwardParameters.horizon +
            config.walkForwardParameters.scoreWindow
    )
    private val processClosedCandle = ProcessClosedCandle(
        store = store,
        policy = WalkForwardPolicy(config.walkForwardParameters),
        publisher = publisher
    )
    private val candleListener: CandleListener = KafkaCandleListener(
        config = config,
        link = link,
        producerHandle = sender,
        requestRegistration = RequestRegistration(
            config = config,
            session = session,
            sender = sender,
            codec = codec,
            log = events
        ),
        handler = IncomingEventHandler(
            config = config,
            session = session,
            processClosedCandle = processClosedCandle,
            mapper = CandleMessageMapper(),
            codec = codec,
            processed = ProcessedEventIds(config.kafkaDedupCapacity),
            log = events
        ),
        properties = KafkaClientProperties(config),
        log = events
    )
    private val getHealthStatus = GetHealthStatus(
        serviceName = config.serviceName,
        serviceVersion = config.serviceVersion,
        probes = listOf(KafkaHealthProbe(link))
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
