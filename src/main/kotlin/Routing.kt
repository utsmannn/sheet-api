package com.utsman

import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.configureRouting() {
    routing {
        get("/") {
            val credPath = System.getenv("CREDENTIAL_PATH") ?: "nothing"
            call.respondText("Hello World! : $credPath")
        }
        sheetRouting()
    }
}
