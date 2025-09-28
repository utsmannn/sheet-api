package com.utsman

import com.google.api.client.googleapis.json.GoogleJsonResponseException
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

fun Application.configureErrorHandling() {
    install(StatusPages) {
        exception<SheetsApiException> { call, cause ->
            call.respond(
                cause.statusCode,
                buildJsonObject {
                    put("error", "Google Sheets API Error")
                    put("message", cause.message ?: "Unknown error")
                    put("status", cause.statusCode.value)
                }
            )
        }

        exception<SheetsAccessDeniedException> { call, cause ->
            call.respond(
                HttpStatusCode.Forbidden,
                buildJsonObject {
                    put("error", "Access Denied")
                    put("message", cause.message ?: "Access denied to Google Sheet")
                    put("status", 403)
                    put("suggestion", "Please check your Google Sheets permissions or credentials")
                }
            )
        }

        exception<SheetsNotFoundException> { call, cause ->
            call.respond(
                HttpStatusCode.NotFound,
                buildJsonObject {
                    put("error", "Not Found")
                    put("message", cause.message ?: "Sheet not found")
                    put("status", 404)
                    put("suggestion", "Please check the sheet ID or sheet name")
                }
            )
        }

        exception<SheetsUnauthorizedException> { call, cause ->
            call.respond(
                HttpStatusCode.Unauthorized,
                buildJsonObject {
                    put("error", "Unauthorized")
                    put("message", cause.message ?: "Unauthorized access")
                    put("status", 401)
                    put("suggestion", "Please check your API credentials")
                }
            )
        }

        exception<SheetsBadRequestException> { call, cause ->
            call.respond(
                HttpStatusCode.BadRequest,
                buildJsonObject {
                    put("error", "Bad Request")
                    put("message", cause.message ?: "Invalid request")
                    put("status", 400)
                }
            )
        }

        exception<GoogleJsonResponseException> { call, cause ->
            call.respond(
                HttpStatusCode.BadRequest,
                buildJsonObject {
                    put("error", cause.details.message)
                    put("message", cause.details.message)
                    put("status", cause.statusCode)
                }
            )
        }

        // Fallback for any other exceptions
        exception<Throwable> { call, cause ->
            call.respond(
                HttpStatusCode.InternalServerError,
                buildJsonObject {
                    put("error", "Internal Server Error")
                    put("message", cause.message)
                    put("status", 500)
                }
            )
        }
    }
}