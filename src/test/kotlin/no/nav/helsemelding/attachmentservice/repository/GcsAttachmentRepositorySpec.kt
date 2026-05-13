package no.nav.helsemelding.attachmentservice.repository

import com.google.cloud.storage.Blob
import com.google.cloud.storage.BlobInfo
import com.google.cloud.storage.Storage
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

    "save should store attachments in GCS bucket" {
        val blob = mockk<Blob>()

        val content = Json.encodeToString(testAttachments).toByteArray()

        every {
            storage.create(any<BlobInfo>(), content)
        } returns blob

        val result = repository.save(messageId, testAttachments)

        result shouldBe messageId.toString()
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
