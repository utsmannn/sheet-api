package com.utsman

import com.google.api.services.sheets.v4.Sheets
import com.google.api.services.sheets.v4.model.Sheet
import io.ktor.http.*
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.json.*

fun Route.sheetRouting(sheets: Sheets) {

    get {
        val sheetId = call.parameters["sheetId"] ?: System.getenv("SHEET_ID").ifEmpty {
            throw BadRequestException("sheetId required in path or SHEET_ID env var")
        }
        val sheetModule = SheetModule(sheetId, sheets)
        val sheetNames = sheetModule.getSheetNames()
        call.respond(sheetNames)
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
        val body = call.receiveText()
        val sheetModule = SheetModule(sheetId, sheets)

        val lenientJson = Json { isLenient = true; ignoreUnknownKeys = true }

        try {
            val jsonObject = lenientJson.parseToJsonElement(body).jsonObject
            // TODO: Implement validation for new schema structure
            // val schema = sheetModule.getSheetSchema(sheetName)
            // val validationErrors = sheetModule.validateJsonSchema(jsonObject, schema)

            // For now, skip validation until we implement new schema validation
            val validationErrors = emptyList<String>()

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

            sheetModule.appendRowFromJson(sheetName, body)
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

    patch("/{sheetName}") {
        val sheetId = call.parameters["sheetId"] ?: System.getenv("SHE-ET_ID").ifEmpty {
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

            val result = sheetModule.updateRootEntityField(sheetName, body)

            if (result != null) {
                call.respond(buildJsonObject {
                    put("ok", true)
                    put("updatedRange", Json.encodeToJsonElement(result.updatedRange))
                })
            } else {
                call.respond(
                    HttpStatusCode.NotFound,
                    mapOf("error" to "Field not found in sheet header or no data available.")
                )
            }

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