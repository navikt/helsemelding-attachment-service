package no.nav.helsemelding.attachmentservice.repository

import com.google.cloud.storage.BlobId
import com.google.cloud.storage.BlobInfo
import com.google.cloud.storage.Storage
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import no.nav.helsemelding.attachmentservice.model.Attachment

private val log = KotlinLogging.logger {}

interface AttachmentRepository {
    fun save(
        messageId: String,
        attachments: List<Attachment>
    ): String

    fun read(
        messageId: String
    ): List<Attachment>
}

class GcsAttachmentRepository(
    private val storage: Storage,
    private val bucketName: String
) : AttachmentRepository {

    override fun save(
        messageId: String,
        attachments: List<Attachment>
    ): String {
        log.info { "Saving attachment for message $messageId" }

        val content = Json.encodeToString(attachments).toByteArray()

        val blobInfo = BlobInfo.newBuilder(
            BlobId.of(bucketName, messageId)
        ).build()

        storage.create(blobInfo, content)

        log.info { "Attachment saved for message $messageId" }
        return messageId
    }

    override fun read(
        messageId: String
    ): List<Attachment> {
        log.info { "Reading attachments for message $messageId" }

        val blob = storage.get(bucketName, messageId)

        if (blob == null) {
            log.warn { "Attachments not found $messageId" }
            return emptyList()
        }

        log.info { "Attachment is read ${blob.name}" }
        return Json.decodeFromString<List<Attachment>>(String(blob.getContent()))
    }
}
