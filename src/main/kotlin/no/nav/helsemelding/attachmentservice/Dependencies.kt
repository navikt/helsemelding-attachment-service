package no.nav.helsemelding.attachmentservice

import arrow.fx.coroutines.ExitCase
import arrow.fx.coroutines.ResourceScope
import arrow.fx.coroutines.await.awaitAll
import com.google.cloud.storage.Storage
import com.google.cloud.storage.StorageOptions
import io.github.oshai.kotlinlogging.KotlinLogging
import io.micrometer.prometheus.PrometheusConfig.DEFAULT
import io.micrometer.prometheus.PrometheusMeterRegistry
import no.nav.helsemelding.attachmentservice.repository.AttachmentRepository
import no.nav.helsemelding.attachmentservice.repository.GcsAttachmentRepository

private val log = KotlinLogging.logger {}

data class Dependencies(
    val meterRegistry: PrometheusMeterRegistry,
    val attachmentRepository: AttachmentRepository
)

internal suspend fun ResourceScope.metricsRegistry(): PrometheusMeterRegistry =
    install({ PrometheusMeterRegistry(DEFAULT) }) { p, _: ExitCase ->
        p.close().also { log.info { "Closed prometheus registry" } }
    }

internal fun attachmentRepository(
    storage: Storage,
    bucketName: String
): AttachmentRepository =
    GcsAttachmentRepository(
        storage = storage,
        bucketName = bucketName
    )

suspend fun ResourceScope.dependencies(): Dependencies = awaitAll {
    val metricsRegistry = async { metricsRegistry() }

    val storage = StorageOptions.getDefaultInstance().service

    Dependencies(
        metricsRegistry.await(),
        attachmentRepository(
            storage = storage,
            bucketName = config().gcs.bucketName
        )
    )
}
