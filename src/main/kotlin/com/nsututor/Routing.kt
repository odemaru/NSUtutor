package com.nsututor

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.http.content.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.configureRouting() {
    routing {
        // Перенаправление на main.html
        get("/") {
            call.respondRedirect("/main.html")
        }

        // Статические файлы
        staticResources("/", "static")

        // Регистрация пользователя
        post("/register") {
            val params = call.receiveParameters()
            val username = params["username"] ?: return@post call.respondText(
                "Missing username",
                status = HttpStatusCode.BadRequest
            )
            val password = params["password"] ?: return@post call.respondText(
                "Missing password",
                status = HttpStatusCode.BadRequest
            )
            val role = params["role"] ?: throw IllegalArgumentException("Role is required")

            try {
                User.register(username, password, role)
                call.respondText("User registered successfully")
            } catch (e: IllegalArgumentException) {
                call.respondText(e.message ?: "Error occurred", status = HttpStatusCode.BadRequest)
            }
        }

        // Вход пользователя
        post("/login") {
            val params = call.receiveParameters()
            val username = params["username"] ?: return@post call.respondText(
                "Missing username",
                status = HttpStatusCode.BadRequest
            )
            val password = params["password"] ?: return@post call.respondText(
                "Missing password",
                status = HttpStatusCode.BadRequest
            )

            val isLoggedIn = User.login(username, password)

            if (isLoggedIn) {
                call.respondText("Login successful")
            } else {
                call.respondText("Invalid username or password", status = HttpStatusCode.Unauthorized)
            }
        }

        // Новый маршрут для получения всех объявлений
        get("/api/ads") {
            try {
                val ads = AdService.getAllAdsAsJson()
                call.respond(ads) // Возвращаем JSON-ответ
                println("$ads")
            } catch (e: Exception) {
                log.error("Failed to fetch ads", e) // Логируем ошибку
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to "Failed to fetch ads: ${e.message}") // Клиенту отправляем JSON с ошибкой
                )
            }
        }
    }
}
