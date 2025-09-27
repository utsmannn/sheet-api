package com.utsman

import io.ktor.http.*
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.json.*

fun Route.sheetRouting() {
    get("/sheets") {
        val sheetNames = SheetModule.getSheetNames()
        call.respond(sheetNames)
    }

    get("/sheets/{sheetName}") {
        val sheetName = call.parameters["sheetName"] ?: throw BadRequestException("sheetName required")
        val perPage = call.request.queryParameters["per_page"]?.toIntOrNull() ?: 10
        val offset = call.request.queryParameters["offset"]?.toIntOrNull() ?: 1

        val (arr, total) = SheetModule.getJsonSlice(sheetName, perPage, offset)
        call.response.header("X-Total-Count", total.toString())
        call.respond(arr)
    }

    get("/sheets/{sheetName}/schema") {
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

        call.respond(schemaJson)
    }

    post("/sheets/{sheetName}") {
        val sheetName = call.parameters["sheetName"] ?: throw BadRequestException("sheetName required")
        val body = call.receiveText()

        val lenientJson = Json { isLenient = true; ignoreUnknownKeys = true }

        try {
            val jsonObject = lenientJson.parseToJsonElement(body).jsonObject
            val schema = SheetModule.getSheetSchema(sheetName)
            val validationErrors = SheetModule.validateJsonSchema(jsonObject, schema)

            if (validationErrors.isNotEmpty()) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf(
                        "error" to "Validation failed",
                        "details" to validationErrors
                    )
                )
                return@post
            }

            SheetModule.appendRowFromJson(sheetName, body)
            call.respond(mapOf("ok" to true))
        } catch (e: Exception) {
            call.respond(
                HttpStatusCode.BadRequest,
                mapOf(
                    "error" to "Invalid JSON format",
                    "details" to (e.message ?: "Unknown error")
                )
            )
        }
    }
}