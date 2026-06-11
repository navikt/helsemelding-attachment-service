package no.nav.helsemelding.attachmentservice.plugin

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.RoutingCall
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import io.micrometer.prometheus.PrometheusMeterRegistry
import no.nav.helsemelding.attachmentmodel.model.Attachment
import no.nav.helsemelding.attachmentservice.config
import no.nav.helsemelding.attachmentservice.metrics.AttachmentReadingResultTag
import no.nav.helsemelding.attachmentservice.metrics.AttachmentSavingResultTag
import no.nav.helsemelding.attachmentservice.metrics.Metrics
import no.nav.helsemelding.attachmentservice.repository.AttachmentRepository
import no.nav.security.token.support.v3.TokenValidationContextPrincipal
import kotlin.uuid.Uuid

val log = KotlinLogging.logger {}

fun Application.configureRoutes(
    registry: PrometheusMeterRegistry,
    attachmentRepository: AttachmentRepository,
    metrics: Metrics
) {
    routing {
        internalRoutes(registry)

        authenticate(config().azureAuth.issuer) {
            externalRoutes(attachmentRepository, metrics)
        }
    }
}

fun Route.internalRoutes(registry: PrometheusMeterRegistry) {
    get("/prometheus") {
        call.respond(registry.scrape())
    }
    route("/internal") {
        get("/health/liveness") {
            call.respondText("I'm alive! :)")
        }
        get("/health/readiness") {
            call.respondText("I'm ready! :)")
        }
    }
}

fun Route.externalRoutes(
    attachmentRepository: AttachmentRepository,
    metrics: Metrics
) {
    post("/attachments/{messageId}") {
        if (!call.requireWriteAccess()) {
            metrics.registerAttachmentSaving(AttachmentSavingResultTag.FORBIDDEN)
            return@post
        }

        val messageId = call.massageId()
        if (messageId == null) {
            metrics.registerAttachmentSaving(AttachmentSavingResultTag.BAD_REQUEST)
            return@post
        }

        val attachments = call.attachments()
        if (attachments == null) {
            metrics.registerAttachmentSaving(AttachmentSavingResultTag.BAD_REQUEST)
            return@post
        }

        try {
            attachmentRepository.save(messageId, attachments)

            metrics.registerAttachmentSaving(AttachmentSavingResultTag.SUCCESS)
            call.respond(HttpStatusCode.Created)
        } catch (e: Exception) {
            metrics.registerAttachmentSaving(AttachmentSavingResultTag.FAILED)

            val errorMessage = "Error saving attachments for message $messageId: ${e.message}"
            log.error(e) { errorMessage }
            call.respond(HttpStatusCode.InternalServerError, errorMessage)
        }
    }

    get("/attachments/{messageId}") {
        val messageId = call.massageId()
        if (messageId == null) {
            metrics.registerAttachmentReading(AttachmentReadingResultTag.BAD_REQUEST)
            return@get
        }

        try {
            val attachments = attachmentRepository.read(messageId)

            if (attachments.isEmpty()) {
                metrics.registerAttachmentReading(AttachmentReadingResultTag.NOT_FOUND)
                call.respond(HttpStatusCode.NotFound)
            } else {
                metrics.registerAttachmentReading(AttachmentReadingResultTag.SUCCESS)
                call.respond(HttpStatusCode.OK, attachments)
            }
        } catch (e: Exception) {
            metrics.registerAttachmentReading(AttachmentReadingResultTag.FAILED)

            val errorMessage = "Error reading attachments for message $messageId: ${e.message}"
            log.error(e) { errorMessage }
            call.respond(HttpStatusCode.InternalServerError, errorMessage)
        }
    }
}

private suspend fun RoutingCall.massageId(): Uuid? {
    val messageIdParam = this.parameters["messageId"]!!
    val messageId = Uuid.parseOrNull(messageIdParam)

    if (messageId == null) {
        val errorMessage = "messageId must be a valid UUID"
        log.warn { errorMessage }
        this.respond(HttpStatusCode.BadRequest, errorMessage)
    }

    return messageId
}

private suspend fun RoutingCall.attachments(): List<Attachment>? = try {
    this.receive<List<Attachment>>()
} catch (e: Exception) {
    val errorMessage = "Invalid attachment format: ${e.message}"
    log.warn { errorMessage }
    this.respond(HttpStatusCode.BadRequest, errorMessage)
    null
}

private suspend fun ApplicationCall.requireWriteAccess(): Boolean {
    val principal = principal<TokenValidationContextPrincipal>()

    val claims = principal
        ?.context
        ?.getClaims("AZURE_AD")

    val clientId = claims?.getStringClaim("azp")

    if (clientId !in config().security.clientsWithWriteAccess) {
        log.warn { "Client $clientId is not allowed to save attachments" }
        respond(HttpStatusCode.Forbidden)
        return false
    }

    return true
}
