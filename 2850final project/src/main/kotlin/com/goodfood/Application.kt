package com.goodfood

import com.goodfood.config.*
import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*

fun main(args: Array<String>): Unit = EngineMain.main(args)

fun Application.module() {
    configureDatabase()
    configureSecurity()
    configureTemplating()
    install(ContentNegotiation) { jackson() }
    configureRouting()
}
