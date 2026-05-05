package no.nav.helsemelding.attachmentservice.model

import kotlin.uuid.Uuid

data class Attachment(
    val messageId: Uuid,
    val attachmentId: String,
    val fileName: String,
    val contentType: String,
    val content: ByteArray
)
