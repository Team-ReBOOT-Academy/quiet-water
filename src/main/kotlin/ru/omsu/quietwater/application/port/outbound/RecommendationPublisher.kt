package ru.omsu.quietwater.application.port.outbound

import ru.omsu.quietwater.domain.recommendation.Recommendation

interface RecommendationPublisher {
    fun publish(recommendation: Recommendation)
}
