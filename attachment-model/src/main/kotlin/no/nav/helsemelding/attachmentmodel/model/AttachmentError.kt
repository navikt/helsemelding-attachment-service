package no.nav.helsemelding.attachmentmodel.model

class AttachmentError(
    val code: Int,
    message: String
) : RuntimeException(message)
