# Sheet API

A Kotlin REST API built with Ktor for managing Google Sheets data with automatic schema detection and validation.

## Features

| Feature | Description |
|---------|-------------|
| **🔥 Auto Hierarchical Grouping** | Automatically detects merged cell patterns and returns nested JSON structures. Merges scattered data into unique parent groups. |
| **✍️ Smart Create & Update** | Use `POST` and `PATCH` with query parameters to add or modify deeply nested data. |
| **📊 Dynamic Sheet ID Support** | Use path parameters to access any Google Sheet without changing environment variables. |
| **🔍 Smart Data Detection** | Auto-detects flat vs hierarchical data structures for optimal response format. |
| **Google Sheets Integration** | Read and write data to Google Sheets using Google Sheets API v4. |
| **Auto Schema Detection** | Automatically detects data types (integer, double, boolean, string) from existing sheet data. |
| **API Key Authentication** | Secure API protection with Base64-encoded keys and timestamp validation. |
| **Pagination Support** | Get paginated data with `per_page` and `offset` parameters (for flat data). |
| **OpenAPI Documentation** | Interactive Swagger UI available at `/`. |

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

### Dynamic Sheet Access

You can access any Google Sheet using two methods:

1. **Default Sheet** (uses `SHEET_ID` environment variable):
   ```
   /api/sheets/{sheetName}
   ```

2. **Custom Sheet ID** (override with path parameter):
   ```
   /api/sheetId/{sheetId}/{sheetName}
   ```

---


### 1. Get Sheet Data (`GET`)
Retrieves data from the sheet with **automatic hierarchical grouping**. If it detects data in the same group scattered across the sheet, it intelligently **merges them** into a single parent object.

**Example - Get All Data from a Sheet:**
```bash
curl -H "X-API-Key: YOUR_API_KEY" \
  "http://localhost:8910/api/sheetId/{sheetId}/{sheetName}"
```

---


### 2. Add Data (`POST`)
Appends a new row to the sheet. This endpoint has two modes:

- **Append Root Item**: If called without query parameters, it adds a new top-level entry.
- **Append Nested Item**: If called with query parameters, it adds a new item inside an existing group.

**Example - Add a New Root Item:**
```bash
curl -X POST \
  -H "X-API-Key: YOUR_API_KEY" \
  -H "Content-Type: application/json" \
  -d '{"title": "New Course", "author": "admin"}' \
  "http://localhost:8910/api/sheetId/{sheetId}/{sheetName}"
```

**Example - Add a Nested Item to an Existing Group:**

This adds a new `content_name` under the group where `curriculums` is "Pengenalan Python".
```bash
curl -X POST \
  -H "X-API-Key: YOUR_API_KEY" \
  -H "Content-Type: application/json" \
  -d '{"content_name": "New Content", "content_description": "..."}' \
  "http://localhost:8910/api/sheetId/{sheetId}/{sheetName}?curriculums=Pengenalan%20Python"
```

---


### 3. Update Data (`PATCH`)
Partially updates a single field in an existing row. This endpoint also has two modes:

- **Update Root Item**: If called without query parameters, it updates a field in the first top-level entry.
- **Update Nested Item**: If called with query parameters, it uses them as a key to find and update a specific nested item.

**Example - Update a Field in the Root Item:**
```bash
curl -X PATCH \
  -H "X-API-Key: YOUR_API_KEY" \
  -H "Content-Type: application/json" \
  -d '{"description": "A new updated description."}' \
  "http://localhost:8910/api/sheetId/{sheetId}/{sheetName}"
```

**Example - Update a Field in a Nested Item:**

This finds the row where `curriculums` is "Fungsi dan Modul" AND `content_name` is "Parameter dan Argumen", then updates its `content_url`.
```bash
curl -X PATCH \
  -H "X-API-Key: YOUR_API_KEY" \
  -H "Content-Type: application/json" \
  -d '{"content_url": "https://new.video.url/12345"}' \
  "http://localhost:8910/api/sheetId/{sheetId}/{sheetName}?curriculums=Fungsi%20dan%20Modul&content_name=Parameter%20dan%20Argumen"
```

---


### 4. Get Sheet Schema (`GET`)
Returns the detected schema from sheet headers (A1-Z1) with auto-detected data types.

**Example:**
```bash
curl -H "X-API-Key: YOUR_API_KEY" \
  "http://localhost:8910/api/sheetId/{sheetId}/{sheetName}/schema"
```

## Configuration

Set the following environment variables:

```bash
export SHEET_ID="your_default_google_sheet_id"  # Optional: fallback when no sheetId in path
export CREDENTIAL_PATH="/path/to/service-account-key.json"
export API_SECRET_KEY="your-secret-key-for-api-key-generation"
```

## API Authentication

All API endpoints are protected with API key authentication, except for public endpoints:
- Root (`/`)
- Health checks (`/health*`)
- Documentation (`/swagger*`, `/openapi*`, `/docs*`)

### Generating API Keys

API keys use the format: `base64("{API_SECRET_KEY}:{13_digit_timestamp}")`

#### Option 1: Command Line (Linux/Mac)
```bash
# Generate current timestamp in milliseconds
timestamp=$(date +%s)000
printf "my-secret-key:$timestamp" | base64
```

#### Option 2: Node.js
```javascript
const secretKey = process.env.API_SECRET_KEY || 'my-secret-key';
const timestamp = Date.now(); // Already in milliseconds
const apiKey = Buffer.from(`${secretKey}:${timestamp}`).toString('base64');
console.log(apiKey);
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

## Landing Page

The API includes a built-in React TypeScript landing page that provides:

- **Interactive API Key Generator** - Generate and copy API keys directly from the browser
- **Live API Demo** - Test endpoints with real requests and responses
- **Code Examples** - Ready-to-use code snippets for cURL, JavaScript, and Python
- **Feature Overview** - Complete documentation and feature showcase

To rebuild the landing page after making changes:

```bash
# Build and deploy the landing page
./build-frontend.sh
```

## License

This project is licensed under the Apache License 2.0 - see the [LICENSE](LICENSE) file for details.
