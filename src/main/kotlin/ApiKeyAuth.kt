package com.utsman

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.util.*

/**
 * API Key Authentication System
 *
 * This system provides secure API key validation using Base64 encoding with strict validation rules.
 * API keys are generated manually using the format: base64("{secret_key}:{timestamp_13_digits}")
 *
 * Security features:
 * - Secret key validation against environment variable API_SECRET_KEY
 * - Timestamp format validation (exactly 13 digits for milliseconds since epoch)
 * - Timestamp range validation (within 1 year of current time)
 * - Multiple header support (X-API-Key and Authorization Bearer)
 */
object ApiKeyAuth {

    private val secretKey: String by lazy {
        System.getenv("API_SECRET_KEY") ?: "default-secret-key"
    }

    /**
     * Validates a Base64 encoded API key
     *
     * Expected format after decoding: "{secret_key}:{13_digit_timestamp}"
     *
     * @param apiKey Base64 encoded API key string
     * @return true if valid, false otherwise
     */
    fun isValidApiKey(apiKey: String): Boolean {
        return try {
            val decoded = String(Base64.getDecoder().decode(apiKey))
            val parts = decoded.split(":")

            if (parts.size != 2) return false

            val keySecret = parts[0]
            val timestampStr = parts[1]
            val timestamp = timestampStr.toLongOrNull() ?: return false

            // Validate secret key
            if (keySecret != secretKey) return false

            // Validate timestamp format (must be exactly 13 digits)
            if (timestampStr.length != 13) return false

            // Validate timestamp range (within 1 year)
            val currentTime = System.currentTimeMillis()
            val maxAge = 365L * 24 * 60 * 60 * 1000

            timestamp in (currentTime - maxAge)..(currentTime + maxAge)
        } catch (e: Exception) {
            false
        }
    }

    // For testing purposes only
    internal fun generateApiKey(): String {
        val timestamp = System.currentTimeMillis()
        val data = "$secretKey:$timestamp"
        return Base64.getEncoder().encodeToString(data.toByteArray())
    }
}

/**
 * Ktor middleware for API key authentication
 *
 * Automatically protects all routes except public endpoints:
 * - Root (/)
 * - Health checks (/health*)
 * - Swagger documentation (/swagger*, /openapi*, /docs*)
 *
 * Supports both X-API-Key and Authorization Bearer headers.
 * Returns 401 with JSON error message for invalid/missing API keys.
 */
fun Application.configureApiKeyAuth() {
    intercept(ApplicationCallPipeline.Plugins) {
        val request = call.request
        val path = request.uri

        // Skip authentication for public endpoints
        if (path == "/" ||
            path.startsWith("/health") ||
            path.startsWith("/swagger") ||
            path.startsWith("/openapi") ||
            path.startsWith("/docs")) {
            return@intercept
        }

        // Extract API key from headers
        val apiKey = request.headers["X-API-Key"]
            ?: request.headers["Authorization"]?.removePrefix("Bearer ")

        if (apiKey == null) {
            call.respond(
                HttpStatusCode.Unauthorized,
                buildJsonObject {
                    put("error", "API key required")
                    put("message", "Please provide X-API-Key header or Authorization Bearer token")
                }
            )
            finish()
            return@intercept
        }

        if (!ApiKeyAuth.isValidApiKey(apiKey)) {
            call.respond(
                HttpStatusCode.Unauthorized,
                buildJsonObject {
                    put("error", "Invalid API key")
                    put("message", "The provided API key is invalid or expired")
                }
            )
            finish()
            return@intercept
        }
    }
}