package no.nav.helsemelding.attachmentclient.config

import java.net.URI
import kotlin.time.Duration

data class Config(
    val httpClient: HttpClient,
    val attachmentService: AttachmentService
)

data class HttpClient(
    val connectionTimeout: Duration
)

data class AttachmentService(
    val url: URI
)
