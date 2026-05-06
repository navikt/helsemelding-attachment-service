package no.nav.helsemelding.attachmentservice.plugin

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
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
import no.nav.helsemelding.attachmentservice.model.Attachment
import no.nav.helsemelding.attachmentservice.repository.AttachmentRepository
import kotlin.uuid.Uuid

fun Application.configureRoutes(
    registry: PrometheusMeterRegistry,
    attachmentRepository: AttachmentRepository
) {
    routing {
        internalRoutes(registry)
        externalRoutes(attachmentRepository)
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
        val messageId = call.massageId()
        val attachments = call.attachments()

        attachmentRepository.save(messageId, attachments)

        call.respond(HttpStatusCode.OK)
    }

    get("/attachments/{messageId}") {
        val messageId = call.massageId()

        val attachments = attachmentRepository.read(messageId)

        call.respond(HttpStatusCode.OK, attachments)
    }
}

private suspend fun RoutingCall.massageId(): Uuid {
    val messageIdParam = this.parameters["messageId"]

    if (messageIdParam.isNullOrEmpty()) {
        this.respond(HttpStatusCode.BadRequest, "Missing messageId")
        throw IllegalArgumentException("Missing messageId")
    }

    return try {
        Uuid.parse(messageIdParam)
    } catch (e: IllegalArgumentException) {
        this.respond(HttpStatusCode.BadRequest, "messageId must be a valid UUID")
        throw e
    }
}

private suspend fun RoutingCall.attachments(): List<Attachment> = try {
    this.receive<List<Attachment>>()
} catch (e: Exception) {
    this.respond(HttpStatusCode.BadRequest, "Invalid attachment format: ${e.message}")
    throw e
}
