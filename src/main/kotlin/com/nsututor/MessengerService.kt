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
    fun openOrCreateConversation(user1Id: Int, user2Id: Int): List<Message> {
        return transaction {
            // Проверяем существует ли переписка (есть ли сообщения между пользователями)
            val existingMessages = Messages.select {
                (Messages.senderId eq user1Id and (Messages.receiverId eq user2Id)) or
                        (Messages.senderId eq user2Id and (Messages.receiverId eq user1Id))
            }.count() > 0

            if (!existingMessages) {
                // Если переписки нет, создаем первое системное сообщение
                Messages.insert {
                    it[senderId] = user1Id
                    it[receiverId] = user2Id
                    it[content] = "Переписка создана"
                    it[isRead] = true
                }
            }

            // Возвращаем все сообщения между пользователями
            getMessagesBetweenUsers(user1Id, user2Id)
        }
    }

    fun sendMessage(senderId: Int, receiverId: Int, content: String): Message {
        return transaction {
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
                content = content,
                isRead = false
            )
        }
    }

    fun getMessagesBetweenUsers(user1Id: Int, user2Id: Int): List<Message> {
        return transaction {
            Messages.select {
                (Messages.senderId eq user1Id and (Messages.receiverId eq user2Id)) or
                        (Messages.senderId eq user2Id and (Messages.receiverId eq user1Id))
            }.orderBy(Messages.id, SortOrder.ASC)
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

    fun markMessagesAsRead(receiverId: Int, senderId: Int) {
        transaction {
            Messages.update({
                Messages.receiverId eq receiverId and (Messages.senderId eq senderId) and (Messages.isRead eq false)
            }) {
                it[isRead] = true
            }
        }
    }

    fun getUnreadMessageCount(userId: Int): Int {
        return transaction {
            Messages.select {
                Messages.receiverId eq userId and (Messages.isRead eq false)
            }.count().toInt()
        }
    }

    fun getRecentContacts(userId: Int): List<User> {
        return transaction {
            val uniqueUserIds = (Messages
                .slice(Messages.senderId)
                .select { Messages.receiverId eq userId }
                .map { it[Messages.senderId] } +
                    Messages
                        .slice(Messages.receiverId)
                        .select { Messages.senderId eq userId }
                        .map { it[Messages.receiverId] })
                .distinct()

            Users.select { Users.id inList uniqueUserIds }
                .map {
                    User(
                        id = it[Users.id],
                        username = it[Users.username],
                        password = "" // Don't send password
                    )
                }
        }
    }

    fun getLastMessage(user1Id: Int, user2Id: Int): Message? {
        return transaction {
            Messages.select {
                (Messages.senderId eq user1Id and (Messages.receiverId eq user2Id)) or
                        (Messages.senderId eq user2Id and (Messages.receiverId eq user1Id))
            }.orderBy(Messages.id to SortOrder.DESC)
                .limit(1)
                .map {
                    Message(
                        id = it[Messages.id],
                        senderId = it[Messages.senderId],
                        receiverId = it[Messages.receiverId],
                        content = it[Messages.content],
                        isRead = it[Messages.isRead]
                    )
                }
                .firstOrNull()
        }
    }
}