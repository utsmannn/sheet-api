package com.utsman

import io.ktor.server.application.*
import io.ktor.server.http.content.*
import io.ktor.server.response.respond
import io.ktor.server.routing.*
import java.io.File

fun Application.configureRouting() {
    routing {
        // API routes first (to avoid conflict)
        route("/api") {
            sheetRouting()
        }

        // Serve React app from landingpage/dist directory
        staticFiles("/", File("${System.getProperty("user.dir")}/landingpage/dist")) {
            default("index.html")
        }
    }
}
