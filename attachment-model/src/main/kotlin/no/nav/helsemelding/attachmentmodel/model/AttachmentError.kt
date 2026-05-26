package no.nav.helsemelding.attachmentmodel.model

/**
 * Exception thrown when an attachment operation fails.
 *
 * It represents an error response returned by the Attachment Service.
 *
 * @property code HTTP status code associated with the failure.
 * @param message Human-readable error message describing the failure.
 */
class AttachmentError(
    val code: Int,
    message: String
) : RuntimeException(message)
