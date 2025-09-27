package com.utsman

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.sheets.v4.Sheets
import com.google.api.services.sheets.v4.model.AppendValuesResponse
import com.google.api.services.sheets.v4.model.Sheet
import com.google.api.services.sheets.v4.model.ValueRange
import com.google.auth.http.HttpCredentialsAdapter
import com.google.auth.oauth2.GoogleCredentials
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*
import java.io.File

class SheetModule(
    private val spreadsheetId: String,
    private val sheets: Sheets,
) {
    /**
     * Get paginated data slice from specified sheet
     * @param sheetName sheet name (tab)
     * @param perPage number of data rows (excluding header)
     * @param offset data row starting from 1 (1 means 2nd row in file)
     * @return Pair<String, Int> -> jsonArrayString & totalRows
     */
    suspend fun getJsonSlice(
        sheetName: String,
        perPage: Int = 10,
        offset: Int = 1
    ): Pair<JsonArray, Int> = withContext(Dispatchers.IO) {

        val totalRows = sheets.spreadsheets().values()
            .get(spreadsheetId, "$sheetName!A:A")
            .execute()
            .getValues()?.size ?: 0

        val startRow = offset + 1
        val endRow = minOf(startRow + perPage - 1, totalRows.coerceAtLeast(1))
        val dataRange = "$sheetName!A$startRow:Z$endRow"

        val headerRow = sheets.spreadsheets().values()
            .get(spreadsheetId, "$sheetName!1:1")
            .execute()
            .getValues()?.firstOrNull() ?: emptyList()

        val data = sheets.spreadsheets().values()
            .get(spreadsheetId, dataRange)
            .execute()
            .getValues().orEmpty()

        val arr = buildJsonArray {
            data.forEach { row ->
                add(buildJsonObject {
                    headerRow.forEachIndexed { i, col ->
                        val cellValue = row.getOrNull(i)?.toString() ?: ""
                        put(col.toString(), cellValue)
                    }
                })
            }
        }

        Pair(arr, totalRows)
    }

    /**
     * Detect data type from string value
     * @param value string value to detect
     * @return String data type: "integer", "double", "boolean", "string"
     */
    private fun detectDataType(value: String): String {
        if (value.isBlank()) return "string"

        value.toIntOrNull()?.let { return "integer" }
        value.toDoubleOrNull()?.let { return "double" }

        if (value.lowercase() in listOf("true", "false", "1", "0", "yes", "no")) {
            return "boolean"
        }

        return "string"
    }

    /**
     * Get schema from header row (A1-Z1) with auto-detected data types
     * @param sheetName sheet name (tab)
     * @return Map<String, String> -> field name to detected type
     */
    suspend fun getSheetSchema(sheetName: String): Map<String, String> = withContext(Dispatchers.IO) {
        val headerRow = sheets.spreadsheets().values()
            .get(spreadsheetId, "$sheetName!1:1")
            .execute()
            .getValues()?.firstOrNull() ?: emptyList()

        val sampleData = sheets.spreadsheets().values()
            .get(spreadsheetId, "$sheetName!2:6")
            .execute()
            .getValues().orEmpty()

        headerRow.mapIndexed { index, col ->
            val columnName = col.toString()

            val sampleValues = sampleData.mapNotNull { row ->
                row.getOrNull(index)?.toString()?.takeIf { it.isNotBlank() }
            }

            val typeVotes = sampleValues.map { detectDataType(it) }
            val detectedType = typeVotes.groupingBy { it }
                .eachCount()
                .maxByOrNull { it.value }?.key ?: "string"

            columnName to detectedType
        }.toMap()
    }

    /**
     * Validate value against expected type
     * @param value JsonElement value to validate
     * @param expectedType expected data type
     * @return String? error message, null if valid
     */
    private fun validateValueType(value: JsonElement, expectedType: String): String? {
        val stringValue = when (value) {
            is JsonPrimitive if value.isString -> value.content
            is JsonPrimitive -> value.toString().removeSurrounding("\"")
            else -> return "Value must be a primitive type"
        }

        return when (expectedType) {
            "integer" -> {
                stringValue.toIntOrNull() ?: return "Value '$stringValue' is not a valid integer"
                null
            }

            "double" -> {
                stringValue.toDoubleOrNull() ?: return "Value '$stringValue' is not a valid number"
                null
            }

            "boolean" -> {
                val lowerValue = stringValue.lowercase()
                if (lowerValue !in listOf("true", "false", "1", "0", "yes", "no")) {
                    return "Value '$stringValue' is not a valid boolean (use: true, false, 1, 0, yes, no)"
                }
                null
            }

            "string" -> null
            else -> null
        }
    }

    /**
     * Validate JSON object against schema with type checking
     * @param jsonObject JSON object to validate
     * @param schema Map from field name to type
     * @return List<String> error messages, empty if valid
     */
    fun validateJsonSchema(jsonObject: JsonObject, schema: Map<String, String>): List<String> {
        val errors = mutableListOf<String>()

        schema.forEach { (requiredField, expectedType) ->
            if (!jsonObject.containsKey(requiredField)) {
                errors.add("Missing required field: $requiredField")
            } else {
                val value = jsonObject[requiredField]!!
                validateValueType(value, expectedType)?.let { error ->
                    errors.add("Field '$requiredField': $error")
                }
            }
        }

        jsonObject.keys.forEach { fieldName ->
            if (!schema.containsKey(fieldName)) {
                errors.add("Unknown field: $fieldName")
            }
        }

        return errors
    }

    /**
     * Insert new row to sheet with schema validation
     * @param jsonString raw json object (keys must match header)
     */
    suspend fun appendRowFromJson(sheetName: String, jsonString: String): AppendValuesResponse? =
        withContext(Dispatchers.IO) {

            val obj = Json.parseToJsonElement(jsonString).jsonObject

            val headerRow = sheets.spreadsheets().values()
                .get(spreadsheetId, "$sheetName!1:1")
                .execute()
                .getValues()?.firstOrNull() ?: emptyList()

            val row = headerRow.map { col ->
                obj[col.toString()]?.jsonPrimitive?.content ?: ""
            }

            val body = ValueRange().setValues(listOf(row))
            sheets.spreadsheets().values()
                .append(spreadsheetId, "$sheetName!A:C", body)
                .setValueInputOption("RAW")
                .execute()
        }

    /**
     * Get list of sheet names (tabs) in the spreadsheet
     * @return List of sheet titles
     */
    suspend fun getSheetNames(): List<String> = withContext(Dispatchers.IO) {
        val spreadsheet = sheets.spreadsheets().get(spreadsheetId).execute()
        spreadsheet.sheets.mapNotNull { it.properties?.title }
    }
}