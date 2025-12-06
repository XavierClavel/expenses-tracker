package com.xavierclavel.config

import com.sksamuel.hoplite.ConfigLoaderBuilder
import com.sksamuel.hoplite.addResourceSource
import com.sksamuel.hoplite.sources.EnvironmentVariablesPropertySource
import com.sksamuel.hoplite.sources.MapPropertySource


data class Configuration(
    val postgres: Postgres,
    val redis: Redis,
    val oauth: OAuth,
    val admin: Admin,
    val aes: Aes,
) {
    data class Postgres(
        val user: String,
        val password: String,
        val jdbc: Jdbc
    ) {
        data class Jdbc(
            val url: String,
        )
    }

    data class OAuth(
        val client: Client,
        val provider: UrlWrapper,
        val redirect: UrlWrapper,
    ) {
        data class Client(
            val id: String,
            val secret: String,
        )

    }

    data class UrlWrapper(
        val url: String,
    )

    data class Redis(
        val password: String,
        val hostname: String,
        val port: Int,
    )

    data class Admin(
        val password: String,
    )

    data class Aes(
        val key: String,
    )

}



val testConfig: Configuration by lazy {
    ConfigLoaderBuilder.default()
        .addResourceSource("/configuration.yaml", false)
        .build()
        .loadConfigOrThrow<Configuration>()
}

fun prodConfig(): Configuration {
    return ConfigLoaderBuilder.default()
        .addSource(
            customEnvSource()
        )
        .addResourceSource("/configuration.yaml", true)
        .build()
        .loadConfigOrThrow<Configuration>()
}

/**
 * Loads config from env variables with single underscore
 */
fun customEnvSource(): MapPropertySource {
    val transformed: Map<String, String> = System.getenv().entries
        .associate { (key, value) ->
            val hopliteKey = key
                .replace("__", ".")
                .replace("_", ".")
                .lowercase()

            hopliteKey to value
        }

    return MapPropertySource(transformed)
}
