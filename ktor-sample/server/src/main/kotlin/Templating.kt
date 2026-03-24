package com.example

import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.html.*
import io.ktor.server.http.content.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlin.random.Random
import kotlinx.css.*
import kotlinx.html.*
import org.jetbrains.exposed.sql.*

fun Application.configureTemplating() {
    routing {
        val random = Random(System.currentTimeMillis())
        
        get("/") {
            call.respondHtml {
                leaderboardPage(random)
            }
        }
        
        get("/more-rows") {
            call.respondHtml {
                body {
                    table {
                        tbody {
                            randomRows(random)
                        }
                    }
                }
            }
        }
        get("/styles.css") {
            call.respondCss {
                body {
                    backgroundColor = Color.darkBlue
                    margin = Margin(0.px)
                }
                rule("h1.page-title") {
                    color = Color.white
                }
            }
        }
        
        get("/html-css-dsl") {
            call.respondHtml {
                head {
                    link(rel = "stylesheet", href = "/styles.css", type = "text/css")
                }
                body {
                    h1(classes = "page-title") {
                        +"Hello from Ktor!"
                    }
                }
            }
        }
    }
}
suspend inline fun ApplicationCall.respondCss(builder: CssBuilder.() -> Unit) {
   this.respondText(CssBuilder().apply(builder).toString(), ContentType.Text.CSS)
}
