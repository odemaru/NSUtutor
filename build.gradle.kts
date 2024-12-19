val kotlin_version: String by project
val logback_version: String by project

plugins {
    kotlin("jvm") version "2.0.21"
    id("io.ktor.plugin") version "3.0.1"
    kotlin("plugin.serialization") version "1.9.0"
}

group = "com.nsututor"
version = "0.0.1"

application {
    mainClass.set("io.ktor.server.netty.EngineMain")

    val isDevelopment: Boolean = project.ext.has("development")
    applicationDefaultJvmArgs = listOf("-Dio.ktor.development=$isDevelopment")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("io.ktor:ktor-server-core-jvm")
    implementation("io.ktor:ktor-server-netty-jvm")
    implementation("io.ktor:ktor-server-content-negotiation:2.x.x")
    implementation("ch.qos.logback:logback-classic:$logback_version")
    implementation("io.ktor:ktor-server-config-yaml-jvm")
    testImplementation("io.ktor:ktor-server-test-host-jvm")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:$kotlin_version")
    implementation("org.jetbrains.exposed:exposed-core:0.41.1")
    implementation("org.jetbrains.exposed:exposed-dao:0.41.1")
    implementation("org.jetbrains.exposed:exposed-jdbc:0.41.1")
    implementation("org.postgresql:postgresql:42.7.2")
    implementation("org.mindrot:jbcrypt:0.4")
    implementation("org.jetbrains.exposed:exposed-java-time:0.39.2")
    implementation("org.postgresql:postgresql:42.7.2")
    implementation("io.ktor:ktor-server-core:2.2.3")
    implementation("io.ktor:ktor-serialization-kotlinx-json:2.2.3") // для работы с JSON
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.1") // зависимость для сериализации
    implementation("io.ktor:ktor-server-cors-jvm:2.3.4")
//    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.x.x")
    implementation("io.ktor:ktor-server-sessions:2.3.4") // Для работы с сессиями
    implementation("io.ktor:ktor-server-cors:2.3.4") // CORS для междоменных запросов
    implementation("io.ktor:ktor-server-call-logging:2.3.4") // Логирование запросов
}
