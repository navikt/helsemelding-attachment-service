package no.nav.helsemelding.attachmentservice.model

data class Attachment(
    val messageId: String,
    val attachmentId: String,
    val fileName: String,
    val contentType: String,
    val content: ByteArray
)
