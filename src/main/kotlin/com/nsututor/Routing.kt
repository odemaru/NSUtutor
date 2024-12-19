package com.nsututor

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.http.content.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.*

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

        post("/api/change-password") {
            val params = call.receiveParameters()
            val oldPassword = params["oldPassword"] ?: return@post call.respondText(
                "Missing oldPassword", status = HttpStatusCode.BadRequest
            )
            val newPassword = params["newPassword"] ?: return@post call.respondText(
                "Missing newPassword", status = HttpStatusCode.BadRequest
            )

            val session = call.sessions.get<UserSession>()
            if (session == null) {
                call.respond(HttpStatusCode.Unauthorized, "User not logged in")
            } else {
                val userId = session.userId
                val success = User.changePassword(userId, oldPassword, newPassword)

                if (success) {
                    call.respondText("Password changed successfully")
                } else {
                    call.respondText("Incorrect old password", status = HttpStatusCode.Unauthorized)
                }
            }
        }

        // Вход пользователя
        post("/login") {
            val params = call.receiveParameters()
            val username = params["username"] ?: return@post call.respondText(
                "Missing username", status = HttpStatusCode.BadRequest
            )
            val password = params["password"] ?: return@post call.respondText(
                "Missing password", status = HttpStatusCode.BadRequest
            )

            val isLoggedIn = User.login(username, password)

            if (isLoggedIn) {
                // Получаем данные пользователя для создания сессии
                val user = transaction {
                    Users.select { Users.username eq username }
                        .map { User(it[Users.id], it[Users.username], it[Users.password]) }
                        .single()
                }

                // Устанавливаем сессию
                call.sessions.set(UserSession(user.id, user.username))

                call.respondText("Login successful")
            } else {
                call.respondText("Invalid username or password", status = HttpStatusCode.Unauthorized)
            }
        }
        post("/logout") {
            call.sessions.clear<UserSession>()
            call.respondText("Logged out successfully")
        }

        get("/api/user-info") {
            val session = call.sessions.get<UserSession>()
            if (session == null) {
                call.respond(HttpStatusCode.Unauthorized, "User not logged in")
            } else {
                call.respondText("Logged in as ${session.username} (ID: ${session.userId})")
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
        // Новый маршрут для создания объявления
        post("/api/ads/create") {
            val session = call.sessions.get<UserSession>()
            if (session == null) {
                call.respond(HttpStatusCode.Unauthorized, "User not logged in")
                return@post
            }

            val searchType = call.parameters["searchType"] ?: return@post call.respondText(
                "Missing searchType", status = HttpStatusCode.BadRequest
            )
            val subject = call.parameters["subject"] ?: return@post call.respondText(
                "Missing subject", status = HttpStatusCode.BadRequest
            )

            try {
                // Получаем ID текущего пользователя из сессии
                val userId = session.userId

                // Создаем объявление
                AdService.createAd(searchType, subject, userId)

                call.respondText("Ad created successfully")
            } catch (e: Exception) {
                call.respondText("Error creating ad: ${e.message}", status = HttpStatusCode.InternalServerError)
            }
        }


        // Маршрут для обновления объявления
        put("/api/ads/{adId}") {
            val session = call.sessions.get<UserSession>()
            if (session == null) {
                call.respond(HttpStatusCode.Unauthorized, "User not logged in")
                return@put
            }

            val adId = call.parameters["adId"]?.toIntOrNull() ?: return@put call.respondText(
                "Invalid adId", status = HttpStatusCode.BadRequest
            )

            val params = call.receiveParameters()
            val newSearchType = params["searchType"]
            val newSubject = params["subject"]

            try {
                val ad = AdService.getAdById(adId)
                if (ad?.authorId != session.userId) {
                    call.respond(HttpStatusCode.Forbidden, "You can only edit your own ads")
                    return@put
                }

                AdService.updateAd(adId, newSearchType, newSubject)
                call.respondText("Ad updated successfully")
            } catch (e: Exception) {
                call.respondText("Error updating ad: ${e.message}", status = HttpStatusCode.InternalServerError)
            }
        }

        // Маршрут для удаления объявления
        delete("/api/ads/{adId}") {
            val session = call.sessions.get<UserSession>()
            if (session == null) {
                call.respond(HttpStatusCode.Unauthorized, "User not logged in")
                return@delete
            }

            val adId = call.parameters["adId"]?.toIntOrNull() ?: return@delete call.respondText(
                "Invalid adId", status = HttpStatusCode.BadRequest
            )

            try {
                val ad = AdService.getAdById(adId)
                if (ad?.authorId != session.userId) {
                    call.respond(HttpStatusCode.Forbidden, "You can only delete your own ads")
                    return@delete
                }

                AdService.deleteAd(adId)
                call.respondText("Ad deleted successfully")
            } catch (e: Exception) {
                call.respondText("Error deleting ad: ${e.message}", status = HttpStatusCode.InternalServerError)
            }
        }
        route("/api/messages") {
            // Открыть или создать переписку
            get("/conversation/open") {
                val user1Id = call.parameters["user1Id"]?.toIntOrNull()
                    ?: return@get call.respondText("Invalid user1Id", status = HttpStatusCode.BadRequest)
                val user2Id = call.parameters["user2Id"]?.toIntOrNull()
                    ?: return@get call.respondText("Invalid user2Id", status = HttpStatusCode.BadRequest)

                try {
                    val conversation = MessengerService.openOrCreateConversation(user1Id, user2Id)
                    call.respondText(Json.encodeToString(conversation), contentType = ContentType.Application.Json)
                } catch (e: Exception) {
                    call.respondText(
                        "Error opening conversation: ${e.message}",
                        status = HttpStatusCode.InternalServerError
                    )
                }
            }

            // Отправка сообщения
            post("/send") {
                val params = call.receiveParameters()
                val senderId = params["senderId"]?.toIntOrNull()
                    ?: return@post call.respondText("Missing senderId", status = HttpStatusCode.BadRequest)
                val receiverId = params["receiverId"]?.toIntOrNull()
                    ?: return@post call.respondText("Missing receiverId", status = HttpStatusCode.BadRequest)
                val content = params["content"]
                    ?: return@post call.respondText("Missing content", status = HttpStatusCode.BadRequest)

                try {
                    val message = MessengerService.sendMessage(senderId, receiverId, content)
                    call.respondText(Json.encodeToString(message), contentType = ContentType.Application.Json)
                } catch (e: Exception) {
                    call.respondText(e.message ?: "Error sending message", status = HttpStatusCode.InternalServerError)
                }
            }

            // Получение сообщений между пользователями
            get("/conversation/{user1Id}/{user2Id}") {
                val user1Id = call.parameters["user1Id"]?.toIntOrNull()
                    ?: return@get call.respondText("Invalid user1Id", status = HttpStatusCode.BadRequest)
                val user2Id = call.parameters["user2Id"]?.toIntOrNull()
                    ?: return@get call.respondText("Invalid user2Id", status = HttpStatusCode.BadRequest)

                val messages = MessengerService.getMessagesBetweenUsers(user1Id, user2Id)
                call.respondText(Json.encodeToString(messages), contentType = ContentType.Application.Json)
            }

            // Пометить сообщения как прочитанные
            post("/read") {
                val params = call.receiveParameters()
                val receiverId = params["receiverId"]?.toIntOrNull()
                    ?: return@post call.respondText("Missing receiverId", status = HttpStatusCode.BadRequest)
                val senderId = params["senderId"]?.toIntOrNull()
                    ?: return@post call.respondText("Missing senderId", status = HttpStatusCode.BadRequest)

                MessengerService.markMessagesAsRead(receiverId, senderId)
                call.respondText("Messages marked as read")
            }

            // Получение количества непрочитанных сообщений
            get("/unread/{userId}") {
                val userId = call.parameters["userId"]?.toIntOrNull()
                    ?: return@get call.respondText("Invalid userId", status = HttpStatusCode.BadRequest)

                val count = MessengerService.getUnreadMessageCount(userId)
                call.respondText(count.toString())
            }

            // Получение списка контактов
            get("/contacts/{userId}") {
                val userId = call.parameters["userId"]?.toIntOrNull()
                    ?: return@get call.respondText("Invalid userId", status = HttpStatusCode.BadRequest)

                val contacts = MessengerService.getRecentContacts(userId)
                call.respondText(Json.encodeToString(contacts), contentType = ContentType.Application.Json)
            }

            // Получение последнего сообщения между пользователями
            get("/last/{user1Id}/{user2Id}") {
                val user1Id = call.parameters["user1Id"]?.toIntOrNull()
                    ?: return@get call.respondText("Invalid user1Id", status = HttpStatusCode.BadRequest)
                val user2Id = call.parameters["user2Id"]?.toIntOrNull()
                    ?: return@get call.respondText("Invalid user2Id", status = HttpStatusCode.BadRequest)

                val lastMessage = MessengerService.getLastMessage(user1Id, user2Id)
                call.respondText(Json.encodeToString(lastMessage), contentType = ContentType.Application.Json)
            }
        }
    }



}
