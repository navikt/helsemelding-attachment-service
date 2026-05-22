package no.nav.helsemelding.attachmentservice.plugin

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
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
import no.nav.helsemelding.attachmentservice.repository.AttachmentRepository
import no.nav.security.token.support.v3.TokenValidationContextPrincipal
import kotlin.uuid.Uuid

val log = KotlinLogging.logger {}

fun Application.configureRoutes(
    registry: PrometheusMeterRegistry,
    attachmentRepository: AttachmentRepository
) {
    routing {
        internalRoutes(registry)

        authenticate(config().azureAuth.issuer) {
            externalRoutes(attachmentRepository)
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

fun Route.externalRoutes(attachmentRepository: AttachmentRepository) {
    post("/attachments/{messageId}") {
        // TODO: Debug logging, remove before merge
        val principal = call.principal<TokenValidationContextPrincipal>()

        if (principal == null) {
            log.warn { "No TokenValidationContextPrincipal found" }
        } else {
            val context = principal.context
            val issuer = config().azureAuth.issuer
            val claims = context.getClaims(issuer)

            log.info { "Token issuer: $issuer" }
            log.info { "azp: ${claims.getStringClaim("azp")}" }
            log.info { "appid: ${claims.getStringClaim("appid")}" }
            log.info { "client_id: ${claims.getStringClaim("client_id")}" }
            log.info { "roles: ${claims.getAsList("roles")}" }
            log.info { "scp: ${claims.getStringClaim("scp")}" }
        }

        val messageId = call.massageId() ?: return@post
        val attachments = call.attachments() ?: return@post

        try {
            attachmentRepository.save(messageId, attachments)

            call.respond(HttpStatusCode.OK)
        } catch (e: Exception) {
            val errorMessage = "Error saving attachments for message $messageId: ${e.message}"
            log.error(e) { errorMessage }
            call.respond(HttpStatusCode.InternalServerError, errorMessage)
        }
    }

    get("/attachments/{messageId}") {
        val messageId = call.massageId() ?: return@get

        try {
            val attachments = attachmentRepository.read(messageId)

            if (attachments.isEmpty()) {
                call.respond(HttpStatusCode.NotFound)
            } else {
                call.respond(HttpStatusCode.OK, attachments)
            }
        } catch (e: Exception) {
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
