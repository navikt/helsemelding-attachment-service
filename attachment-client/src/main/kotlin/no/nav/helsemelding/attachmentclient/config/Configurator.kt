package no.nav.helsemelding.attachmentclient.config

import arrow.core.memoize
import com.sksamuel.hoplite.ConfigLoader
import com.sksamuel.hoplite.addResourceSource

val config: () -> Config = {
    ConfigLoader.builder()
        .addResourceSource("/attachment-client-personal.conf", optional = true)
        .addResourceSource("/attachment-client.conf")
        .build()
        .loadConfigOrThrow<Config>()
}.memoize()
