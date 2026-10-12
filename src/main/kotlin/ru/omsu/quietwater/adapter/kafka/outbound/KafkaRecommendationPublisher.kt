package ru.omsu.quietwater.adapter.kafka.outbound

import org.slf4j.LoggerFactory
import ru.omsu.quietwater.adapter.config.EnvConfig
import ru.omsu.quietwater.application.port.outbound.RecommendationPublisher
import ru.omsu.quietwater.domain.recommendation.Recommendation

class KafkaRecommendationPublisher(
    private val config: EnvConfig
) : RecommendationPublisher {

    private val logger = LoggerFactory.getLogger(KafkaRecommendationPublisher::class.java)

    override fun publish(recommendation: Recommendation) {
        if (!config.isKafkaConfigured()) {
            logger.debug(
                "Пропуск produce: {} {} seq={} signal={}",
                recommendation.instrument.key,
                recommendation.runId,
                recommendation.sequenceNumber,
                recommendation.signal
            )
            return
        }
        logger.info("Клиент Kafka ещё не подключён, рекомендация не отправлена")
    }
}
