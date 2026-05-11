package no.nav.helsemelding.attachmentservice

import no.nav.helsemelding.attachmentservice.model.Attachment
import java.nio.file.Files
import java.nio.file.Paths
import java.util.Base64

fun buildTestAttachments(): List<Attachment> {
    val testFileBytes = Files.readAllBytes(Paths.get("src/test/resources/content.pdf"))

    return listOf(
        Attachment(
            description = "attachment 1",
            contentType = "text/plain",
            contentBase64 = "Arbitrary text here".toBase64()
        ),
        Attachment(
            description = "PDF file",
            contentType = "application/pdf",
            contentBase64 = testFileBytes.toBase64()
        )
    )
}

fun ByteArray.toBase64() = Base64.getEncoder().encodeToString(this)

fun String.toBase64() = Base64.getEncoder().encodeToString(this.toByteArray())
