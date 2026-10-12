package ru.omsu.quietwater.adapter.kafka.inbound

import org.slf4j.LoggerFactory
import ru.omsu.quietwater.adapter.config.EnvConfig
import ru.omsu.quietwater.application.candle.ProcessClosedCandle
import ru.omsu.quietwater.application.port.inbound.CandleListener

class KafkaCandleListener(
    private val config: EnvConfig,
    @Suppress("unused") private val processClosedCandle: ProcessClosedCandle
) : CandleListener {

    private val logger = LoggerFactory.getLogger(KafkaCandleListener::class.java)

    override fun start() {
        if (!config.isKafkaConfigured()) {
            logger.info("Kafka не сконфигурирована, listener не запускается")
            return
        }
        logger.info("Клиент Kafka ещё не подключён (задание про брокер впереди)")
    }

    override fun stop() {
        logger.info("Остановка Kafka listener (клиент не был создан)")
    }
}
