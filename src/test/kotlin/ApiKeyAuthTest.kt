package com.utsman

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.util.*
import kotlin.test.*

class ApiKeyAuthTest {

    @Test
    fun testValidApiKeyGeneration() {
        val apiKey = ApiKeyAuth.generateApiKey()

        assertNotNull(apiKey)
        assertTrue(apiKey.isNotEmpty())
        assertTrue(ApiKeyAuth.isValidApiKey(apiKey))
    }

    @Test
    fun testApiKeyValidation() {
        // Test valid API key
        val validKey = ApiKeyAuth.generateApiKey()
        assertTrue(ApiKeyAuth.isValidApiKey(validKey))

        // Test invalid keys
        assertFalse(ApiKeyAuth.isValidApiKey("invalid-key"))
        assertFalse(ApiKeyAuth.isValidApiKey(""))

        // Test malformed Base64
        assertFalse(ApiKeyAuth.isValidApiKey("not-base64!@#"))
    }

    @Test
    fun testTimestampValidation() {
        val secretKey = System.getenv("API_SECRET_KEY") ?: "default-secret-key"

        // Test valid 13-digit timestamp (current time)
        val validTimestamp = System.currentTimeMillis().toString()
        val validApiKey = Base64.getEncoder().encodeToString("$secretKey:$validTimestamp".toByteArray())
        assertTrue(ApiKeyAuth.isValidApiKey(validApiKey))

        // Test invalid 9-digit timestamp (should fail)
        val invalidTimestamp = "123456789"
        val invalidApiKey = Base64.getEncoder().encodeToString("$secretKey:$invalidTimestamp".toByteArray())
        assertFalse(ApiKeyAuth.isValidApiKey(invalidApiKey))

        // Test wrong secret key
        val wrongSecretKey = Base64.getEncoder().encodeToString("wrong-secret:$validTimestamp".toByteArray())
        assertFalse(ApiKeyAuth.isValidApiKey(wrongSecretKey))

        // Test old timestamp (should fail - more than 1 year old)
        val oldTimestamp = "1234567890123" // Year 2009
        val oldApiKey = Base64.getEncoder().encodeToString("$secretKey:$oldTimestamp".toByteArray())
        assertFalse(ApiKeyAuth.isValidApiKey(oldApiKey))
    }

    @Test
    fun testProtectedEndpointWithoutApiKey() = testApplication {
        application {
            module()
        }

        val response = client.get("/api/sheets/test")
        assertEquals(HttpStatusCode.Unauthorized, response.status)

        val responseBody = response.bodyAsText()
        val json = Json.parseToJsonElement(responseBody).jsonObject
        assertEquals("API key required", json["error"]?.jsonPrimitive?.content)
    }

    @Test
    fun testProtectedEndpointWithInvalidApiKey() = testApplication {
        application {
            module()
        }

        val response = client.get("/api/sheets/test") {
            header("X-API-Key", "invalid-key")
        }
        assertEquals(HttpStatusCode.Unauthorized, response.status)

        val responseBody = response.bodyAsText()
        val json = Json.parseToJsonElement(responseBody).jsonObject
        assertEquals("Invalid API key", json["error"]?.jsonPrimitive?.content)
    }

    @Test
    fun testProtectedEndpointWithValidApiKey() = testApplication {
        application {
            module()
        }

        val validApiKey = ApiKeyAuth.generateApiKey()
        val response = client.get("/api/sheets/test") {
            header("X-API-Key", validApiKey)
        }

        // Should not be 401 (passes auth, might get other errors from business logic)
        assertNotEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun testAuthorizationBearerHeader() = testApplication {
        application {
            module()
        }

        val validApiKey = ApiKeyAuth.generateApiKey()
        val response = client.get("/api/sheets/test") {
            header("Authorization", "Bearer $validApiKey")
        }

        // Should not be 401 (passes auth)
        assertNotEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun testPublicEndpointsAccessible() = testApplication {
        application {
            module()
        }

        // Root endpoint should be accessible without API key
        val rootResponse = client.get("/")
        assertEquals(HttpStatusCode.OK, rootResponse.status)

        // Swagger endpoints should be accessible without API key
        val swaggerResponse = client.get("/swagger")
        assertNotEquals(HttpStatusCode.Unauthorized, swaggerResponse.status)

        val openApiResponse = client.get("/openapi.json")
        assertNotEquals(HttpStatusCode.Unauthorized, openApiResponse.status)

        val docsResponse = client.get("/docs")
        assertNotEquals(HttpStatusCode.Unauthorized, docsResponse.status)
    }

    @Test
    fun testNonExistentApiEndpointRequiresAuth() = testApplication {
        application {
            module()
        }

        val response = client.get("/api/nonexistent")
        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }
}