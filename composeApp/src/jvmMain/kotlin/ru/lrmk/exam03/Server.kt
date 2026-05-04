package ru.lrmk.exam03

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.gson.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.request.receive
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.util.getOrFail
import ru.lrmk.exam03.database.Database
import ru.lrmk.exam03.database.User
import java.util.UUID


fun server() = embeddedServer(Netty, port = 80, host = "0.0.0.0", module = {

    install(CallLogging)            // Логирование запросов в консоль

    install(ContentNegotiation) {   // Подсистема обмена данными в формате json
        gson {
            setPrettyPrinting()
        }
    }

    val database by lazy {          // Локальная база данных SQLite в папке проекта
        Database(JdbcSqliteDriver("jdbc:sqlite:data.s3db", schema = Database.Schema))
    }

    routing {
        get("users") {
            val users = database.userQueries.select().executeAsList()
            call.respond(users)
        }

        post("login") {
            val params = call.parameters
            val login = params["login"]
            val password = params["password"]

            if (login == null || password == null) {
                return@post call.respond(HttpStatusCode.BadRequest)
            }

            runCatching {
                val user = database.userQueries.login(login, password).executeAsOne()

                if (user.password != password) {
                    if (user.count < 2) {
                        database.userQueries.count(login)
                        call.respond(HttpStatusCode.Forbidden)
                    } else {
                        database.userQueries.block(login)
                        call.respond(HttpStatusCode.Unauthorized)
                    }
                }
                else if (user.token == null) {
                    val newToken = UUID.randomUUID().toString()
                    database.userQueries.successLogin(newToken, login)
                    val updatedUser = database.userQueries.getUserByLogin(login).executeAsOne()
                    call.respond(updatedUser)
                }
                else
                    call.respond(user)
            }.onFailure {
                call.respond(HttpStatusCode.Forbidden)
            }
        }

        put("insert") {
            val param = call.parameters
            val login = param.getOrFail("login")
            val password = param.getOrFail("password")

            runCatching {
                database.userQueries.insert(login, password)
                println("Insert success for $login, $password")
                call.respond(HttpStatusCode.Created)
            }.onFailure {
                println("Insert failed for $login: ${it.message}")
                call.respond(HttpStatusCode.BadRequest)
            }
        }

        patch("update") {
            val param = call.parameters
            val oldLogin = param.getOrFail("login")
            val user = call.receive<User>()

            runCatching {
                val userUpdate = database.userQueries.update(
                    user.login,
                    user.password,
                    user.admin,
                    oldLogin
                )

                if (userUpdate.value == 1L) {
                    call.respond(HttpStatusCode.OK)
                }
                else {
                    call.respond(HttpStatusCode.NotFound)
                }
            }.onFailure {
                call.respond(HttpStatusCode.BadRequest)
            }
        }

        delete("delete") {
            val param = call.parameters
            val login = param.getOrFail("login")
            runCatching {
                val userDelete = database.userQueries.delete(login)

                if (userDelete.value == 1L) {
                    call.respond(HttpStatusCode.OK)
                }
                else {
                    call.respond(HttpStatusCode.NotFound)
                }
            }.onFailure {
                call.respond(HttpStatusCode.BadRequest)
            }
        }
    }
})