package no.nav.helsemelding.attachmentservice.repository

import com.google.cloud.storage.BlobId
import com.google.cloud.storage.BlobInfo
import com.google.cloud.storage.Storage
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.helsemelding.attachmentservice.model.Attachment
import kotlin.uuid.Uuid

private val log = KotlinLogging.logger {}

interface AttachmentRepository {
    fun save(
        attachment: Attachment
    ): String

    fun read(
        messageId: Uuid,
        fileName: String
    ): Attachment?

    fun readAllByMessageId(messageId: Uuid): List<Attachment>
}

class GcsAttachmentRepository(
    private val storage: Storage,
    private val bucketName: String
) : AttachmentRepository {

    override fun save(
        attachment: Attachment
    ): String {
        log.info { "Saving attachment ${attachment.fileName} for message ${attachment.messageId}" }

        val objectName = objectName(attachment.messageId, attachment.attachmentId)

        val blobInfo = BlobInfo.newBuilder(
            BlobId.of(bucketName, objectName)
        )
            .setContentType(attachment.contentType)
            .setMetadata(
                mapOf(
                    "fileName" to attachment.fileName
                )
            )
            .build()

        storage.create(blobInfo, attachment.content)

        log.info { "Attachment saved $objectName" }
        return objectName
    }

    override fun read(
        messageId: Uuid,
        attachmentId: String
    ): Attachment? {
        log.info { "Reading attachment $attachmentId for message $messageId" }

        val objectName = objectName(messageId, attachmentId)

        val blob = storage.get(bucketName, objectName)

        if (blob == null) {
            log.warn { "Attachment not found $objectName" }
            return null
        }

        log.info { "Attachment is read ${blob.name}" }
        return Attachment(
            messageId = messageId,
            attachmentId = attachmentId,
            fileName = blob.metadata?.get("fileName")!!,
            contentType = blob.contentType,
            content = blob.getContent()
        )
    }

    override fun readAllByMessageId(messageId: Uuid): List<Attachment> {
        log.info { "Reading all attachments for message $messageId" }

        val prefix = "$messageId/"

        return storage.list(
            bucketName,
            Storage.BlobListOption.prefix(prefix)
        )
            .iterateAll()
            .mapNotNull { blob ->
                val attachmentId = blob.name.removePrefix(prefix)

                read(
                    messageId = messageId,
                    attachmentId = attachmentId
                )
            }
    }

    private fun objectName(
        messageId: Uuid,
        attachmentId: String
    ): String = "$messageId/$attachmentId"
}
