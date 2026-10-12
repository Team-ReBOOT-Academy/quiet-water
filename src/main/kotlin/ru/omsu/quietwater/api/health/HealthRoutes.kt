package ru.omsu.quietwater.api.health

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import ru.omsu.quietwater.api.dto.HealthResponse
import ru.omsu.quietwater.application.health.GetHealthStatus

class HealthRoutes(
    private val getHealthStatus: GetHealthStatus
) {

    fun register(application: Application) {
        application.routing {
            get(ROOT_PATH) { respondHealth() }
            get(PATH) { respondHealth() }
        }
    }

    private suspend fun io.ktor.server.routing.RoutingContext.respondHealth() {
        call.respond(HttpStatusCode.OK, HealthResponse.from(getHealthStatus.execute()))
    }

    companion object {
        const val ROOT_PATH: String = "/"
        const val PATH: String = "/health"
    }
}
