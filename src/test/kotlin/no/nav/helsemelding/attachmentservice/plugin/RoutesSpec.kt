package no.nav.helsemelding.attachmentservice.plugin

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode.Companion.BadRequest
import io.ktor.http.HttpStatusCode.Companion.Created
import io.ktor.http.HttpStatusCode.Companion.Forbidden
import io.ktor.http.HttpStatusCode.Companion.InternalServerError
import io.ktor.http.HttpStatusCode.Companion.NotFound
import io.ktor.http.HttpStatusCode.Companion.OK
import io.ktor.http.HttpStatusCode.Companion.Unauthorized
import io.ktor.http.contentType
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import io.micrometer.prometheus.PrometheusConfig
import io.micrometer.prometheus.PrometheusMeterRegistry
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import no.nav.helsemelding.attachmentmodel.model.Attachment
import no.nav.helsemelding.attachmentservice.buildTestAttachments
import no.nav.helsemelding.attachmentservice.repository.FakeAttachmentRepository
import no.nav.security.mock.oauth2.MockOAuth2Server
import no.nav.security.mock.oauth2.token.DefaultOAuth2TokenCallback
import kotlin.uuid.Uuid

private const val CLIENT_WITH_WRITE_ACCESS = "a284f235-78ac-4df9-8f1e-441783dbaccb"
private const val CLIENT_WITHOUT_WRITE_ACCESS = "11111111-2222-3333-4444-555555555555"

