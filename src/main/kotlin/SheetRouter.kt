package com.utsman

import io.ktor.http.*
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.json.*

fun Route.sheetRouting() {
    get("/api/sheets/{sheetName}") {
        val sheetName = call.parameters["sheetName"] ?: throw BadRequestException("sheetName required")
        val perPage = call.request.queryParameters["per_page"]?.toIntOrNull() ?: 10
        val offset = call.request.queryParameters["offset"]?.toIntOrNull() ?: 1

        val (jsonBody, total) = SheetModule.getJsonSlice(sheetName, perPage, offset)
        call.response.header("X-Total-Count", total.toString())
        call.respondText(jsonBody, ContentType.Application.Json)
    }

    get("/api/sheets/{sheetName}/schema") {
        val sheetName = call.parameters["sheetName"] ?: throw BadRequestException("sheetName required")
        val schema = SheetModule.getSheetSchema(sheetName)

        val schemaJson = buildJsonObject {
            put("sheetName", sheetName)
            put("fields", buildJsonObject {
                schema.forEach { (field, type) ->
                    put(field, buildJsonObject {
                        put("type", type)
                        put("required", true)
                    })
                }
            })
        }

        call.respondText(schemaJson.toString(), ContentType.Application.Json)
    }

    post("/api/sheets/{sheetName}") {
        val sheetName = call.parameters["sheetName"] ?: throw BadRequestException("sheetName required")
        val body = call.receiveText()

        try {
            val jsonObject = Json.parseToJsonElement(body).jsonObject
            val schema = SheetModule.getSheetSchema(sheetName)
            val validationErrors = SheetModule.validateJsonSchema(jsonObject, schema)

            if (validationErrors.isNotEmpty()) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    buildJsonObject {
                        put("error", "Validation failed")
                        put("details", buildJsonArray {
                            validationErrors.forEach { add(it) }
                        })
                    }.toString()
                )
                return@post
            }

            SheetModule.appendRowFromJson(sheetName, body)
            call.respondText("""{"ok":true}""", ContentType.Application.Json)
        } catch (e: Exception) {
            call.respond(
                HttpStatusCode.BadRequest,
                buildJsonObject {
                    put("error", "Invalid JSON format")
                    put("details", e.message ?: "Unknown error")
                }.toString()
            )
        }
    }
}