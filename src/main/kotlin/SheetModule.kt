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
     * Get grouped/hierarchical data from specified sheet
     * Groups rows by columns that have merged/empty cells
     * @param sheetName sheet name (tab)
     * @return JsonObject with hierarchical structure
     */
    suspend fun getGroupedJson(sheetName: String): JsonObject = withContext(Dispatchers.IO) {

        // Get all data from sheet
        val headerRow = sheets.spreadsheets().values()
            .get(spreadsheetId, "$sheetName!1:1")
            .execute()
            .getValues()?.firstOrNull() ?: emptyList()

        val allData = sheets.spreadsheets().values()
            .get(spreadsheetId, "$sheetName!A:Z")
            .execute()
            .getValues().orEmpty()

        if (allData.size <= 1) {
            return@withContext buildJsonObject {}
        }

        val dataRows = allData.drop(1) // Skip header

        // Step 1: Detect root vs grouping columns
        val groupingColumns = mutableSetOf<Int>()
        val rootColumns = mutableSetOf<Int>()

        headerRow.forEachIndexed { colIndex, _ ->
            val firstRowValue = dataRows.firstOrNull()?.getOrNull(colIndex)?.toString() ?: ""

            // Count non-empty values in this column across ALL data rows
            val nonEmptyCount = dataRows.count { row ->
                val cellValue = row.getOrNull(colIndex)?.toString() ?: ""
                cellValue.isNotBlank()
            }

            val totalRows = dataRows.size

            when {
                // Root column: has value in first row AND only 1 occurrence in total (only in first row)
                firstRowValue.isNotBlank() && nonEmptyCount == 1 -> {
                    rootColumns.add(colIndex)
                }
                // Grouping column: has value in first row AND has multiple but not all occurrences (merged pattern)
                firstRowValue.isNotBlank() && nonEmptyCount > 1 && nonEmptyCount < totalRows -> {
                    groupingColumns.add(colIndex)
                }
                // Content columns: all or most rows have values (not grouped, not root)
                // These will be handled as content within groups
            }
        }

        // Step 2: Build result object
        val result = buildJsonObject {

            // Add root level fields (from first row)
            val firstRow = dataRows.firstOrNull() ?: return@buildJsonObject
            rootColumns.forEach { colIndex ->
                val fieldName = headerRow.getOrNull(colIndex)?.toString() ?: ""
                val fieldValue = firstRow.getOrNull(colIndex)?.toString() ?: ""
                if (fieldName.isNotBlank()) {
                    put(fieldName, fieldValue)
                }
            }

            // Step 3: Process grouping columns (should only be one main grouping column)
            if (groupingColumns.isNotEmpty()) {
                // Take the first grouping column (assuming single-level grouping for now)
                val groupColIndex = groupingColumns.first()
                val groupFieldName = headerRow.getOrNull(groupColIndex)?.toString() ?: ""

                val groups = mutableMapOf<String, MutableList<JsonObject>>()
                var currentGroupName = ""

                dataRows.forEach { row ->
                    val groupValue = row.getOrNull(groupColIndex)?.toString() ?: ""

                    // Update current group if we have a new group value
                    if (groupValue.isNotBlank()) {
                        currentGroupName = groupValue
                    }

                    // Add row to current group (only include non-root, non-group columns)
                    if (currentGroupName.isNotBlank()) {
                        val rowData = buildJsonObject {
                            headerRow.forEachIndexed { colIndex, colName ->
                                // Skip root columns and the grouping column itself
                                if (colIndex !in rootColumns && colIndex !in groupingColumns) {
                                    val cellValue = row.getOrNull(colIndex)?.toString() ?: ""
                                    if (cellValue.isNotBlank()) {
                                        put(colName.toString(), cellValue)
                                    }
                                }
                            }
                        }

                        // Only add if rowData has content
                        if (rowData.isNotEmpty()) {
                            groups.getOrPut(currentGroupName) { mutableListOf() }.add(rowData)
                        }
                    }
                }

                // Add groups to result with plural name
                val pluralFieldName = "${groupFieldName}s" // Simple pluralization
                put(pluralFieldName, buildJsonArray {
                    groups.forEach { (groupName, items) ->
                        add(buildJsonObject {
                            put("name", groupName)
                            put("data", buildJsonArray {
                                items.forEach { add(it) }
                            })
                        })
                    }
                })
            }
        }

        result
    }

    /**
     * Check if sheet has grouping pattern (merged cells)
     * @param sheetName sheet name (tab)
     * @return Boolean true if has grouping pattern
     */
    private suspend fun hasGroupingPattern(sheetName: String): Boolean = withContext(Dispatchers.IO) {
        val headerRow = sheets.spreadsheets().values()
            .get(spreadsheetId, "$sheetName!1:1")
            .execute()
            .getValues()?.firstOrNull() ?: emptyList()

        val allData = sheets.spreadsheets().values()
            .get(spreadsheetId, "$sheetName!A:Z")
            .execute()
            .getValues().orEmpty()

        if (allData.size <= 1) return@withContext false

        val dataRows = allData.drop(1)

        // Check if any column has grouping pattern
        headerRow.indices.any { colIndex ->
            val firstRowValue = dataRows.firstOrNull()?.getOrNull(colIndex)?.toString() ?: ""
            val nonEmptyCount = dataRows.count { row ->
                val cellValue = row.getOrNull(colIndex)?.toString() ?: ""
                cellValue.isNotBlank()
            }
            val totalRows = dataRows.size

            // Grouping pattern: has value in first row AND has multiple but not all occurrences
            firstRowValue.isNotBlank() && nonEmptyCount > 1 && nonEmptyCount < totalRows
        }
    }

    /**
     * Get data with automatic format detection (grouped or flat) - OPTIMIZED
     * @param sheetName sheet name (tab)
     * @param perPage number of data rows (excluding header) - only for flat data
     * @param offset data row starting from 1 - only for flat data
     * @return JsonElement - JsonObject for grouped, JsonArray for flat
     */
    suspend fun getAutoFormattedData(
        sheetName: String,
        perPage: Int = 10,
        offset: Int = 1
    ): JsonElement = withContext(Dispatchers.IO) {

        // Single API call to get all data - then decide format
        val headerRow = sheets.spreadsheets().values()
            .get(spreadsheetId, "$sheetName!1:1")
            .execute()
            .getValues()?.firstOrNull() ?: emptyList()

        val allData = sheets.spreadsheets().values()
            .get(spreadsheetId, "$sheetName!A:Z")
            .execute()
            .getValues().orEmpty()

        if (allData.size <= 1) {
            return@withContext buildJsonArray {}
        }

        val dataRows = allData.drop(1)

        // Check for grouping pattern inline
        val hasGrouping = headerRow.indices.any { colIndex ->
            val firstRowValue = dataRows.firstOrNull()?.getOrNull(colIndex)?.toString() ?: ""
            val nonEmptyCount = dataRows.count { row ->
                val cellValue = row.getOrNull(colIndex)?.toString() ?: ""
                cellValue.isNotBlank()
            }
            val totalRows = dataRows.size

            // Grouping pattern: has value in first row AND has multiple but not all occurrences
            firstRowValue.isNotBlank() && nonEmptyCount > 1 && nonEmptyCount < totalRows
        }

        if (hasGrouping) {
            // Build grouped format using already fetched data
            buildGroupedJsonFromData(headerRow, dataRows)
        } else {
            // Build flat format with pagination
            buildFlatJsonFromData(headerRow, dataRows, perPage, offset)
        }
    }

    /**
     * Build grouped JSON from already fetched data
     */
    private fun buildGroupedJsonFromData(headerRow: List<Any>, dataRows: List<List<Any>>): JsonObject {
        // Detect columns (same logic as getGroupedJson)
        val groupingColumns = mutableSetOf<Int>()
        val rootColumns = mutableSetOf<Int>()

        headerRow.forEachIndexed { colIndex, _ ->
            val firstRowValue = dataRows.firstOrNull()?.getOrNull(colIndex)?.toString() ?: ""
            val nonEmptyCount = dataRows.count { row ->
                val cellValue = row.getOrNull(colIndex)?.toString() ?: ""
                cellValue.isNotBlank()
            }
            val totalRows = dataRows.size

            when {
                firstRowValue.isNotBlank() && nonEmptyCount == 1 -> {
                    rootColumns.add(colIndex)
                }
                firstRowValue.isNotBlank() && nonEmptyCount > 1 && nonEmptyCount < totalRows -> {
                    groupingColumns.add(colIndex)
                }
            }
        }

        return buildJsonObject {
            // Add root level fields
            val firstRow = dataRows.firstOrNull() ?: return@buildJsonObject
            rootColumns.forEach { colIndex ->
                val fieldName = headerRow.getOrNull(colIndex)?.toString() ?: ""
                val fieldValue = firstRow.getOrNull(colIndex)?.toString() ?: ""
                if (fieldName.isNotBlank()) {
                    put(fieldName, fieldValue)
                }
            }

            // Process grouping columns
            if (groupingColumns.isNotEmpty()) {
                val groupColIndex = groupingColumns.first()
                val groupFieldName = headerRow.getOrNull(groupColIndex)?.toString() ?: ""

                val groups = mutableMapOf<String, MutableList<JsonObject>>()
                var currentGroupName = ""

                dataRows.forEach { row ->
                    val groupValue = row.getOrNull(groupColIndex)?.toString() ?: ""

                    if (groupValue.isNotBlank()) {
                        currentGroupName = groupValue
                    }

                    if (currentGroupName.isNotBlank()) {
                        val rowData = buildJsonObject {
                            headerRow.forEachIndexed { colIndex, colName ->
                                if (colIndex !in rootColumns && colIndex !in groupingColumns) {
                                    val cellValue = row.getOrNull(colIndex)?.toString() ?: ""
                                    if (cellValue.isNotBlank()) {
                                        put(colName.toString(), cellValue)
                                    }
                                }
                            }
                        }

                        if (rowData.isNotEmpty()) {
                            groups.getOrPut(currentGroupName) { mutableListOf() }.add(rowData)
                        }
                    }
                }

                val pluralFieldName = "${groupFieldName}s"
                put(pluralFieldName, buildJsonArray {
                    groups.forEach { (groupName, items) ->
                        add(buildJsonObject {
                            put("name", groupName)
                            put("data", buildJsonArray {
                                items.forEach { add(it) }
                            })
                        })
                    }
                })
            }
        }
    }

    /**
     * Build flat JSON array from already fetched data with pagination
     */
    private fun buildFlatJsonFromData(headerRow: List<Any>, dataRows: List<List<Any>>, perPage: Int, offset: Int): JsonArray {
        val startIndex = offset - 1
        val endIndex = minOf(startIndex + perPage, dataRows.size)
        val paginatedRows = if (startIndex < dataRows.size) dataRows.subList(startIndex, endIndex) else emptyList()

        return buildJsonArray {
            paginatedRows.forEach { row ->
                add(buildJsonObject {
                    headerRow.forEachIndexed { i, col ->
                        val cellValue = row.getOrNull(i)?.toString() ?: ""
                        put(col.toString(), cellValue)
                    }
                })
            }
        }
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