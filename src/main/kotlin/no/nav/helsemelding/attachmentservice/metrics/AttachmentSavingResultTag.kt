package no.nav.helsemelding.attachmentservice.metrics

enum class AttachmentSavingResultTag(val value: String) {
    SUCCESS("success"),
    FORBIDDEN("forbidden"),
    BAD_REQUEST("bad_request"),
    FAILED("failed")
}
