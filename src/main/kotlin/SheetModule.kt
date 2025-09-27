package com.utsman

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.sheets.v4.Sheets
import com.google.api.services.sheets.v4.model.ValueRange
import com.google.auth.http.HttpCredentialsAdapter
import com.google.auth.oauth2.GoogleCredentials
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*
import java.io.File

object SheetModule {

    /* ---------- config ---------- */
    private val spreadsheetId = System.getenv("SHEET_ID")
    private val credentialFile = File(System.getenv("CREDENTIAL_PATH"))
        .also { require(it.exists()) { "credential file not found at ${it.absolutePath}" } }

    private val transport = GoogleNetHttpTransport.newTrustedTransport()
    private val jsonFactory = GsonFactory.getDefaultInstance()

    private val credentials = GoogleCredentials.fromStream(credentialFile.inputStream())
        .createScoped(listOf("https://www.googleapis.com/auth/spreadsheets"))

    private val sheets = Sheets.Builder(transport, jsonFactory, HttpCredentialsAdapter(credentials))
        .setApplicationName("KtorSheetAPI")
        .build()

    /* ---------- public helpers ---------- */

    /**
     * Ambil slice data dari sheet tertentu + paging
     * @param sheetName  nama sheet (tab)
     * @param perPage    jumlah baris data (bukan termasuk header)
     * @param offset     baris data mulai dari 1 (1 berarti baris ke-2 di file)
     * @return Pair<String, Int> -> jsonArrayString & totalRows
     */
    suspend fun getJsonSlice(
        sheetName: String,
        perPage: Int = 10,
        offset: Int = 1
    ): Pair<String, Int> = withContext(Dispatchers.IO) {

        /* 1. hitung total baris (kolom A sebagai acuan) */
        val totalRows = sheets.spreadsheets().values()
            .get(spreadsheetId, "$sheetName!A:A")
            .execute()
            .getValues()?.size ?: 0

        /* 2. tentukan range yg akan diambil */
        val startRow = offset + 1                 // baris 1 = header
        val endRow   = minOf(startRow + perPage - 1, totalRows.coerceAtLeast(1))
        val dataRange = "$sheetName!A$startRow:Z$endRow"

        /* 3. ambil header (baris 1) */
        val headerRow = sheets.spreadsheets().values()
            .get(spreadsheetId, "$sheetName!1:1")
            .execute()
            .getValues()?.firstOrNull() ?: emptyList()

        /* 4. ambil data sesuai range */
        val data = sheets.spreadsheets().values()
            .get(spreadsheetId, dataRange)
            .execute()
            .getValues().orEmpty()

        /* 5. buat JsonArray dinamis */
        val arr = buildJsonArray {
            data.forEach { row ->
                add(buildJsonObject {
                    headerRow.forEachIndexed { i, col ->
                        put(col.toString(), row.getOrNull(i).toString())
                    }
                })
            }
        }

        Pair(arr.toString(), totalRows)
    }

    /**
     * Insert 1 baris baru ke sheetName!A:C
     * @param jsonString raw json object (key harus sesuai header)
     */
    suspend fun appendRowFromJson(sheetName: String, jsonString: String) =
        withContext(Dispatchers.IO) {

            val obj = Json.parseToJsonElement(jsonString).jsonObject

            /* susun kolom sesuai urutan header A,B,C */
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
}