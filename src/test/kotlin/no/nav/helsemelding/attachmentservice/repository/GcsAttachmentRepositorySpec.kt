package no.nav.helsemelding.attachmentservice.repository

import com.google.api.gax.paging.Page
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

    "readAllByMessageId should return all attachments for message" {
        val blob1 = mockk<Blob>()
        val blob2 = mockk<Blob>()
        val page = mockk<Page<Blob>>()

        val attachment1 = testAttachment

        val attachment2 = testAttachment.copy(
            attachmentId = "attachment-2",
            fileName = "attachment-2.txt",
            content = "More arbitrary text her".toByteArray()
        )

        every { storage.list(bucketName, any<Storage.BlobListOption>()) } returns page
        every { page.iterateAll() } returns listOf(blob1, blob2)

        every { storage.get(bucketName, "message-1/attachment-1") } returns blob1
        every { storage.get(bucketName, "message-1/attachment-2") } returns blob2

        every { blob1.getContent() } returns attachment1.content
        every { blob1.contentType } returns attachment1.contentType
        every { blob1.metadata } returns mapOf("fileName" to attachment1.fileName)
        every { blob1.name } returns "message-1/attachment-1"

        every { blob2.getContent() } returns attachment2.content
        every { blob2.contentType } returns attachment2.contentType
        every { blob2.metadata } returns mapOf("fileName" to attachment2.fileName)
        every { blob2.name } returns "message-1/attachment-2"

        val result = repository.readAllByMessageId("message-1")

        result shouldBe listOf(attachment1, attachment2)
    }

    "readAllByMessageId should return empty list when message has no attachments" {
        val page = mockk<Page<Blob>>()

        every { storage.list(bucketName, any<Storage.BlobListOption>()) } returns page
        every { page.iterateAll() } returns emptyList()

        val result = repository.readAllByMessageId("message-without-attachments")

        result shouldBe emptyList()
    }
})
