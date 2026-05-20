package no.nav.helsemelding.attachmentservice.config

import kotlin.time.Duration

data class Config(
    val server: Server,
    val gcs: Gcs,
    val azureAuth: AzureAuth
)

data class Server(
    val port: Port,
    val preWait: Duration
)

@JvmInline
value class Port(val value: Int)

data class Gcs(
    val bucketName: String
)

data class AzureAuth(
    val issuer: String,
    val azureWellKnownUrl: String,
    val acceptedAudience: String
)
