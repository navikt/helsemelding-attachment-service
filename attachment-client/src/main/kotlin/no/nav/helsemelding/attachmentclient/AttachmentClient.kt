package no.nav.helsemelding.attachmentclient

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.client.statement.request
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import no.nav.helsemelding.attachmentclient.config.config
import no.nav.helsemelding.attachmentmodel.model.Attachment
import no.nav.helsemelding.attachmentmodel.model.AttachmentError
import kotlin.uuid.Uuid

private val log = KotlinLogging.logger {}

interface AttachmentClient {
    suspend fun saveAttachments(
        messageId: Uuid,
        attachments: List<Attachment>
    ): Result<Unit>

    suspend fun getAttachments(messageId: Uuid): Result<List<Attachment>>

    fun close()
}

class HttpAttachmentClient(
    clientProvider: () -> HttpClient = scopedAuthHttpClient(),
    private val attachmentServiceUrl: String = config().attachmentService.url.toString()
) : AttachmentClient {

    private val httpClient = clientProvider()

    override suspend fun saveAttachments(
        messageId: Uuid,
        attachments: List<Attachment>
    ): Result<Unit> {
        val response = httpClient.post("$attachmentServiceUrl/attachments/$messageId") {
            contentType(ContentType.Application.Json)
            setBody(attachments)
        }.withLogging()

        if (response.status != HttpStatusCode.OK) {
            return Result.failure(response.toAttachmentError())
        }

        return Result.success(Unit)
    }

    override suspend fun getAttachments(messageId: Uuid): Result<List<Attachment>> {
        val response = httpClient.get("$attachmentServiceUrl/attachments/$messageId").withLogging()

        if (response.status != HttpStatusCode.OK) {
            return Result.failure(response.toAttachmentError())
        }

        return Result.success(response.body())
    }

    override fun close() {
        httpClient.close()
    }
}

private suspend fun HttpResponse.toAttachmentError() = AttachmentError(
    code = status.value,
    message = bodyAsText()
)

private fun HttpResponse.withLogging(): HttpResponse {
    log.debug { "Response from ${request.method} ${request.url} is $status" }
    return this
}
