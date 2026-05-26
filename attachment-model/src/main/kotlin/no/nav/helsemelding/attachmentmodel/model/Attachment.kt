package no.nav.helsemelding.attachmentmodel.model

import kotlinx.serialization.Serializable

/**
 * Represents a file attachment associated with a message.
 *
 * @property description Human-readable description or filename of the attachment.
 * @property contentType MIME type of the attachment content
 * (for example `application/pdf` or `image/png`).
 * @property contentBase64 Base64-encoded binary content of the attachment.
 */
@Serializable
data class Attachment(
    val description: String,
    val contentType: String,
    val contentBase64: String
)
