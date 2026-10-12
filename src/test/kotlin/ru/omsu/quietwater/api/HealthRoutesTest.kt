package ru.omsu.quietwater.api

import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json
import ru.omsu.quietwater.QuietWaterApplication
import ru.omsu.quietwater.adapter.config.EnvConfig
import ru.omsu.quietwater.api.dto.HealthResponse
import com.typesafe.config.ConfigFactory
import kotlin.test.Test
import kotlin.test.assertEquals

class HealthRoutesTest {

    @Test
    fun healthReturnsOkAndDegradedKafkaPayload() = testApplication {
        val app = QuietWaterApplication(
            EnvConfig(
                config = ConfigFactory.load().resolve(),
                environment = emptyMap()
            )
        )
        application(app.module())
        val jsonClient = createClient {
            install(ContentNegotiation) {
                json(Json { encodeDefaults = true })
            }
        }

        val root = jsonClient.get("/")
        assertEquals(HttpStatusCode.OK, root.status)

        val response = jsonClient.get("/health")
        val body = response.body<HealthResponse>()

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("DEGRADED", body.status)
        assertEquals("quiet-water", body.service)
        assertEquals("1.0.0", body.version)
        assertEquals("DOWN", body.components.getValue("kafka").status)
        assertEquals("broker is not configured", body.components.getValue("kafka").details)
    }
}
