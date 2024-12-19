package com.nsututor

import com.sun.tools.javac.resources.CompilerProperties
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction

object DatabaseFactory {
    fun init() {
        // Подключение к базе данных
        Database.connect(
            url = "jdbc:postgresql://localhost:5432/nsututor",
            driver = "org.postgresql.Driver",
            user = "postgres",
            password = "root"
        )

        // Создание таблиц при старте
        transaction {
            SchemaUtils.create(Users)
            SchemaUtils.create(Ads)
            SchemaUtils.create(Messages)
        }

        User.addRootUserIfNotExists()
        AdService.insertSampleAdsIfNotExist()
//        MessengerService.createInitialConversation()
    }
}
