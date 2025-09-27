# Sheet API

A Kotlin REST API built with Ktor for managing Google Sheets data with automatic schema detection and validation.

## Features

| Feature | Description |
|---------|-------------|
| **🔥 Auto Hierarchical Grouping** | Automatically detects merged cell patterns and returns nested JSON structures |
| **📊 Dynamic Sheet ID Support** | Use path parameters to access any Google Sheet without changing environment variables |
| **🔍 Smart Data Detection** | Auto-detects flat vs hierarchical data structures for optimal response format |
| **Google Sheets Integration** | Read and write data to Google Sheets using Google Sheets API v4 |
| **Auto Schema Detection** | Automatically detects data types (integer, double, boolean, string) from existing sheet data |
| **JSON Validation** | Validates POST requests against detected schema with detailed error messages |
| **API Key Authentication** | Secure API protection with Base64-encoded keys and timestamp validation |
| **Pagination Support** | Get paginated data with `per_page` and `offset` parameters (for flat data) |
| **Performance Optimized** | Single API call optimization for faster response times |
| **OpenAPI Documentation** | Interactive Swagger UI available at `/swagger` |
| **CORS Support** | Cross-Origin Resource Sharing enabled for web applications |

## Installation with Docker

You can run the application using the pre-built Docker image from Docker Hub.

1.  **Pull the image:**
    ```bash
    docker pull utsmannn/sheet-api:latest
    ```

2.  **Run the container:**
    ```bash
    docker run -d \
      -p 8910:8910 \
      -e SHEET_ID="your_google_sheet_id" \
      -e API_SECRET_KEY="your-secret-key" \
      -e CREDENTIAL_PATH="/app/credentials/service-account.json" \
      -v /path/on/your/machine/service-account.json:/app/credentials/service-account.json \
      --name sheet-api \
      utsmannn/sheet-api:latest
    ```

    **Important:**
    - Replace `"your_google_sheet_id"` and `"your-secret-key"` with your actual configuration.
    - Replace `/path/on/your/machine/service-account.json` with the absolute path to your Google service account JSON key file on your local machine.
    - The container will restart automatically unless stopped.

### Using Docker Compose

For a more manageable setup, you can use `docker-compose`.

1.  **Create a `docker-compose.yml` file:**
    ```yaml
    version: '3.8'

    services:
      sheet-api:
        image: utsmannn/sheet-api:latest
        container_name: sheet-api-kotlin
        ports:
          - "8910:8910"
        environment:
          - SHEET_ID=${SHEET_ID}
          - API_SECRET_KEY=${API_SECRET_KEY}
          - CREDENTIAL_PATH=/app/credentials/service-account.json
        volumes:
          - ./credentials:/app/credentials:ro
        restart: unless-stopped

    ```

2.  **Create a `.env` file** in the same directory with your secrets:
    ```
    SHEET_ID=your_google_sheet_id
    API_SECRET_KEY=your-secret-key
    ```

3.  **Create a `credentials` directory** and place your `service-account-key.json` file inside it.

4.  **Run the container:**
    ```bash
    docker-compose up -d
    ```

## API Endpoints

