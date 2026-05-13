package no.nav.helsemelding.attachmentclient

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import no.nav.helsemelding.attachmentclient.config.config

fun httpClient(): () -> HttpClient = {
    HttpClient(CIO) {
        install(HttpTimeout) {
            connectTimeoutMillis = config().httpClient.connectionTimeout.inWholeMilliseconds
        }

        install(ContentNegotiation) {
            json(
                Json {
                    ignoreUnknownKeys = true
                    encodeDefaults = true
                }
            )
        }
    }
}
