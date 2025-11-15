package com.xavierclavel

import com.xavierclavel.config.Configuration
import com.xavierclavel.config.testConfig
import com.xavierclavel.dtos.SignupDto
import com.xavierclavel.plugins.DatabaseManager
import com.xavierclavel.plugins.RedisService
import com.xavierclavel.services.AuthService
import com.xavierclavel.services.CategoryService
import com.xavierclavel.services.EncryptionService
import com.xavierclavel.services.UserService
import com.xavierclavel.utils.login
import com.xavierclavel.utils.logout
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.cookies.HttpCookies
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.Plugin
import io.ktor.server.testing.*
import io.ktor.utils.io.KtorDsl
import kotlinx.serialization.json.Json
import main.com.xavierclavel.containers.RedisTestContainer
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.koin.core.context.GlobalContext.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.test.KoinTest
import org.koin.test.inject
import kotlin.coroutines.EmptyCoroutineContext

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
abstract class ApplicationTest: KoinTest {
    val userService by inject<UserService>()
    val configuration by inject<Configuration>()

    val adminPassword by lazy { configuration.admin.password }
    val user1 = SignupDto("user1", "user1@mail.com", "Passw0rd")
    val user2 = SignupDto("user2", "user2@mail.com", "Passw0rd")

    companion object {

        @BeforeAll
        @JvmStatic
        fun startKoin() {
            val testModules = module {
                single { UserService() }
                single { RedisService(getProperty("redis.url")) }
                single { AuthService() }
                single { testConfig }
                single { EncryptionService() }
                single { CategoryService() }
            }

            startKoin {
                modules(testModules)
                properties(mapOf("redis.url" to RedisTestContainer.getRedisUri()))
            }
        }

        @AfterAll
        @JvmStatic
        fun stopKoinApplication() {
            stopKoin()
        }

    }

    fun cleanDb() {
        DatabaseManager.getTables().forEach { table ->
            table.delete()
        }
        userService.setupDefaultAdmin()
        userService.create(user1)
        userService.create(user2)
    }

    fun runTest(block: suspend TestBuilderWrapper.() -> Unit) {
        return testApplication(EmptyCoroutineContext) {
            application {
                module()
            }
            cleanDb()
            val wrapper = TestBuilderWrapper(this)
            wrapper.block() // Use the wrapper in the block
        }
    }

    @KtorDsl
    fun runTestAsAdmin(block: suspend TestBuilderWrapper.() -> Unit) = runTest {
        runAsAdmin {
            this.block()
        }
    }

    @KtorDsl
    fun runTestAsUser(block: suspend TestBuilderWrapper.() -> Unit) = runTest {
        runAsUser1 {
            this.block()
        }
    }

    suspend fun TestBuilderWrapper.runAs(username: String, password: String = "Passw0rd", block: suspend TestBuilderWrapper.() -> Unit) {
        client.login(username, password)
        this.block()
        client.logout()
    }

    suspend fun TestBuilderWrapper.runAsAdmin(block: suspend TestBuilderWrapper.() -> Unit) = runAs("admin@mail.com", adminPassword) {
        this.block()
    }

    suspend fun TestBuilderWrapper.runAsUser1(block: suspend TestBuilderWrapper.() -> Unit) = runAs("user1@mail.com", "Passw0rd") {
        this.block()
    }

    suspend fun TestBuilderWrapper.runAsUser2(block: suspend TestBuilderWrapper.() -> Unit) = runAs("user2@mail.com", "Passw0rd") {
        this.block()
    }



}


class TestBuilderWrapper(private val builder: ApplicationTestBuilder) {
    val client: HttpClient by lazy {
        builder.client.config {
            install(HttpCookies)
            install(ContentNegotiation) {
                json(Json {
                    prettyPrint = true
                    isLenient = true
                })
            }
        }
    }

    // Delegate install with proper types
    fun <P : Any, B : Any, F : Any> install(plugin: Plugin<Application, B, F>, configure: B.() -> Unit = {}) {
        builder.install(plugin, configure)
    }

    fun application(block: Application.() -> Unit) = builder.application(block)
}
