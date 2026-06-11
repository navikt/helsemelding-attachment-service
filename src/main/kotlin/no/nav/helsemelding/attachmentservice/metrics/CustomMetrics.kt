package no.nav.helsemelding.attachmentservice.metrics

import io.github.oshai.kotlinlogging.KotlinLogging
import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.MeterRegistry

val log = KotlinLogging.logger {}

interface Metrics {
    fun registerAttachmentSaving(result: AttachmentSavingResultTag)
}

class CustomMetrics(
    private val registry: MeterRegistry
) : Metrics {

    override fun registerAttachmentSaving(result: AttachmentSavingResultTag) {
        Counter.builder("helsemelding_attachments_saving")
            .description("Number of attachment save requests")
            .tag("result", result.value)
            .register(registry)
            .increment()
    }
}

class FakeMetrics() : Metrics {
    override fun registerAttachmentSaving(result: AttachmentSavingResultTag) {
        log.info { "helsemelding_attachments_saving metric is registered with saving result: $result" }
    }
}
