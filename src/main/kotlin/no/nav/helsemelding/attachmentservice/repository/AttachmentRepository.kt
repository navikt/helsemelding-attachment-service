package no.nav.helsemelding.attachmentservice.repository

import com.google.cloud.storage.BlobId
import com.google.cloud.storage.BlobInfo
import com.google.cloud.storage.Storage
import io.github.oshai.kotlinlogging.KotlinLogging

private val log = KotlinLogging.logger {}

interface AttachmentRepository {
    fun save(
        messageId: String,
        fileName: String,
        contentType: String,
        content: ByteArray
    ): String

    fun read(
        messageId: String,
        fileName: String
    ): ByteArray?
}

class GcsAttachmentRepository(
    private val storage: Storage,
    private val bucketName: String
) : AttachmentRepository {

    override fun save(
        messageId: String,
        fileName: String,
        contentType: String,
        content: ByteArray
    ): String {
        log.info { "Saving attachment $fileName for message $messageId" }

        val objectName = objectName(messageId, fileName)

        val blobInfo = BlobInfo.newBuilder(
            BlobId.of(bucketName, objectName)
        )
            .setContentType(contentType)
            .build()

        storage.create(blobInfo, content)

        log.info { "Attachment saved $objectName" }
        return objectName
    }

    override fun read(
        messageId: String,
        fileName: String
    ): ByteArray? {
        log.info { "Reading attachment $fileName for message $messageId" }

        val objectName = objectName(messageId, fileName)

        val blob = storage.get(bucketName, objectName)

        log.info { "Attachment is read ${blob?.name}" }
        return blob?.getContent()
    }

    private fun objectName(
        messageId: String,
        fileName: String
    ): String = "$messageId/$fileName"
}
