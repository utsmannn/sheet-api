package com.utsman

import com.google.api.services.sheets.v4.Sheets
import com.google.api.services.sheets.v4.model.Sheet
import io.ktor.http.*
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.util.toMap
import kotlinx.serialization.json.*

fun Route.sheetRouting(sheets: Sheets) {

    get {
        val sheetId = call.parameters["sheetId"] ?: System.getenv("SHEET_ID").ifEmpty {
            throw BadRequestException("sheetId required in path or SHEET_ID env var")
        }
        val perPage = call.request.queryParameters["per_page"]?.toIntOrNull() ?: 10
        val offset = call.request.queryParameters["offset"]?.toIntOrNull() ?: 1

        val sheetModule = SheetModule(sheetId, sheets)
        val (sheetsData, totalSheets) = sheetModule.getAllSheetsData(perPage, offset)

        call.response.header("X-Total-Count", totalSheets.toString())
        call.respond(sheetsData)
    }

    get("/{sheetName}") {
        val sheetId = call.parameters["sheetId"] ?: System.getenv("SHEET_ID").ifEmpty {
            throw BadRequestException("sheetId required in path or SHEET_ID env var")
        }
        val sheetName = call.parameters["sheetName"] ?: throw BadRequestException("sheetName required")
        val perPage = call.request.queryParameters["per_page"]?.toIntOrNull() ?: 10
        val offset = call.request.queryParameters["offset"]?.toIntOrNull() ?: 1

        val sheetModule = SheetModule(sheetId, sheets)
        val data = sheetModule.getAutoFormattedData(sheetName, perPage, offset)

        // For flat data (JsonArray), include pagination header
        if (data is JsonArray) {
            val (_, total) = sheetModule.getJsonSlice(sheetName, perPage, offset)
            call.response.header("X-Total-Count", total.toString())
        }

        call.respond(data)
    }

    get("/{sheetName}/schema") {
        val sheetId = call.parameters["sheetId"] ?: System.getenv("SHEET_ID").ifEmpty {
            throw BadRequestException("sheetId required in path or SHEET_ID env var")
        }
        val sheetName = call.parameters["sheetName"] ?: throw BadRequestException("sheetName required")
        val sheetModule = SheetModule(sheetId, sheets)
        val schema = sheetModule.getSheetSchema(sheetName)

        val schemaJson = buildJsonObject {
            put("sheetName", sheetName)
            put("schema", schema)
        }

        call.respond(schemaJson)
    }

    post("/{sheetName}") {
        val sheetId = call.parameters["sheetId"] ?: System.getenv("SHEET_ID").ifEmpty {
            throw BadRequestException("sheetId required in path or SHEET_ID env var")
        }
        val sheetName = call.parameters["sheetName"] ?: throw BadRequestException("sheetName required")
        val sheetModule = SheetModule(sheetId, sheets)

        try {
            val body = call.receiveText()
            val jsonObject = Json.parseToJsonElement(body).jsonObject
            val queryParameters = call.request.queryParameters

            val result = if (queryParameters.isEmpty()) {
                // Handle appending a root entity (old behavior)
                sheetModule.appendRowFromJson(sheetName, body)
            } else {
                // Handle appending a nested item
                val identifiers = queryParameters.toMap().mapValues { it.value.first() }
                sheetModule.appendNestedItem(sheetName, identifiers, jsonObject)
            }

            if (result != null) {
                call.respond(buildJsonObject {
                    put("ok", true)
                    put("updatedRange", result.updates.updatedRange)
                })
            } else {
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "Failed to append data."))
            }

        } catch (e: Exception) {
            call.respond(
                HttpStatusCode.BadRequest,
                mapOf(
                    "error" to "Invalid JSON format or request",
                    "details" to (e.message ?: "Unknown error")
                )
            )
        }
    }

    patch("/{sheetName}") {
        val sheetId = call.parameters["sheetId"] ?: System.getenv("SHEET_ID").ifEmpty {
            throw BadRequestException("sheetId required in path or SHEET_ID env var")
        }
        val sheetName = call.parameters["sheetName"] ?: throw BadRequestException("sheetName required")
        val sheetModule = SheetModule(sheetId, sheets)

        try {
            val body = call.receive<JsonObject>()
            if (body.keys.size != 1) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to "Request body must contain exactly one field to update.")
                )
                return@patch
            }

            val queryParameters = call.request.queryParameters
            val result = if (queryParameters.isEmpty()) {
                // Handle root entity update
                sheetModule.updateRootEntityField(sheetName, body)
            } else {
                // Handle nested entity update
                val identifiers = queryParameters.toMap().mapValues { it.value.first() }
                sheetModule.updateNestedField(sheetName, identifiers, body)
            }

            if (result != null) {
                call.respond(buildJsonObject {
                    put("ok", true)
                    put("updatedRange", Json.encodeToJsonElement(result.updatedRange))
                })
            } else {
                call.respond(
                    HttpStatusCode.NotFound,
                    mapOf("error" to "Field or target row not found.")
                )
            }

        } catch (e: Exception) {
            call.respond(
                HttpStatusCode.BadRequest,
                mapOf(
                    "error" to "Invalid JSON format or request",
                    "details" to (e.message ?: "Unknown error")
                )
            )
        }
    }
}