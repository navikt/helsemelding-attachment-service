package no.nav.helsemelding.attachmentservice.metrics

import io.github.oshai.kotlinlogging.KotlinLogging
import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.DistributionSummary
import io.micrometer.core.instrument.MeterRegistry

val log = KotlinLogging.logger {}

interface Metrics {
    fun registerAttachmentSaving(result: AttachmentSavingResultTag)
    fun registerAttachmentReading(result: AttachmentReadingResultTag)
    fun registerAttachmentSize(bytes: Double)
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

    override fun registerAttachmentReading(result: AttachmentReadingResultTag) {
        Counter.builder("helsemelding_attachments_reading")
            .description("Number of attachment read requests")
            .tag("result", result.value)
            .register(registry)
            .increment()
    }

    override fun registerAttachmentSize(bytes: Double) {
        DistributionSummary.builder("helsemelding_attachment_size_bytes")
            .description("Size of each successfully saved attachment in bytes")
            .baseUnit("bytes")
            .publishPercentileHistogram()
            .register(registry)
            .record(bytes)
    }
}

class FakeMetrics() : Metrics {
    override fun registerAttachmentSaving(result: AttachmentSavingResultTag) {
        log.info { "helsemelding_attachments_saving metric is registered with saving result: $result" }
    }

    override fun registerAttachmentReading(result: AttachmentReadingResultTag) {
        log.info { "helsemelding_attachments_reading metric is registered with reading result: $result" }
    }

    override fun registerAttachmentSize(bytes: Double) {
        log.info { "helsemelding_attachment_size_bytes metric is registered with size in bytes: $bytes" }
    }
}
