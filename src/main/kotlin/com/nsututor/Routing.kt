package com.nsututor

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
            val username = params["username"] ?: return@post call.respondText("Missing username", status = io.ktor.http.HttpStatusCode.BadRequest)
            val password = params["password"] ?: return@post call.respondText("Missing password", status = io.ktor.http.HttpStatusCode.BadRequest)

            try {
                User.register(username, password)
                call.respondText("User registered successfully")
            } catch (e: IllegalArgumentException) {
                call.respondText(e.message ?: "Error occurred", status = io.ktor.http.HttpStatusCode.BadRequest)
            }
        }

        // Вход пользователя
        post("/login") {
            val params = call.receiveParameters()
            val username = params["username"] ?: return@post call.respondText("Missing username", status = io.ktor.http.HttpStatusCode.BadRequest)
            val password = params["password"] ?: return@post call.respondText("Missing password", status = io.ktor.http.HttpStatusCode.BadRequest)

            val isLoggedIn = User.login(username, password)

            if (isLoggedIn) {
                call.respondText("Login successful")
            } else {
                call.respondText("Invalid username or password", status = io.ktor.http.HttpStatusCode.Unauthorized)
            }
        }
    }
}
