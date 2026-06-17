package no.nav.helsemelding.attachmentservice.metrics

enum class AttachmentReadingResultTag(val value: String) {
    SUCCESS("success"),
    NOT_FOUND("not_found"),
    BAD_REQUEST("bad_request"),
    FAILED("failed")
}
