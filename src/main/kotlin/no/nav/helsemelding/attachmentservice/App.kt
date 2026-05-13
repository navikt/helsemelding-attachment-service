package no.nav.helsemelding.attachmentservice

import arrow.continuations.SuspendApp
import arrow.continuations.ktor.server
import arrow.core.raise.result
import arrow.fx.coroutines.resourceScope
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.server.application.Application
import io.ktor.server.netty.Netty
import io.ktor.utils.io.CancellationException
import io.micrometer.prometheus.PrometheusMeterRegistry
import kotlinx.coroutines.awaitCancellation
import no.nav.helsemelding.attachmentservice.plugin.configureMetrics
import no.nav.helsemelding.attachmentservice.plugin.configureRoutes
import no.nav.helsemelding.attachmentservice.plugin.installContentNegotiation
import no.nav.helsemelding.attachmentservice.repository.AttachmentRepository

private val log = KotlinLogging.logger {}

fun main() = SuspendApp {
    result {
        resourceScope {
            val deps = dependencies()

            server(
                Netty,
                port = config().server.port.value,
                preWait = config().server.preWait,
                module = attachmentServiceModule(deps.meterRegistry, deps.attachmentRepository)
            )

            awaitCancellation()
        }
    }
        .onFailure { error -> if (error !is CancellationException) logError(error) }
}

internal fun attachmentServiceModule(
    meterRegistry: PrometheusMeterRegistry,
    attachmentRepository: AttachmentRepository
): Application.() -> Unit {
    return {
        installContentNegotiation()
        configureMetrics(meterRegistry)
        configureRoutes(meterRegistry, attachmentRepository)
    }
}

private fun logError(t: Throwable) = log.error { "Shutdown Attachment Servicer due to: ${t.stackTraceToString()}" }
