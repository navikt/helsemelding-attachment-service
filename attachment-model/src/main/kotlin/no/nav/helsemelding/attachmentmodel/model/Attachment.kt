package no.nav.helsemelding.attachmentmodel.model

import kotlinx.serialization.Serializable

@Serializable
data class Attachment(
    val description: String,
    val contentType: String,
    val contentBase64: String
)
