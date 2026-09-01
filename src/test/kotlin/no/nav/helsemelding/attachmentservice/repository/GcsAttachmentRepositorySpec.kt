package no.nav.helsemelding.attachmentservice.repository

import com.google.cloud.storage.Blob
import com.google.cloud.storage.BlobInfo
import com.google.cloud.storage.Storage
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import no.nav.helsemelding.attachmentservice.buildTestAttachments
import kotlin.uuid.Uuid

class GcsAttachmentRepositorySpec : StringSpec({

    val bucketName = "test-bucket"
    val messageId = Uuid.random()
    val testAttachments = buildTestAttachments()

    val storage = mockk<Storage>()
    val repository = GcsAttachmentRepository(
        storage = storage,
        bucketName = bucketName
    )

    "save should store attachments in GCS bucket when they do not exist" {
        val blob = mockk<Blob>()
        val content = Json.encodeToString(testAttachments).toByteArray()

        every { storage.create(any<BlobInfo>(), content) } returns blob

        val result = repository.save(messageId, testAttachments)

        result shouldBe content.size
    }

    "save should skip storing attachment when it already exist" {
        val existingBlob = mockk<Blob>()
        val existingContent = Json.encodeToString(testAttachments).toByteArray()

        every { storage.create(any<BlobInfo>(), any<ByteArray>()) } throws RuntimeException("retention policy")
        every { storage.get(bucketName, messageId.toString()) } returns existingBlob
        every { existingBlob.getContent() } returns existingContent

        val result = repository.save(messageId, testAttachments)

        result shouldBe existingContent.size
    }

    "save should rethrow exception when create fails and attachments do not exist" {
        every { storage.create(any<BlobInfo>(), any<ByteArray>()) } throws RuntimeException("network error")
        every { storage.get(bucketName, messageId.toString()) } returns null

        shouldThrow<RuntimeException> {
            repository.save(messageId, testAttachments)
        }
    }

    "read should read attachments from GCS bucket" {
        val blob = mockk<Blob>()

        every {
            storage.get(bucketName, messageId.toString())
        } returns blob

        every { blob.getContent() } returns Json.encodeToString(testAttachments).toByteArray()

        val result = repository.read(messageId)

        result shouldBe testAttachments
    }

    "read should return empty list when attachment is not found" {
        val missingMessageId = Uuid.random()

        every {
            storage.get(bucketName, missingMessageId.toString())
        } returns null

        val result = repository.read(missingMessageId)

        result shouldBe emptyList()
    }
})
