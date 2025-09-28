package com.utsman

import io.ktor.http.*

/**
 * Custom exception for handling Google Sheets API errors
 */
open class SheetsApiException(
    val statusCode: HttpStatusCode,
    message: String,
    cause: Throwable? = null
) : Exception(message, cause)

/**
 * Exception for access denied errors
 */
class SheetsAccessDeniedException(message: String, cause: Throwable? = null) :
    SheetsApiException(HttpStatusCode.Forbidden, message, cause)

/**
 * Exception for not found errors
 */
class SheetsNotFoundException(message: String, cause: Throwable? = null) :
    SheetsApiException(HttpStatusCode.NotFound, message, cause)

/**
 * Exception for unauthorized errors
 */
class SheetsUnauthorizedException(message: String, cause: Throwable? = null) :
    SheetsApiException(HttpStatusCode.Unauthorized, message, cause)

/**
 * Exception for bad request errors
 */
class SheetsBadRequestException(message: String, cause: Throwable? = null) :
    SheetsApiException(HttpStatusCode.BadRequest, message, cause)