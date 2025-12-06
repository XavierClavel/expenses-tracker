import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val koin_version: String by project
val kotlin_version: String by project
val logback_version: String by project
val ktor_version: String by project
val ebean_version: String by project
val testcontainers_version: String by project

plugins {
    kotlin("jvm") version "2.2.21"
    kotlin("kapt") version "2.2.21"
    kotlin("plugin.serialization") version "2.2.21"
    id("io.ktor.plugin") version "3.3.2"
    id("io.ebean") version "17.0.1"
}

application {
    mainClass = "com.xavierclavel.ApplicationKt"
}

dependencies {
    val hopliteVersion = "2.9.0"

    //Ktor server
    implementation("io.ktor:ktor-server-core:$ktor_version")
    implementation("io.ktor:ktor-server-openapi:$ktor_version")
    implementation("io.ktor:ktor-server-host-common:$ktor_version")
    implementation("io.ktor:ktor-server-status-pages:$ktor_version")
    implementation("io.ktor:ktor-server-call-logging:$ktor_version")
    implementation("io.ktor:ktor-server-netty:${ktor_version}")
    implementation("io.ktor:ktor-server-config-yaml:${ktor_version}")
    implementation("io.ktor:ktor-server-content-negotiation:${ktor_version}")
    implementation("io.ktor:ktor-server-sessions:${ktor_version}")
    implementation("io.ktor:ktor-server-auth:${ktor_version}")
    implementation("io.ktor:ktor-serialization-kotlinx-json:${ktor_version}")
    testImplementation("io.ktor:ktor-server-test-host:${ktor_version}")

    //Ktor client
    implementation("io.ktor:ktor-client-core:${ktor_version}")
    implementation("io.ktor:ktor-client-cio:${ktor_version}")
    implementation("io.ktor:ktor-client-content-negotiation:${ktor_version}")

    implementation("dev.hayden:khealth:3.0.2")

    implementation("io.insert-koin:koin-ktor:$koin_version")
    implementation("io.insert-koin:koin-logger-slf4j:$koin_version")
    implementation("io.insert-koin:koin-test-jvm:${koin_version}")


    //Logging
    implementation("io.github.microutils:kotlin-logging-jvm:3.0.5")
    implementation("ch.qos.logback:logback-classic:$logback_version")

    //Tests
    testImplementation(kotlin("test"))
    testImplementation("org.testcontainers:junit-jupiter:$testcontainers_version")
    testImplementation("org.testcontainers:testcontainers:${testcontainers_version}")
    testImplementation("org.testcontainers:postgresql:${testcontainers_version}")

    //DB
    implementation("org.postgresql:postgresql:42.7.4")
    implementation("com.zaxxer:HikariCP:6.0.0")
    implementation("org.flywaydb:flyway-core:10.20.0")
    implementation("io.ebean:ebean:$ebean_version")
    implementation("io.ebean:ebean-platform-postgres:$ebean_version")
    implementation("io.ebean:ebean-ddl-generator:$ebean_version")
    implementation("io.ebean:ebean-migration:14.2.0")
    testImplementation("io.ebean:ebean-test:$ebean_version")
    testImplementation("io.ebean:ebean:$ebean_version")
    kapt("io.ebean:querybean-generator:$ebean_version")

    //Redis for session storage
    implementation("io.lettuce:lettuce-core:7.0.0.RELEASE")

    //Encryption -> bcrypt
    implementation("at.favre.lib:bcrypt:0.10.2")

    //Configuration
    implementation("com.sksamuel.hoplite:hoplite-core:${hopliteVersion}")
    implementation("com.sksamuel.hoplite:hoplite-yaml:${hopliteVersion}")

    //Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactive:1.10.1")

    implementation("org.hibernate:hibernate-core:6.6.1.Final")

}


tasks.test {
    useJUnitPlatform()
}