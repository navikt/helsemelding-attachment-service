package no.nav.helsemelding.attachmentservice.plugin

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode.Companion.BadRequest
import io.ktor.http.HttpStatusCode.Companion.NotFound
import io.ktor.http.HttpStatusCode.Companion.OK
import io.ktor.http.contentType
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import io.micrometer.prometheus.PrometheusConfig
import io.micrometer.prometheus.PrometheusMeterRegistry
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import no.nav.helsemelding.attachmentservice.model.Attachment
import no.nav.helsemelding.attachmentservice.repository.FakeAttachmentRepository
import kotlin.uuid.Uuid

class RoutesSpec : StringSpec({
    val testAttachments = listOf(
        Attachment("attachment.txt", "text/plain", "Arbitrary text here".toByteArray()),
        Attachment("attachment2.txt", "text/plain", "More arbitrary text here".toByteArray())
    )

    lateinit var repository: FakeAttachmentRepository

    beforeEach {
        repository = FakeAttachmentRepository()
    }

    suspend fun withTestApplication(
        testBlock: suspend ApplicationTestBuilder.() -> Unit
    ) {
        testApplication {
            application {
                installContentNegotiation()
                configureRoutes(
                    registry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT),
                    attachmentRepository = repository
                )
            }
            testBlock()
        }
    }

    "POST /attachments/{messageId} saves attachments" {
        withTestApplication {
            val messageId = Uuid.random()

            val response = client.post("/attachments/$messageId") {
                contentType(ContentType.Application.Json)
                setBody(Json.encodeToString(testAttachments))
            }

            response.status shouldBe OK
            repository.read(messageId) shouldBe testAttachments
        }
    }

    "POST /attachments/{messageId} returns Bad Request when request body is invalid" {
        withTestApplication {
            val messageId = Uuid.random()
            val response = client.post("/attachments/$messageId") {
                contentType(ContentType.Application.Json)
                setBody("{ invalid json }")
            }

            response.status shouldBe BadRequest
        }
    }

    "POST /attachments/{messageId} returns Bad Request when messageId is invalid" {
        withTestApplication {
            val response = client.post("/attachments/invalid-uuid") {
                contentType(ContentType.Application.Json)
                setBody(Json.encodeToString(testAttachments))
            }

            response.status shouldBe BadRequest
        }
    }

    "GET /attachments/{messageId} returns list of attachments" {
        withTestApplication {
            val messageId = Uuid.random()
            repository.save(messageId, testAttachments)

            val response = client.get("/attachments/$messageId")

            response.status shouldBe OK

            val attachmentsRetrieved = Json.decodeFromString<List<Attachment>>(response.bodyAsText())
            attachmentsRetrieved shouldBe testAttachments
        }
    }

    "GET /attachments/{messageId} returns Not Found when attachments not found" {
        withTestApplication {
            val messageId = Uuid.random()
            repository.save(messageId, emptyList())

            val response = client.get("/attachments/$messageId")

            response.status shouldBe NotFound
        }
    }

    "GET /attachments/{messageId} returns Bad Request when messageId is invalid" {
        withTestApplication {
            val response = client.get("/attachments/invalid-uuid")

            response.status shouldBe BadRequest
        }
    }
})
