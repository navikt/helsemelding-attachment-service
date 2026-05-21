package no.nav.helsemelding.attachmentservice.plugin

import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import no.nav.helsemelding.attachmentservice.config
import no.nav.security.token.support.v3.IssuerConfig
import no.nav.security.token.support.v3.TokenSupportConfig
import no.nav.security.token.support.v3.tokenValidationSupport

fun Application.configureAuthentication() {
    install(Authentication) {
        tokenValidationSupport(
            name = config().azureAuth.issuer,
            config = TokenSupportConfig(
                IssuerConfig(
                    name = config().azureAuth.issuer,
                    discoveryUrl = config().azureAuth.discoveryUrl,
                    acceptedAudience = listOf(config().azureAuth.acceptedAudience)
                )
            )
        )
    }
}