> ⚠️ **All API endpoints require authentication** - See [API Authentication](#api-authentication) section for details.

### 🔥 Dynamic Sheet Access

You can access any Google Sheet using two methods:

1. **Default Sheet** (uses `SHEET_ID` environment variable):
   ```
   GET /api/sheets/{sheetName}
   ```

2. **Custom Sheet ID** (override with path parameter):
   ```
   GET /api/sheetId/{sheetId}/{sheetName}
   ```

### Get Sheet Data
```bash
GET /api/sheets/{sheetName}?per_page=10&offset=1
GET /api/sheetId/{sheetId}/{sheetName}?per_page=10&offset=1
# Requires: X-API-Key or Authorization Bearer header
```
Retrieves data from specified Google Sheet with **automatic hierarchical grouping detection**.

**🔥 New Feature: Auto-Grouping**
API automatically detects merged cell patterns in your spreadsheet and returns:
- **Hierarchical JSON**: For data with grouping structure (nested objects)
- **Flat JSON Array**: For regular tabular data with pagination

**Headers:**
- `X-API-Key: YOUR_API_KEY` (required)

**Parameters:**
- `sheetName` (path): Name of the Google Sheet tab
- `sheetId` (path, optional): Specific Google Sheet ID (overrides SHEET_ID env var)
- `per_page` (query): Number of rows per page (default: 10, only for flat data)
- `offset` (query): Starting row number (default: 1, only for flat data)

**Example - Basic Usage:**
```bash
curl -H "X-API-Key: YOUR_API_KEY" \
  "http://localhost:8910/api/sheets/MySheet?per_page=5&offset=1"
```

**Example - Custom Sheet ID:**
```bash
curl -H "X-API-Key: YOUR_API_KEY" \
  "http://localhost:8910/api/sheetId/1BxiMVs0XRA5nFMdKvBdBZjgmUUqptlbs74OgvE2upms/Class%20Data"
```

**Response Types:**

*Flat Data (with pagination):*
```json
[
  {"id": 1, "name": "John", "email": "john@example.com"},
  {"id": 2, "name": "Jane", "email": "jane@example.com"}
]
```
*Headers: `X-Total-Count: 150`*

*Hierarchical Data (auto-detected grouping):*
```json
{
  "Provinces": [
    {
      "name": "DKI Jakarta",
      "data": [
        {
          "District": "Tanah Abang",
          "Population": "87450",
          "Area": "7.39",
          "Density": "11833.6"
        },
        {
          "District": "Menteng",
          "Population": "65420",
          "Area": "5.12",
          "Density": "12777.3"
        }
      ]
    },
    {
      "name": "Jawa Barat",
      "data": [
        {
          "District": "Cicendo",
          "Population": "85420",
          "Area": "12.5",
          "Density": "6833.6"
        }
      ]
    }
  ]
}
```

### Get Sheet Schema
```bash
GET /api/sheets/{sheetName}/schema
GET /api/sheetId/{sheetId}/{sheetName}/schema
# Requires: X-API-Key or Authorization Bearer header
```
Returns the detected schema from sheet headers (A1-Z1) with auto-detected data types.

**Headers:**
- `X-API-Key: YOUR_API_KEY` (required)

**Example:**
```bash
curl -H "X-API-Key: YOUR_API_KEY" \
  "http://localhost:8910/api/sheets/MySheet/schema"
```

**Example with Custom Sheet ID:**
```bash
curl -H "X-API-Key: YOUR_API_KEY" \
  "http://localhost:8910/api/sheetId/1BxiMVs0XRA5nFMdKvBdBZjgmUUqptlbs74OgvE2upms/MySheet/schema"
```

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
```bash
POST /api/sheets/{sheetName}
POST /api/sheetId/{sheetId}/{sheetName}
# Requires: X-API-Key or Authorization Bearer header
```
Appends a new row to the sheet with automatic validation against detected schema.

**Headers:**
- `X-API-Key: YOUR_API_KEY` (required)
- `Content-Type: application/json` (required)

**Request Body:**
```json
{
  "id": 123,
  "name": "John Doe",
  "email": "john@example.com",
  "active": "true"
}
```

**Example:**
```bash
curl -X POST \
  -H "X-API-Key: YOUR_API_KEY" \
  -H "Content-Type: application/json" \
  -d '{"id": 123, "name": "John Doe", "email": "john@example.com", "active": "true"}' \
  "http://localhost:8910/api/sheets/MySheet"
```

**Example with Custom Sheet ID:**
```bash
curl -X POST \
  -H "X-API-Key: YOUR_API_KEY" \
  -H "Content-Type: application/json" \
  -d '{"district": "New District", "population": 75000}' \
  "http://localhost:8910/api/sheetId/1BxiMVs0XRA5nFMdKvBdBZjgmUUqptlbs74OgvE2upms/MySheet"
```

**Success Response:**
```json
{"ok": true}
```

**Validation Error Response:**
```json
{
  "error": "Validation failed",
  "details": ["Field 'email' is required", "Field 'id' must be an integer"]
}
```

## Configuration

Set the following environment variables:

```bash
export SHEET_ID="your_default_google_sheet_id"  # Optional: fallback when no sheetId in path
export CREDENTIAL_PATH="/path/to/service-account-key.json"
export API_SECRET_KEY="your-secret-key-for-api-key-generation"
```

**Note:** `SHEET_ID` is now optional. You can:
- Set it as default for `/api/sheets/{sheetName}` endpoints
- Override it using `/api/sheetId/{sheetId}/{sheetName}` endpoints
- Mix both approaches in the same application

## API Authentication

All API endpoints are protected with API key authentication, except for public endpoints:
- Root (`/`)
- Health checks (`/health*`)
- Documentation (`/swagger*`, `/openapi*`, `/docs*`)

### Security Features

- **Secret Key Validation**: API keys must contain the correct secret key from `API_SECRET_KEY` environment variable
- **Timestamp Validation**: Enforces 13-digit millisecond timestamps to prevent truncation attacks
- **Time Range Validation**: API keys expire after 1 year to prevent replay attacks
- **Multiple Header Support**: Accepts both `X-API-Key` and `Authorization: Bearer` headers

### Generating API Keys

API keys use the format: `base64("{API_SECRET_KEY}:{13_digit_timestamp}")`

#### Option 1: Online Base64 Encoder
1. Get current timestamp in milliseconds from https://currentmillis.com/
2. Create a string: `{API_SECRET_KEY}:{timestamp}`
   - Example: `my-secret-key:1758964134468`
   - ⚠️ **Important**: Timestamp must be exactly 13 digits (milliseconds since epoch)
3. Encode at https://www.base64encode.org/
4. Use the encoded result as your API key

#### Option 2: Command Line (Linux/Mac)
```bash
# Generate current timestamp in milliseconds
timestamp=$(date +%s)000
printf "my-secret-key:$timestamp" | base64
```

#### Option 3: Node.js
```javascript
const secretKey = process.env.API_SECRET_KEY || 'my-secret-key';
const timestamp = Date.now(); // Already in milliseconds
const apiKey = Buffer.from(`${secretKey}:${timestamp}`).toString('base64');
console.log(apiKey);
```

#### Option 4: Python
```python
import base64
import time
import os

secret_key = os.getenv('API_SECRET_KEY', 'my-secret-key')
timestamp = int(time.time() * 1000)  # Convert to milliseconds
api_key = base64.b64encode(f"{secret_key}:{timestamp}".encode()).decode()
print(api_key)
```

### Using API Keys

Include the API key in your requests using either header format:

**Option 1: X-API-Key Header**
```bash
curl -H "X-API-Key: YOUR_API_KEY" http://localhost:8910/api/sheets/MySheet
```

**Option 2: Authorization Bearer Header**
```bash
curl -H "Authorization: Bearer YOUR_API_KEY" http://localhost:8910/api/sheets/MySheet
```

### Authentication Responses

| Status | Response | Description |
|--------|----------|-------------|
| 200+ | Normal response | ✅ Valid API key, request processed |
| 401 | `{"error": "API key required"}` | ❌ No API key provided |
| 401 | `{"error": "Invalid API key"}` | ❌ Invalid, malformed, or expired API key |

### Common Authentication Errors

| Issue | Cause | Solution |
|-------|-------|----------|
| Invalid API key | Wrong secret key | Check `API_SECRET_KEY` environment variable |
| Invalid API key | Timestamp not 13 digits | Use `Date.now()`, `date +%s000`, or https://currentmillis.com/ |
| Invalid API key | API key expired | Generate new key (keys expire after 1 year) |
| Invalid API key | Malformed Base64 | Ensure proper Base64 encoding without newlines |

💡 **Quick Tip**: For manual testing, get current timestamp from https://currentmillis.com/ and encode at https://www.base64encode.org/

## 🔥 Hierarchical Data Grouping

The API automatically detects merged cell patterns in your Google Sheets and converts them into nested JSON structures.

### How Auto-Detection Works

1. **Pattern Recognition**: Analyzes your spreadsheet for columns with merged cells
2. **Smart Grouping**: Identifies parent-child relationships automatically
3. **Response Format**: Returns hierarchical JSON instead of flat arrays

### Example Input (Google Sheet)
```
Province    | Regency      | District     | Population
DKI Jakarta | Jakarta Pusat| Tanah Abang  | 87450
            |              | Menteng      | 65420
            | Jakarta Barat| Tambora      | 95480
Jawa Barat  | Bandung      | Cicendo      | 85420
```

### Example Output (Auto-Grouped JSON)
```json
{
  "Provinces": [
    {
      "name": "DKI Jakarta",
      "data": [
        {
          "Regency": "Jakarta Pusat",
          "District": "Tanah Abang",
          "Population": "87450",
          "Area": "7.39",
          "Density": "11833.6"
        },
        {
          "Regency": "Jakarta Pusat",
          "District": "Menteng",
          "Population": "65420",
          "Area": "5.12",
          "Density": "12777.3"
        },
        {
          "Regency": "Jakarta Barat",
          "District": "Tambora",
          "Population": "95480",
          "Area": "11.2",
          "Density": "8525.0"
        }
      ]
    },
    {
      "name": "Jawa Barat",
      "data": [
        {
          "Regency": "Bandung",
          "District": "Cicendo",
          "Population": "85420",
          "Area": "12.5",
          "Density": "6833.6"
        }
      ]
    }
  ]
}
```

### Performance Optimization

- **Single API Call**: Optimized to fetch and analyze data in one request
- **Smart Detection**: Inline pattern recognition without extra API calls
- **Fallback Support**: Automatically falls back to flat JSON for non-grouped data

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
2025-09-28 00:25:41.273 [main] INFO  Application - Application started in 0.85 seconds.
2025-09-28 00:25:41.352 [DefaultDispatcher-worker-1] INFO  Application - Responding at http://0.0.0.0:8910
```

### Testing the API

Once running, you can test the hierarchical grouping feature:

```bash
# Test with your data
curl -H "X-API-Key: YOUR_API_KEY" \
  "http://localhost:8910/api/sheets/YourSheetName"

# Test with custom sheet ID
curl -H "X-API-Key: YOUR_API_KEY" \
  "http://localhost:8910/api/sheetId/CUSTOM_SHEET_ID/YourSheetName"
```

## Landing Page

The API includes a built-in React TypeScript landing page that provides:

- **Interactive API Key Generator** - Generate and copy API keys directly from the browser
- **Live API Demo** - Test endpoints with real requests and responses
- **Code Examples** - Ready-to-use code snippets for cURL, JavaScript, and Python
- **Feature Overview** - Complete documentation and feature showcase

### Accessing the Landing Page

Once the server is running, visit: **http://localhost:8910/**

The landing page provides a user-friendly interface for:
- Understanding API capabilities
- Generating API keys without command line tools
- Testing API endpoints interactively
- Getting integration code examples

### Development

The landing page is built with:
- **React** with TypeScript for type safety
- **Tailwind CSS** for responsive styling
- **Vite** for fast development and building

To rebuild the landing page after making changes:

```bash
# Build and deploy the landing page
./build-frontend.sh
```

Or manually:

```bash
cd landingpage
npm install
npm run build
cp -r dist/* ../src/main/resources/static/
```

## License

This project is licensed under the Apache License 2.0 - see the [LICENSE](LICENSE) file for details.

