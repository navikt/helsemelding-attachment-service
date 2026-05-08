package no.nav.helsemelding.attachmentservice

import no.nav.helsemelding.attachmentservice.model.Attachment
import java.nio.file.Files
import java.nio.file.Paths
import java.util.Base64

fun buildTestAttachments(): List<Attachment> {
    val testFileBytes = Files.readAllBytes(Paths.get("src/test/resources/content.pdf"))

    return listOf(
        Attachment(
            fileName = "attachment.txt",
            contentType = "text/plain",
            contentBase64 = "Arbitrary text here".toByteArray().toBase64()
        ),
        Attachment(
            fileName = "content.pdf",
            contentType = "application/pdf",
            contentBase64 = testFileBytes.toBase64()
        )
    )
}

fun ByteArray.toBase64() = Base64.getEncoder().encodeToString(this).toByteArray()
