package main.com.xavierclavel.containers

import org.testcontainers.containers.GenericContainer
import kotlin.apply

object RedisTestContainer {

    val redis = GenericContainer("redis:alpine").apply {
        withExposedPorts(6379)
        start()
    }

    fun getRedisUri(): String {
        return "redis://${redis.host}:${redis.firstMappedPort}"
    }
}
