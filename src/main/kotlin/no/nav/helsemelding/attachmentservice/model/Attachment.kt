package no.nav.helsemelding.attachmentservice.model

import kotlinx.serialization.Serializable

@Serializable
data class Attachment(
    val fileName: String,
    val contentType: String,
    val content: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Attachment) return false

        if (fileName != other.fileName) return false
        if (contentType != other.contentType) return false
        if (!content.contentEquals(other.content)) return false

        return true
    }
}
