package com.utsman

import com.google.api.client.googleapis.json.GoogleJsonResponseException

/**
 * Execute Google Sheets API call with proper error handling
 */
inline fun <T> executeWithErrorHandling(operation: () -> T): T {
    try {
        return operation()
    } catch (e: GoogleJsonResponseException) {
        when (e.statusCode) {
            403 -> throw SheetsAccessDeniedException(
                "Access denied to Google Sheet. Please check permissions or credentials.",
                e
            )
            404 -> throw SheetsNotFoundException(
                "Sheet not found. Please check the sheet ID or sheet name.",
                e
            )
            401 -> throw SheetsUnauthorizedException(
                "Unauthorized access to Google Sheet. Please check your credentials.",
                e
            )
            400 -> throw SheetsBadRequestException(
                "Invalid request to Google Sheets API: ${e.message}",
                e
            )
            else -> throw SheetsApiException(
                io.ktor.http.HttpStatusCode.fromValue(e.statusCode),
                "Google Sheets API error: ${e.message}",
                e
            )
        }
    } catch (e: Exception) {
        throw SheetsApiException(
            io.ktor.http.HttpStatusCode.InternalServerError,
            "Unexpected error accessing Google Sheets: ${e.message}",
            e
        )
    }
}