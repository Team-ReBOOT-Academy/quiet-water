package ru.omsu.quietwater.adapter.kafka.dto

import kotlinx.serialization.Serializable

@Serializable
data class RegistrationRequestPayload(
    val name: String,
    val studentId: String,
    val strategyId: String,
    val serviceName: String
)
