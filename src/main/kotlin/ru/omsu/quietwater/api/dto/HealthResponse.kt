package ru.omsu.quietwater.api.dto

import kotlinx.serialization.Serializable
import ru.omsu.quietwater.application.health.HealthSnapshot

@Serializable
data class HealthResponse(
    val status: String,
    val service: String,
    val version: String,
    val components: Map<String, ComponentHealthResponse>
) {
    companion object {
        fun from(snapshot: HealthSnapshot): HealthResponse {
            return HealthResponse(
                status = snapshot.status.name,
                service = snapshot.service,
                version = snapshot.version,
                components = snapshot.components.mapValues { (_, result) ->
                    ComponentHealthResponse(
                        status = result.status.name,
                        details = result.details
                    )
                }
            )
        }
    }
}

@Serializable
data class ComponentHealthResponse(
    val status: String,
    val details: String
)
