package com.nsututor

import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.mindrot.jbcrypt.BCrypt

// Таблица пользователей
object Users : Table() {
    val id = integer("id").autoIncrement()
    val username = varchar("username", 255).uniqueIndex()
    val password = varchar("password", 255) // Хешированный пароль
    val role = varchar("role", 50).nullable()
    override val primaryKey = PrimaryKey(id)
}

// Класс для работы с пользователями
class User(val id: Int, val username: String, val password: String) {

    companion object {

        // Регистрация пользователя с хешированием пароля
        fun register(username: String, password: String, role: String) {
            transaction {
                // Хешируем пароль перед сохранением
                val hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt())

                // Проверяем, есть ли уже пользователь с таким именем
                val existingUser = Users.select { Users.username eq username }.singleOrNull()
                if (existingUser != null) {
                    throw IllegalArgumentException("User with this username already exists")
                }

                // Добавляем нового пользователя
                Users.insert {
                    it[Users.username] = username
                    it[Users.password] = hashedPassword
                    it[Users.role] = role
                }
            }
        }

        // Проверка логина пользователя
        fun login(username: String, password: String): Boolean {
            return transaction {
                val user = Users.select { Users.username eq username }.singleOrNull()

                if (user != null) {
                    // Сравниваем хеш пароля с введённым
                    BCrypt.checkpw(password, user[Users.password])
                } else {
                    false
                }
            }
        }

        // Добавление пользователя "root" с паролем "root", если его нет
        fun addRootUserIfNotExists() {
            transaction {
                val existingUser = Users.select { Users.username eq "root" }.singleOrNull()
                if (existingUser ==  null) {
                    val hashedPassword = BCrypt.hashpw("root", BCrypt.gensalt())
                    Users.insert {
                        it[Users.username] = "root"
                        it[Users.password] = hashedPassword
                        it[Users.role] = "Student"
                    }
                    Users.insert {
                        it[Users.username] = "root2"
                        it[Users.password] = hashedPassword
                        it[Users.role] = "Student"
                    }
                }
            }
        }
    }
}
