package com.utsman

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.sheets.v4.Sheets
import com.google.auth.http.HttpCredentialsAdapter
import com.google.auth.oauth2.GoogleCredentials
import io.ktor.server.application.*
import io.ktor.server.http.content.*
import io.ktor.server.routing.*
import java.io.File

private fun buildSheets(): Sheets {
    val sheets = executeWithErrorHandling {
        val credentialFile = File(System.getenv("CREDENTIAL_PATH"))
            .also { require(it.exists()) { "credential file not found at ${it.absolutePath}" } }

        val transport = GoogleNetHttpTransport.newTrustedTransport()
        val jsonFactory = GsonFactory.getDefaultInstance()

        val credentials = GoogleCredentials.fromStream(credentialFile.inputStream())
            .createScoped(listOf("https://www.googleapis.com/auth/spreadsheets"))


        Sheets.Builder(transport, jsonFactory, HttpCredentialsAdapter(credentials))
            .setApplicationName("KtorSheetAPI")
            .build()
    }

    return sheets
}

fun Application.configureRouting() {
    val sheets = buildSheets()
    routing {
        // API routes first (to avoid conflict)
        route("/api/sheets") {
            sheetRouting(sheets)
        }

        route(path = "/api/sheetId/{sheetId}") {
            sheetRouting(sheets)
        }

        // Serve React app from resources/static directory
        staticResources("/", "static") {
            default("index.html")
        }
    }
}
