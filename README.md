# Sheet API

A Kotlin REST API built with Ktor for managing Google Sheets data with automatic schema detection and validation.

## Features

| Feature | Description |
|---------|-------------|
| **Google Sheets Integration** | Read and write data to Google Sheets using Google Sheets API v4 |
| **Auto Schema Detection** | Automatically detects data types (integer, double, boolean, string) from existing sheet data |
| **JSON Validation** | Validates POST requests against detected schema with detailed error messages |
| **API Key Authentication** | Secure API protection with Base64-encoded keys and timestamp validation |
| **Pagination Support** | Get paginated data with `per_page` and `offset` parameters |
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

### Get Sheet Data
```bash
GET /api/sheets/{sheetName}?per_page=10&offset=1
# Requires: X-API-Key or Authorization Bearer header
```
Retrieves paginated data from specified Google Sheet.

**Headers:**
- `X-API-Key: YOUR_API_KEY` (required)

**Parameters:**
- `sheetName` (path): Name of the Google Sheet tab
- `per_page` (query): Number of rows per page (default: 10)
- `offset` (query): Starting row number (default: 1)

**Example:**
```bash
curl -H "X-API-Key: YOUR_API_KEY" \
  "http://localhost:8910/api/sheets/MySheet?per_page=5&offset=1"
```

### Get Sheet Schema
```bash
GET /api/sheets/{sheetName}/schema
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
export SHEET_ID="your_google_sheet_id"
export CREDENTIAL_PATH="/path/to/service-account-key.json"
export API_SECRET_KEY="your-secret-key-for-api-key-generation"
```

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
2024-12-04 14:32:45.682 [main] INFO  Application - Responding at http://0.0.0.0:8910
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

