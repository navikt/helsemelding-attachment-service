package no.nav.helsemelding.attachmentservice.plugin

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import io.micrometer.prometheus.PrometheusMeterRegistry
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import no.nav.helsemelding.attachmentservice.model.Attachment
import no.nav.helsemelding.attachmentservice.repository.AttachmentRepository

fun Application.configureRoutes(
    registry: PrometheusMeterRegistry,
    attachmentRepository: AttachmentRepository
) {
    routing { internalRoutes(registry, attachmentRepository) }
}

fun Route.internalRoutes(registry: PrometheusMeterRegistry, attachmentRepository: AttachmentRepository) {
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

    // TODO: For testing purpose only. Remove before merge
    route("/attachments") {
        post("/test") {
            try {
                val messageId = "8ccc4474-47c7-11f1-83a5-325096b39f47"
                val testAttachments = listOf(
                    Attachment(
                        fileName = "attachment.txt",
                        contentType = "text/plain",
                        content = "Arbitrary text here".toByteArray()
                    ),
                    Attachment(
                        fileName = "attachment2.txt",
                        contentType = "text/plain",
                        content = "More arbitrary text here".toByteArray()
                    )
                )

                attachmentRepository.save(messageId, testAttachments)
            } catch (e: Exception) {
                call.respondText("Error saving attachment: ${e.message}")
                return@post
            }

            call.respondText("OK")
        }

        get("/test") {
            try {
                val messageId = "8ccc4474-47c7-11f1-83a5-325096b39f47"
                val content = attachmentRepository.read(messageId)

                if (content.isEmpty()) {
                    call.respond(HttpStatusCode.NotFound)
                } else {
                    call.respondText(Json.encodeToString(content))
                }
            } catch (e: Exception) {
                call.respondText("Error reading attachment: ${e.message}")
            }
        }
    }
}
