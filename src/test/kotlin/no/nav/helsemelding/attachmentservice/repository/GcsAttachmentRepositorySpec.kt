package no.nav.helsemelding.attachmentservice.repository

import com.google.cloud.storage.Blob
import com.google.cloud.storage.BlobInfo
import com.google.cloud.storage.Storage
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import no.nav.helsemelding.attachmentservice.model.Attachment

class GcsAttachmentRepositorySpec : StringSpec({

    val bucketName = "test-bucket"

    val testAttachment = Attachment(
        messageId = "message-1",
        attachmentId = "attachment-1",
        fileName = "attachment.txt",
        contentType = "text/plain",
        content = "Arbitrary text her".toByteArray()
    )

    val storage = mockk<Storage>()
    val repository = GcsAttachmentRepository(
        storage = storage,
        bucketName = bucketName
    )

    "save should store a file in GCS bucket" {
        val blob = mockk<Blob>()

        every {
            storage.create(any<BlobInfo>(), testAttachment.content)
        } returns blob

        val result = repository.save(testAttachment)

        result shouldBe "message-1/attachment-1"
    }

    "read should read a file from GCS bucket" {
        val blob = mockk<Blob>()

        every {
            storage.get(bucketName, "message-1/attachment-1")
        } returns blob

        every { blob.getContent() } returns testAttachment.content
        every { blob.contentType } returns testAttachment.contentType
        every { blob.metadata } returns mapOf("fileName" to testAttachment.fileName)

        val result = repository.read(
            messageId = "message-1",
            attachmentId = "attachment-1"
        )

        result shouldBe testAttachment
    }

    "read should return null when file does not exist" {
        every {
            storage.get(bucketName, "message-1/attachment-missing")
        } returns null

        val result = repository.read(
            messageId = "message-1",
            attachmentId = "attachment-missing"
        )

        result.shouldBeNull()
    }
})