class RoutesSpec : StringSpec({
    lateinit var repository: FakeAttachmentRepository
    lateinit var mockOAuth2Server: MockOAuth2Server

    val testAttachments = buildTestAttachments()

    beforeSpec {
        mockOAuth2Server = MockOAuth2Server()
        mockOAuth2Server.start(port = 3344)
    }

    afterSpec {
        mockOAuth2Server.shutdown()
    }

    beforeEach {
        repository = FakeAttachmentRepository()
    }

    fun getToken(clientId: String = CLIENT_WITH_WRITE_ACCESS) = mockOAuth2Server.issueToken(
        issuerId = "AZURE_AD",
        clientId = clientId,
        tokenCallback = DefaultOAuth2TokenCallback(
            audience = listOf("api://dev-gcp.helsemelding.attachment-service")
        )
    )

    fun withTestApplication(
        testBlock: suspend ApplicationTestBuilder.() -> Unit
    ) {
        testApplication {
            application {
                installContentNegotiation()
                configureAuthentication()
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
                bearerAuth(getToken().serialize())
                contentType(ContentType.Application.Json)
                setBody(Json.encodeToString(testAttachments))
            }

            response.status shouldBe Created
            repository.read(messageId) shouldBe testAttachments
        }
    }

    "POST /attachments/{messageId} returns Forbidden when the client does not have write access" {
        withTestApplication {
            val messageId = Uuid.random()

            val response = client.post("/attachments/$messageId") {
                bearerAuth(getToken(CLIENT_WITHOUT_WRITE_ACCESS).serialize())
                contentType(ContentType.Application.Json)
                setBody(Json.encodeToString(testAttachments))
            }

            response.status shouldBe Forbidden
        }
    }

    "POST /attachments/{messageId} returns Bad Request when request body is invalid" {
        withTestApplication {
            val messageId = Uuid.random()
            val response = client.post("/attachments/$messageId") {
                bearerAuth(getToken().serialize())
                contentType(ContentType.Application.Json)
                setBody("{ invalid json }")
            }

            response.status shouldBe BadRequest
        }
    }

    "POST /attachments/{messageId} returns Bad Request when messageId is invalid" {
        withTestApplication {
            val response = client.post("/attachments/invalid-uuid") {
                bearerAuth(getToken().serialize())
                contentType(ContentType.Application.Json)
                setBody(Json.encodeToString(testAttachments))
            }

            response.status shouldBe BadRequest
        }
    }

    "POST /attachments/{messageId} returns Not Found when messageId is missing" {
        withTestApplication {
            val response = client.post("/attachments/") {
                bearerAuth(getToken().serialize())
                contentType(ContentType.Application.Json)
                setBody(Json.encodeToString(testAttachments))
            }

            response.status shouldBe NotFound
        }
    }

    "POST /attachments/{messageId} returns Internal Server Error when repository throws exception" {
        withTestApplication {
            val messageId = Uuid.random()
            repository.givenSaveThrowsException(true)

            val response = client.post("/attachments/$messageId") {
                bearerAuth(getToken().serialize())
                contentType(ContentType.Application.Json)
                setBody(Json.encodeToString(testAttachments))
            }

            response.status shouldBe InternalServerError
        }
    }

    "POST /attachments/{messageId} returns Unauthorized without Azure AD token" {
        withTestApplication {
            val messageId = Uuid.random()

            val response = client.post("/attachments/$messageId") {
                contentType(ContentType.Application.Json)
                setBody(Json.encodeToString(testAttachments))
            }

            response.status shouldBe Unauthorized
        }
    }

    "POST /attachments/{messageId} returns Unauthorized with invalid Azure AD token" {
        withTestApplication {
            val messageId = Uuid.random()

            val response = client.post("/attachments/$messageId") {
                bearerAuth("invalid-token")
                contentType(ContentType.Application.Json)
                setBody(Json.encodeToString(testAttachments))
            }

            response.status shouldBe Unauthorized
        }
    }

    "GET /attachments/{messageId} returns list of attachments" {
        withTestApplication {
            val messageId = Uuid.random()
            repository.save(messageId, testAttachments)

            val response = client.get("/attachments/$messageId") {
                bearerAuth(getToken().serialize())
            }

            response.status shouldBe OK

            val attachmentsRetrieved = Json.decodeFromString<List<Attachment>>(response.bodyAsText())
            attachmentsRetrieved shouldBe testAttachments
        }
    }

    "GET /attachments/{messageId} returns list of attachments client does not have write access" {
        withTestApplication {
            val messageId = Uuid.random()
            repository.save(messageId, testAttachments)

            val response = client.get("/attachments/$messageId") {
                bearerAuth(getToken(CLIENT_WITHOUT_WRITE_ACCESS).serialize())
            }

            response.status shouldBe OK

            val attachmentsRetrieved = Json.decodeFromString<List<Attachment>>(response.bodyAsText())
            attachmentsRetrieved shouldBe testAttachments
        }
    }

    "GET /attachments/{messageId} returns Not Found when attachments not found" {
        withTestApplication {
            val messageId = Uuid.random()
            repository.save(messageId, emptyList())

            val response = client.get("/attachments/$messageId") {
                bearerAuth(getToken().serialize())
            }

            response.status shouldBe NotFound
        }
    }

    "GET /attachments/{messageId} returns Bad Request when messageId is invalid" {
        withTestApplication {
            val response = client.get("/attachments/invalid-uuid") {
                bearerAuth(getToken().serialize())
            }

            response.status shouldBe BadRequest
        }
    }

    "GET /attachments/{messageId} returns Not Found when messageId is missing" {
        withTestApplication {
            val response = client.get("/attachments/") {
                bearerAuth(getToken().serialize())
            }

            response.status shouldBe NotFound
        }
    }

    "GET /attachments/{messageId} returns Internal Server Error when repository throws exception" {
        withTestApplication {
            val messageId = Uuid.random()
            repository.givenReadThrowsException(true)

            val response = client.get("/attachments/$messageId") {
                bearerAuth(getToken().serialize())
            }

            response.status shouldBe InternalServerError
        }
    }

    "GET /attachments/{messageId} returns Unauthorized without Azure AD token" {
        withTestApplication {
            val messageId = Uuid.random()

            val response = client.get("/attachments/$messageId")

            response.status shouldBe Unauthorized
        }
    }

    "GET /attachments/{messageId} returns Unauthorized with invalid Azure AD token" {
        withTestApplication {
            val messageId = Uuid.random()

            val response = client.get("/attachments/$messageId") {
                bearerAuth("invalid-token")
            }

            response.status shouldBe Unauthorized
        }
    }
})
