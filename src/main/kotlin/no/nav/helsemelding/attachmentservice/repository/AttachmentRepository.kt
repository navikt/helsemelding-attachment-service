package no.nav.helsemelding.attachmentservice.repository

import com.google.cloud.storage.BlobId
import com.google.cloud.storage.BlobInfo
import com.google.cloud.storage.Storage
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.helsemelding.attachmentservice.model.Attachment

private val log = KotlinLogging.logger {}

interface AttachmentRepository {
    fun save(
        attachment: Attachment
    ): String

    fun read(
        messageId: String,
        fileName: String
    ): Attachment?
}

class GcsAttachmentRepository(
    private val storage: Storage,
    private val bucketName: String
) : AttachmentRepository {

    override fun save(
        attachment: Attachment
    ): String {
        log.info { "Saving attachment ${attachment.fileName} for message ${attachment.messageId}" }

        val objectName = objectName(attachment.messageId, attachment.fileName)

        val blobInfo = BlobInfo.newBuilder(
            BlobId.of(bucketName, objectName)
        )
            .setContentType(attachment.contentType)
            .build()

        storage.create(blobInfo, attachment.content)

        log.info { "Attachment saved $objectName" }
        return objectName
    }

    override fun read(
        messageId: String,
        fileName: String
    ): Attachment? {
        log.info { "Reading attachment $fileName for message $messageId" }

        val objectName = objectName(messageId, fileName)

        val blob = storage.get(bucketName, objectName)

        if (blob == null) {
            log.warn { "Attachment not found $objectName" }
            return null
        }

        log.info { "Attachment is read ${blob.name}" }
        return Attachment(
            messageId = messageId,
            fileName = fileName,
            contentType = blob.contentType,
            content = blob.getContent()
        )
    }

    private fun objectName(
        messageId: String,
        fileName: String
    ): String = "$messageId/$fileName"
}
