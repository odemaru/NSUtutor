package com.nsututor

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.http.content.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

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
        post("/api/messages/send") {
            val params = call.receiveParameters()
            val senderId = params["senderId"]?.toIntOrNull()
                ?: return@post call.respondText("Missing senderId", status = HttpStatusCode.BadRequest)
            val receiverId = params["receiverId"]?.toIntOrNull()
                ?: return@post call.respondText("Missing receiverId", status = HttpStatusCode.BadRequest)
            val content = params["content"]
                ?: return@post call.respondText("Missing message content", status = HttpStatusCode.BadRequest)

            try {
                val message = MessengerService.sendMessage(senderId, receiverId, content)
                call.respondText(Json.encodeToString(message), contentType = ContentType.Application.Json)
            } catch (e: IllegalArgumentException) {
                call.respondText(e.message ?: "Error sending message", status = HttpStatusCode.BadRequest)
            }
        }

        // Get messages between two users
        get("/api/messages") {
            val user1Id = call.parameters["user1Id"]?.toIntOrNull()
                ?: return@get call.respondText("Missing user1Id", status = HttpStatusCode.BadRequest)
            val user2Id = call.parameters["user2Id"]?.toIntOrNull()
                ?: return@get call.respondText("Missing user2Id", status = HttpStatusCode.BadRequest)

            try {
                val messages = MessengerService.getMessagesBetweenUsers(user1Id, user2Id)
                call.respondText(Json.encodeToString(messages), contentType = ContentType.Application.Json)
            } catch (e: Exception) {
                call.respondText("Error fetching messages", status = HttpStatusCode.InternalServerError)
            }
        }

        // Mark messages as read
        post("/api/messages/read") {
            val params = call.receiveParameters()
            val receiverId = params["receiverId"]?.toIntOrNull()
                ?: return@post call.respondText("Missing receiverId", status = HttpStatusCode.BadRequest)
            val senderId = params["senderId"]?.toIntOrNull()
                ?: return@post call.respondText("Missing senderId", status = HttpStatusCode.BadRequest)

            MessengerService.markMessagesAsRead(receiverId, senderId)
            call.respondText("Messages marked as read")
        }

        // Get unread message count
        get("/api/messages/unread-count") {
            val userId = call.parameters["userId"]?.toIntOrNull()
                ?: return@get call.respondText("Missing userId", status = HttpStatusCode.BadRequest)

            val unreadCount = MessengerService.getUnreadMessageCount(userId)
            call.respondText(unreadCount.toString())
        }

        // Get recent contacts
        get("/api/messages/recent-contacts") {
            val userId = call.parameters["userId"]?.toIntOrNull()
                ?: return@get call.respondText("Missing userId", status = HttpStatusCode.BadRequest)

            val recentContacts = MessengerService.getRecentContacts(userId)
            call.respondText(Json.encodeToString(recentContacts), contentType = ContentType.Application.Json)
        }

        get("/api/init-messenger") {
            try {
//                MessengerService.createInitialConversation()
                val testUsers = MessengerService.getTestUserIds()
                call.respondText(
                    "Тестовая переписка создана. Пользователи: ${testUsers.first} и ${testUsers.second}",
                    status = HttpStatusCode.OK
                )
            } catch (e: Exception) {
                call.respondText(
                    "Ошибка при создании тестовой переписки: ${e.message}",
                    status = HttpStatusCode.InternalServerError
                )
            }
        }
    }


}
