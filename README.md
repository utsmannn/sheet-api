# Sheet API

A Kotlin REST API built with Ktor for managing Google Sheets data with automatic schema detection and validation.

## Features

| Feature | Description |
|---------|-------------|
| **Google Sheets Integration** | Read and write data to Google Sheets using Google Sheets API v4 |
| **Auto Schema Detection** | Automatically detects data types (integer, double, boolean, string) from existing sheet data |
| **JSON Validation** | Validates POST requests against detected schema with detailed error messages |
| **Pagination Support** | Get paginated data with `per_page` and `offset` parameters |
| **OpenAPI Documentation** | Interactive Swagger UI available at `/swagger` |
| **CORS Support** | Cross-Origin Resource Sharing enabled for web applications |

## API Endpoints

### Get Sheet Data
```
GET /api/sheets/{sheetName}?per_page=10&offset=1
```
Retrieves paginated data from specified Google Sheet.

**Parameters:**
- `sheetName` (path): Name of the Google Sheet tab
- `per_page` (query): Number of rows per page (default: 10)
- `offset` (query): Starting row number (default: 1)

### Get Sheet Schema
```
GET /api/sheets/{sheetName}/schema
```
Returns the detected schema from sheet headers (A1-Z1) with auto-detected data types.

**Response:**
```json
{
  "sheetName": "MySheet",
  "fields": {
    "id": {"type": "integer", "required": true},
    "name": {"type": "string", "required": true},
    "email": {"type": "string", "required": true},
    "active": {"type": "boolean", "required": true}
  }
}
```

### Add New Row
```
POST /api/sheets/{sheetName}
```
Appends a new row to the sheet with automatic validation against detected schema.

**Request Body:**
```json
{
  "id": 123,
  "name": "John Doe",
  "email": "john@example.com",
  "active": "true"
}
```

## Configuration

Set the following environment variables:

```bash
export SHEET_ID="your_google_sheet_id"
export CREDENTIAL_PATH="/path/to/service-account-key.json"
```

## Data Type Detection

The API automatically detects column data types by analyzing sample data:

- **Integer**: Values that can be parsed as integers (e.g., "123", "0")
- **Double**: Values that can be parsed as floating-point numbers (e.g., "12.34", "0.5")
- **Boolean**: Values like "true", "false", "1", "0", "yes", "no"
- **String**: Default fallback for all other values

## Ktor Framework Features

| Name | Description |
|------|-------------|
| [Routing](https://start.ktor.io/p/routing) | Provides a structured routing DSL |
| [OpenAPI](https://start.ktor.io/p/openapi) | Serves OpenAPI documentation |
| [Compression](https://start.ktor.io/p/compression) | Compresses responses using encoding algorithms like GZIP |
| [Content Negotiation](https://start.ktor.io/p/content-negotiation) | Provides automatic content conversion according to Content-Type and Accept headers |
| [kotlinx.serialization](https://start.ktor.io/p/kotlinx-serialization) | Handles JSON serialization using kotlinx.serialization library |
| [CORS](https://start.ktor.io/p/cors) | Enables Cross-Origin Resource Sharing (CORS) |

## Building & Running

To build or run the project, use one of the following tasks:

| Task                          | Description                                                          |
| -------------------------------|---------------------------------------------------------------------- |
| `./gradlew test`              | Run the tests                                                        |
| `./gradlew build`             | Build everything                                                     |
| `buildFatJar`                 | Build an executable JAR of the server with all dependencies included |
| `buildImage`                  | Build the docker image to use with the fat JAR                       |
| `publishImageToLocalRegistry` | Publish the docker image locally                                     |
| `run`                         | Run the server                                                       |
| `runDocker`                   | Run using the local docker image                                     |

If the server starts successfully, you'll see the following output:

```
2024-12-04 14:32:45.584 [main] INFO  Application - Application started in 0.303 seconds.
2024-12-04 14:32:45.682 [main] INFO  Application - Responding at http://0.0.0.0:8080
```

