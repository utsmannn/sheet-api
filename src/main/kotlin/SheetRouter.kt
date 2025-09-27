package com.utsman

import io.ktor.http.*
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.sheetRouting() {
    get("/api/sheets/{sheetName}") {
        val sheetName = call.parameters["sheetName"] ?: throw BadRequestException("sheetName required")
        val perPage = call.request.queryParameters["per_page"]?.toIntOrNull() ?: 10
        val offset = call.request.queryParameters["offset"]?.toIntOrNull() ?: 1

        val (jsonBody, total) = SheetModule.getJsonSlice(sheetName, perPage, offset)
        call.response.header("X-Total-Count", total.toString())
        call.respondText(jsonBody, ContentType.Application.Json)
    }

    post("/api/sheets/{sheetName}") {
        val sheetName = call.parameters["sheetName"] ?: throw BadRequestException("sheetName required")
        val body = call.receiveText()
        SheetModule.appendRowFromJson(sheetName, body)
        call.respondText("""{"ok":true}""", ContentType.Application.Json)
    }
}