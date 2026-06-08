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

/**
 * Client interface for interacting with the Attachment Service.
 */
interface AttachmentClient {

    /**
     * Stores attachments for the given message ID.
     *
     * Note: In order to persist attachments, the caller must have write access to the Attachment Service.
     *
     * @param messageId Unique identifier for the message the attachments belong to.
     * @param attachments List of attachments to persist.
     *
     * @return A successful [Result] if the attachments were stored successfully,
     * otherwise a failed [Result] containing the underlying error.
     */
    suspend fun saveAttachments(
        messageId: Uuid,
        attachments: List<Attachment>
    ): Result<Unit>

    /**
     * Retrieves all attachments associated with the given message ID.
     *
     * @param messageId Unique identifier for the message.
     *
     * @return A successful [Result] containing the list of attachments,
     * otherwise a failed [Result] containing the underlying error.
     */
    suspend fun getAttachments(messageId: Uuid): Result<List<Attachment>>

    fun close()
}

/**
 * HTTP-based implementation of [AttachmentClient].
 *
 * @property clientProvider Provider for creating the underlying HTTP client
 * @property attachmentServiceUrl Base URL of the Attachment Service.
 */
class HttpAttachmentClient(
    clientProvider: () -> HttpClient = scopedAuthHttpClient(),
    private val attachmentServiceUrl: String = config().attachmentService.url.toString()
) : AttachmentClient {

    private val httpClient = clientProvider()

    /**
     * Sends attachments to the Attachment Service for storage.
     *
     * Note: In order to persist attachments, the caller must have write access to the Attachment Service.
     */
    override suspend fun saveAttachments(
        messageId: Uuid,
        attachments: List<Attachment>
    ): Result<Unit> {
        val response = httpClient.post("$attachmentServiceUrl/attachments/$messageId") {
            contentType(ContentType.Application.Json)
            setBody(attachments)
        }.withLogging()

        if (response.status != HttpStatusCode.Created) {
            return Result.failure(response.toAttachmentError())
        }

        return Result.success(Unit)
    }

    /**
     * Fetches attachments associated with the provided message ID.
     */
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
