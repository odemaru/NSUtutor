package com.nsututor

import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.application.*

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

fun Application.module() {
    install(CORS) {
        anyHost() // Разрешает запросы с любого источника. Использовать только для разработки!
        allowHeader(HttpHeaders.ContentType) // Разрешает заголовки типа Content-Type
        allowHeader(HttpHeaders.Authorization) // Если вы используете авторизацию, добавьте этот заголовок
        allowMethod(HttpMethod.Get) // Разрешает метод GET
        allowMethod(HttpMethod.Post) // Разрешает метод POST (если понадобится)
    }
    DatabaseFactory.init()
    install(ContentNegotiation) {
        json()
    }
    configureRouting()
}
