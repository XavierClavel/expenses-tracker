package com.xavierclavel.plugins

import com.xavierclavel.config.Configuration
import com.xavierclavel.models.query.QCategory
import com.xavierclavel.models.query.QUser
import com.xavierclavel.utils.logger
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ebean.Database
import io.ebean.DatabaseFactory
import io.ebean.config.DatabaseConfig
import io.ebean.migration.MigrationConfig
import io.ebean.migration.MigrationRunner
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.koin.java.KoinJavaComponent.inject


object DatabaseManager {
    var mainDB : Database? = null
    private val dataSource: HikariDataSource by lazy { initConfig() }
    val configuration by inject<Configuration>(Configuration::class.java)

    fun getTables() = listOf(
        QUser(),
        QCategory(),
    )

    private fun initConfig(): HikariDataSource {
        val config = HikariConfig()
        config.driverClassName = "org.postgresql.Driver"
        config.jdbcUrl = configuration.postgres.jdbc.url
        config.username = configuration.postgres.user
        config.password = configuration.postgres.password
        return HikariDataSource(config)
    }

    fun init() {
        logger.info("Initializing database...")
        while (mainDB == null) {
            try {
                mainDB = DatabaseFactory.create(DatabaseConfig().apply {
                    it.dataSource(dataSource)
                })
            }catch (e:Exception) {
                logger.error { e.message }
                logger.error { "Failed to connect to database at ${dataSource.jdbcUrl} with user ${dataSource.username}, retrying in 5 seconds" }
                runBlocking { (delay(5000)) }
            }
        }
        logger.info { "Successfully connected to database at ${dataSource.jdbcUrl}" }

        // Trigger migration runner manually
        val config = MigrationConfig()
        val migrationRunner = MigrationRunner(config)
        migrationRunner.run(dataSource)
        logger.info("Database migrations applied.")
    }
}