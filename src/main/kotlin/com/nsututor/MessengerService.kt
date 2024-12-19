package com.nsututor

import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

@Serializable
data class Message(
    val id: Int? = null,
    val senderId: Int,
    val receiverId: Int,
    val content: String,
    val isRead: Boolean = false
)

object Messages : Table() {
    val id = integer("id").autoIncrement()
    val senderId = reference("sender_id", Users.id)
    val receiverId = reference("receiver_id", Users.id)
    val content = text("content")
    val isRead = bool("is_read").default(false)

    override val primaryKey = PrimaryKey(id)
}

object MessengerService {
    // Отправить сообщение от одного пользователя другому
    fun sendMessage(senderId: Int, receiverId: Int, content: String): Message {
        return transaction {
            // Проверка существования отправителя и получателя
            val senderExists = Users.select { Users.id eq senderId }.count() > 0
            val receiverExists = Users.select { Users.id eq receiverId }.count() > 0

            if (!senderExists || !receiverExists) {
                throw IllegalArgumentException("Invalid sender or receiver")
            }

            val messageId = Messages.insert {
                it[Messages.senderId] = senderId
                it[Messages.receiverId] = receiverId
                it[Messages.content] = content
                it[isRead] = false
            } get Messages.id

            Message(
                id = messageId,
                senderId = senderId,
                receiverId = receiverId,
                content = content
            )
        }
    }

    // Получить сообщения между двумя пользователями
    fun getMessagesBetweenUsers(user1Id: Int, user2Id: Int): List<Message> {
        return transaction {
            Messages.select {
                (Messages.senderId eq user1Id and (Messages.receiverId eq user2Id)) or
                        (Messages.senderId eq user2Id and (Messages.receiverId eq user1Id))
            }.orderBy(Messages.id)
                .map {
                    Message(
                        id = it[Messages.id],
                        senderId = it[Messages.senderId],
                        receiverId = it[Messages.receiverId],
                        content = it[Messages.content],
                        isRead = it[Messages.isRead]
                    )
                }
        }
    }

    // Пометить сообщения как прочитанные
    fun markMessagesAsRead(receiverId: Int, senderId: Int) {
        transaction {
            Messages.update({
                (Messages.receiverId eq receiverId) and
                        (Messages.senderId eq senderId) and
                        (Messages.isRead eq false)
            }) {
                it[isRead] = true
            }
        }
    }

    // Получить количество непрочитанных сообщений
    fun getUnreadMessageCount(userId: Int): Int {
        return transaction {
            Messages.select {
                (Messages.receiverId eq userId) and (Messages.isRead eq false)
            }.count().toInt()
        }
    }

    // Получить недавние контакты пользователя
    fun getRecentContacts(userId: Int, limit: Int = 10): List<Int> {
        return transaction {
            (Messages.slice(Messages.senderId, Messages.receiverId)
                .select {
                    (Messages.senderId eq userId) or (Messages.receiverId eq userId)
                }
                .groupBy(Messages.senderId, Messages.receiverId)
                .orderBy(Messages.id.max(), SortOrder.DESC)
                .limit(limit)
                .map { row ->
                    if (row[Messages.senderId] == userId) row[Messages.receiverId]
                    else row[Messages.senderId]
                })
        }
    }

    // Создание тестовой переписки
    fun createInitialConversation() {
        transaction {
            // Создаем тестовых пользователей, если их нет
            val user1Id = Users.insert {
                it[username] = "testuser1"
                it[password] = "password1" // В реальности нужно хешировать
                it[role] = "student"
            } get Users.id

            val user2Id = Users.insert {
                it[username] = "testuser2"
                it[password] = "password2" // В реальности нужно хешировать
                it[role] = "student"
            } get Users.id

            // Создаем несколько тестовых сообщений
            val messages = listOf(
                Message(
                    senderId = user1Id,
                    receiverId = user2Id,
                    content = "Привет! Как дела?"
                ),
                Message(
                    senderId = user2Id,
                    receiverId = user1Id,
                    content = "Привет! Все хорошо, готовлюсь к экзамену."
                ),
                Message(
                    senderId = user1Id,
                    receiverId = user2Id,
                    content = "Удачи! Чем помочь?"
                ),
                Message(
                    senderId = user2Id,
                    receiverId = user1Id,
                    content = "Спасибо! Нужна помощь по математике."
                )
            )

            // Вставляем сообщения в базу данных
            messages.forEach { message ->
                Messages.insert {
                    it[senderId] = message.senderId
                    it[receiverId] = message.receiverId
                    it[content] = message.content
                    it[isRead] = false
                }
            }
        }
    }

    // Получение ID тестовых пользователей
    fun getTestUserIds(): Pair<Int, Int> {
        return transaction {
            val user1 = Users.select { Users.username eq "testuser1" }.firstOrNull()
            val user2 = Users.select { Users.username eq "testuser2" }.firstOrNull()

            if (user1 == null || user2 == null) {
                throw IllegalStateException("Тестовые пользователи не созданы. Сначала вызовите createInitialConversation()")
            }

            Pair(
                user1[Users.id],
                user2[Users.id]
            )
        }
    }
}