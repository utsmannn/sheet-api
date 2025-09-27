import { useState } from 'react'

const examples = {
  curl: {
    title: 'cURL',
    code: `# Get sheet data
curl -H "X-API-Key: YOUR_API_KEY" \\
  "http://localhost:8910/api/sheets/MySheet?per_page=5&offset=1"

# Get schema
curl -H "X-API-Key: YOUR_API_KEY" \\
  "http://localhost:8910/api/sheets/MySheet/schema"

# Add new row
curl -X POST \\
  -H "X-API-Key: YOUR_API_KEY" \\
  -H "Content-Type: application/json" \\
  -d '{"id": 123, "name": "John Doe", "email": "john@example.com"}' \\
  "http://localhost:8910/api/sheets/MySheet"`
  },
  javascript: {
    title: 'JavaScript',
    code: `// Initialize API client
const apiKey = 'YOUR_API_KEY';
const baseUrl = 'http://localhost:8910/api/sheets';

const headers = {
  'X-API-Key': apiKey,
  'Content-Type': 'application/json'
};

// Get sheet data
async function getSheetData(sheetName) {
  const response = await fetch(\`\${baseUrl}/\${sheetName}?per_page=10&offset=1\`, {
    headers
  });
  return response.json();
}

// Get schema
async function getSchema(sheetName) {
  const response = await fetch(\`\${baseUrl}/\${sheetName}/schema\`, {
    headers
  });
  return response.json();
}

// Add new row
async function addRow(sheetName, data) {
  const response = await fetch(\`\${baseUrl}/\${sheetName}\`, {
    method: 'POST',
    headers,
    body: JSON.stringify(data)
  });
  return response.json();
}`
  },
  python: {
    title: 'Python',
    code: `import requests
import json

class SheetAPI:
    def __init__(self, api_key, base_url="http://localhost:8910/api/sheets"):
        self.api_key = api_key
        self.base_url = base_url
        self.headers = {
            'X-API-Key': api_key,
            'Content-Type': 'application/json'
        }

    def get_sheet_data(self, sheet_name, per_page=10, offset=1):
        """Get paginated sheet data"""
        url = f"{self.base_url}/{sheet_name}"
        params = {'per_page': per_page, 'offset': offset}
        response = requests.get(url, headers=self.headers, params=params)
        return response.json()

    def get_schema(self, sheet_name):
        """Get sheet schema"""
        url = f"{self.base_url}/{sheet_name}/schema"
        response = requests.get(url, headers=self.headers)
        return response.json()

    def add_row(self, sheet_name, data):
        """Add new row to sheet"""
        url = f"{self.base_url}/{sheet_name}"
        response = requests.post(url, headers=self.headers, json=data)
        return response.json()

# Usage example
api = SheetAPI('YOUR_API_KEY')
data = api.get_sheet_data('MySheet')
print(json.dumps(data, indent=2))`
  }
}

export default function CodeExamples() {
  const [activeTab, setActiveTab] = useState('curl')

  const copyCode = () => {
    navigator.clipboard.writeText(examples[activeTab as keyof typeof examples].code)
  }

  return (
    <div className="max-w-6xl mx-auto px-4 sm:px-6 lg:px-8 py-16">
      <div className="bg-white rounded-2xl shadow-xl p-8">
        <h2 className="text-3xl font-bold text-gray-900 mb-6 text-center">
          Code Examples
        </h2>
        <p className="text-gray-600 text-center mb-8">
          Integration examples for different programming languages.
        </p>

        <div className="border-b border-gray-200 mb-6">
          <nav className="-mb-px flex space-x-8">
            {Object.entries(examples).map(([key, example]) => (
              <button
                key={key}
                onClick={() => setActiveTab(key)}
                className={`py-2 px-1 border-b-2 font-medium text-sm ${
                  activeTab === key
                    ? 'border-blue-500 text-blue-600'
                    : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'
                }`}
              >
                {example.title}
              </button>
            ))}
          </nav>
        </div>

        <div className="relative">
          <div className="flex justify-between items-center mb-4">
            <h3 className="text-lg font-semibold text-gray-900">
              {examples[activeTab as keyof typeof examples].title} Example
            </h3>
            <button
              onClick={copyCode}
              className="px-4 py-2 bg-gray-100 text-gray-700 rounded-lg hover:bg-gray-200 transition-colors text-sm"
            >
              Copy Code
            </button>
          </div>

          <div className="bg-gray-900 rounded-lg p-6 overflow-x-auto">
            <pre className="text-gray-300 text-sm">
              <code>{examples[activeTab as keyof typeof examples].code}</code>
            </pre>
          </div>
        </div>

        <div className="mt-8 p-4 bg-blue-50 rounded-lg">
          <h4 className="text-sm font-semibold text-blue-900 mb-2">💡 Quick Tips</h4>
          <ul className="text-sm text-blue-800 space-y-1">
            <li>• Replace <code className="bg-blue-100 px-1 rounded">YOUR_API_KEY</code> with your generated API key</li>
            <li>• Make sure the server is running on port 8910</li>
            <li>• Check the <a href="/swagger" className="underline">API documentation</a> for complete endpoint details</li>
          </ul>
        </div>
      </div>
    </div>
  )
}