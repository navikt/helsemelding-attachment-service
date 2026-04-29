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
                val content = "hello from deployed app".toByteArray()

                attachmentRepository.save(
                    messageId = "test-message",
                    fileName = "test.txt",
                    contentType = "text/plain",
                    content = content
                )
            } catch (e: Exception) {
                call.respondText("Error saving attachment: ${e.message}")
                return@post
            }

            call.respondText("OK")
        }

        get("/test") {
            try {
                val content = attachmentRepository.read(
                    messageId = "test-message",
                    fileName = "test.txt"
                )

                if (content == null) {
                    call.respond(HttpStatusCode.NotFound)
                } else {
                    call.respondText(content.toString(Charsets.UTF_8))
                }
            } catch (e: Exception) {
                call.respondText("Error reading attachment: ${e.message}")
            }
        }
    }
}
