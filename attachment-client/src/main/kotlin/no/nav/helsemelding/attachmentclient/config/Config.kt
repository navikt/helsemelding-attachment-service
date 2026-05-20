package no.nav.helsemelding.attachmentclient.config

import java.net.URI
import kotlin.time.Duration

data class Config(
    val httpClient: HttpClient,
    val httpTokenClient: HttpClient,
    val attachmentService: AttachmentService,
    val azureAuth: AzureAuth
)

data class HttpClient(
    val connectionTimeout: Duration
)

data class AttachmentService(
    val url: URI,
    val scope: String
)

data class AzureAuth(
    val grantType: String,
    val tokenEndpoint: URI,
    val appClientId: String,
    val appClientSecret: String
)
