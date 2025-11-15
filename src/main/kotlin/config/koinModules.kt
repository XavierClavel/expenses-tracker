package com.xavierclavel.config

import com.xavierclavel.plugins.RedisService
import com.xavierclavel.services.AuthService
import com.xavierclavel.services.CategoryService
import com.xavierclavel.services.EncryptionService
import com.xavierclavel.services.UserService
import org.koin.dsl.module

val config = prodConfig()
val koinModules = module {
    single { UserService() }
    single { AuthService() }
    single { RedisService(getProperty("redis.url", "redis://:${config.redis.password}@${config.redis.hostname}:${config.redis.port}")) }
    single { config }
    single { EncryptionService() }
    single { AuthService() }
    single { CategoryService() }
}