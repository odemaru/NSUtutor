package com.nsututor

import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.javatime.*
import java.time.LocalDateTime

// Таблица для карточек
object Ads : Table() {
    val id = integer("id").autoIncrement()
    val searchType = varchar("search_type", 50)
    val subject = varchar("subject", 255)
    val authorId = integer("author_id").references(Users.id)
    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime())
    override val primaryKey = PrimaryKey(id)
}

// Класс данных для объявления
@Serializable
data class Ad(
    val id: Int,
    val searchType: String,
    val subject: String,
    val authorId: Int,
//    val createdAt: String // Преобразуем LocalDateTime в String для сериализации
)

class AdService {
    companion object {
        // Получение всех карточек
        fun getAllAdsAsJson(): List<Ad> {
            return transaction {
                Ads.selectAll().map {
                    Ad(
                        id = it[Ads.id],
                        searchType = it[Ads.searchType],
                        subject = it[Ads.subject],
                        authorId = it[Ads.authorId],
//                        createdAt = it[Ads.createdAt].toString() // Преобразуем LocalDateTime в String
                    )
                }
            }
        }

        // Другие методы остаются без изменений
        fun createAd(searchType: String, subject: String, authorId: Int) {
            transaction {
                Ads.insert {
                    it[Ads.searchType] = searchType
                    it[Ads.subject] = subject
                    it[Ads.authorId] = authorId
                    it[Ads.createdAt] = LocalDateTime.now()
                }
            }
        }

        fun getAdById(adId: Int): Ad? {
            return transaction {
                Ads.select { Ads.id eq adId }.singleOrNull()?.let {
                    Ad(
                        id = it[Ads.id],
                        searchType = it[Ads.searchType],
                        subject = it[Ads.subject],
                        authorId = it[Ads.authorId],
//                        createdAt = it[Ads.createdAt].toString()
                    )
                }
            }
        }

        fun deleteAd(adId: Int) {
            transaction {
                Ads.deleteWhere { Ads.id eq adId }
            }
        }

        fun updateAd(adId: Int, newSearchType: String?, newSubject: String?) {
            transaction {
                Ads.update({ Ads.id eq adId }) {
                    if (newSearchType != null) it[Ads.searchType] = newSearchType
                    if (newSubject != null) it[Ads.subject] = newSubject
                }
            }
        }

        fun insertSampleAdsIfNotExist() {
            transaction {
                val user1 = Users.select { Users.id eq 4 }.singleOrNull()
                if (user1 == null) {
                    Users.insert {
                        it[Users.username] = "user1"
                        it[Users.password] = "password1"
                        it[Users.role] = "student"
                    }
                }

                val user2 = Users.select { Users.id eq 5 }.singleOrNull()
                if (user2 == null) {
                    Users.insert {
                        it[Users.username] = "user2"
                        it[Users.password] = "password2"
                        it[Users.role] = "tutor"
                    }
                }

                val ad1 = Ads.select { Ads.searchType eq "Ищу репетитора" and (Ads.subject eq "Высшая математика, 3 курс ФИТ") }.singleOrNull()
                if (ad1 == null) {
                    Ads.insert {
                        it[Ads.searchType] = "Ищу репетитора"
                        it[Ads.subject] = "Высшая математика, 3 курс ФИТ"
                        it[Ads.authorId] = 4
                        it[Ads.createdAt] = LocalDateTime.now()
                    }
                }

                val ad2 = Ads.select { Ads.searchType eq "Ищу студентов" and (Ads.subject eq "Английский язык, 1 курс ФИТ") }.singleOrNull()
                if (ad2 == null) {
                    Ads.insert {
                        it[Ads.searchType] = "Ищу студентов"
                        it[Ads.subject] = "Английский язык, 1 курс ФИТ"
                        it[Ads.authorId] = 5
                        it[Ads.createdAt] = LocalDateTime.now()
                    }
                }
            }
        }
    }
}
