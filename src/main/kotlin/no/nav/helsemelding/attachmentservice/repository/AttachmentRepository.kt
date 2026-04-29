package no.nav.helsemelding.attachmentservice.repository

import com.google.cloud.storage.BlobId
import com.google.cloud.storage.BlobInfo
import com.google.cloud.storage.Storage

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
        val objectName = objectName(messageId, fileName)

        val blobInfo = BlobInfo.newBuilder(
            BlobId.of(bucketName, objectName)
        )
            .setContentType(contentType)
            .build()

        storage.create(blobInfo, content)

        return objectName
    }

    override fun read(
        messageId: String,
        fileName: String
    ): ByteArray? {
        val objectName = objectName(messageId, fileName)

        val blob = storage.get(bucketName, objectName)

        return blob?.getContent()
    }

    private fun objectName(
        messageId: String,
        fileName: String
    ): String = "$messageId/$fileName"
}
