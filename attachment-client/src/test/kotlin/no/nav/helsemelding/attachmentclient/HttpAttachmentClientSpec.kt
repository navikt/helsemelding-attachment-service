package no.nav.helsemelding.attachmentclient

import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.fullPath
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import no.nav.helsemelding.attachmentmodel.model.Attachment
import kotlin.uuid.Uuid

class HttpAttachmentClientSpec : StringSpec({

    val messageId = Uuid.random()
    val attachments = listOf(
        Attachment(
            description = "attachment 1",
            contentType = "text/plain",
            contentBase64 = "JVBERi0xLjU="
        ),
        Attachment(
            description = "attachment 2",
            contentType = "application/pdf",
            contentBase64 = "QXJiaXRyYXJ5IHRleHQgaGVyZQ=="
        )
    )

    "saveAttachments should send attachments to Attachment Service" {
        val client = testClient { request ->
            request.method shouldBe HttpMethod.Post
            request.url.fullPath shouldBe "/attachments/$messageId"
            request.body.contentType shouldBe ContentType.Application.Json

            respond(
                content = "",
                status = HttpStatusCode.OK
            )
        }

        client.saveAttachments(messageId, attachments).shouldBeRight()
    }

    "saveAttachments should return AttachmentError when Attachment Service returns error" {
        val client = testClient {
            respond(
                content = "Unable to save attachments",
                status = HttpStatusCode.InternalServerError
            )
        }

        val error = client.saveAttachments(messageId, attachments).shouldBeLeft()

        error.code shouldBe HttpStatusCode.InternalServerError.value
        error.message shouldBe "Unable to save attachments"
    }

    "getAttachments should return a list of attachments received from Attachment Service" {
        val client = testClient { request ->
            request.method shouldBe HttpMethod.Get
            request.url.fullPath shouldBe "/attachments/$messageId"

            respond(
                content = Json.encodeToString(attachments),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            )
        }

        val result = client.getAttachments(messageId).shouldBeRight()

        result shouldBe attachments
    }

    "getAttachments should return AttachmentError when Attachment Service returns error" {
        val client = testClient {
            respond(
                content = "Attachments not found",
                status = HttpStatusCode.NotFound
            )
        }

        val error = client.getAttachments(messageId).shouldBeLeft()

        error.code shouldBe HttpStatusCode.NotFound.value
        error.message shouldBe "Attachments not found"
    }
})

private fun testClient(
    handler: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData
): AttachmentClient = HttpAttachmentClient(
    attachmentServiceUrl = "http://localhost",
    clientProvider = {
        HttpClient(MockEngine) {
            engine { addHandler(handler) }

            install(ContentNegotiation) {
                json(
                    Json {
                        ignoreUnknownKeys = true
                        encodeDefaults = true
                    }
                )
            }
        }
    }
)
