package com.xavierclavel.config

import com.sksamuel.hoplite.ConfigAlias
import com.sksamuel.hoplite.ConfigLoaderBuilder
import com.sksamuel.hoplite.addResourceSource
import com.sksamuel.hoplite.sources.EnvironmentVariablesPropertySource


data class Configuration(
    val postgres: Postgres,
    val redis: Redis,
    val oauth: OAuth,
    val admin: Admin,
    val aes: Aes,
) {
    data class Postgres(
        @ConfigAlias("POSTGRES_USER")
        val user: String,

        @ConfigAlias("POSTGRES_PASSWORD")
        val password: String,

        val jdbc: Jdbc
    ) {
        data class Jdbc(

            @ConfigAlias("POSTGRES_JDBC_URL")
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

        @ConfigAlias("REDIS_PASSWORD")
        val password: String,

        @ConfigAlias("REDIS_HOSTNAME")
        val hostname: String,

        @ConfigAlias("REDIS_PORT")
        val port: Int,
    )

    data class Admin(
        @ConfigAlias("ADMIN_PASSWORD")
        val password: String,
    )

    data class Aes(
        @ConfigAlias("AES_KEY")
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
            EnvironmentVariablesPropertySource(
                useUnderscoresAsSeparator = true,
                allowUppercaseNames = true,
            )
        )
        .addResourceSource("/configuration.yaml", true)
        .build()
        .loadConfigOrThrow<Configuration>()
}