package ru.omsu.quietwater.adapter.kafka.dto

import kotlinx.serialization.Serializable

@Serializable
data class RegistrationResultPayload(
    val accountId: String? = null,
    val groupId: String? = null,
    val sessionId: String? = null,
    val secretKey: String? = null,
    val reason: String? = null,
    val accepted: Boolean? = null
)
