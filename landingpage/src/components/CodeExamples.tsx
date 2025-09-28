import { useState } from 'react'

const examples = {
  curl: {
    title: 'cURL',
    code: `# Get auto-grouped hierarchical data
curl -H "X-API-Key: YOUR_API_KEY" \\
  "http://localhost:8910/api/sheets/MySheet?per_page=5&offset=1"

# Access custom sheet ID
curl -H "X-API-Key: YOUR_API_KEY" \\
  "http://localhost:8910/api/sheetId/1BxiMVs0XRA5nFMdKvBdBZjgmUUqptlbs74OgvE2upms/MySheet"

# Get schema
curl -H "X-API-Key: YOUR_API_KEY" \\
  "http://localhost:8910/api/sheets/MySheet/schema"

# Add new row with validation
curl -X POST \\
  -H "X-API-Key: YOUR_API_KEY" \\
  -H "Content-Type: application/json" \\
  -d '{"district": "New District", "population": "75000", "area": "15.2"}' \\
  "http://localhost:8910/api/sheets/MySheet"`
  },
  javascript: {
    title: 'JavaScript',
    code: `// Enhanced Sheet API Client with Auto-Grouping
class SheetAPIClient {
  constructor(apiKey, baseUrl = 'http://localhost:8910/api') {
    this.apiKey = apiKey;
    this.baseUrl = baseUrl;
    this.headers = {
      'X-API-Key': apiKey,
      'Content-Type': 'application/json'
    };
  }

  // Get auto-grouped data (flat or hierarchical)
  async getSheetData(sheetName, options = {}) {
    const { perPage = 10, offset = 1 } = options;
    const response = await fetch(
      \`\${this.baseUrl}/sheets/\${sheetName}?per_page=\${perPage}&offset=\${offset}\`,
      { headers: this.headers }
    );
    return response.json();
  }

  // Access any sheet with custom ID
  async getCustomSheetData(sheetId, sheetName, options = {}) {
    const { perPage = 10, offset = 1 } = options;
    const response = await fetch(
      \`\${this.baseUrl}/sheetId/\${sheetId}/\${sheetName}?per_page=\${perPage}&offset=\${offset}\`,
      { headers: this.headers }
    );
    return response.json();
  }

  // Get schema with auto-detection
  async getSchema(sheetName) {
    const response = await fetch(
      \`\${this.baseUrl}/sheets/\${sheetName}/schema\`,
      { headers: this.headers }
    );
    return response.json();
  }

  // Add row with validation
  async addRow(sheetName, data) {
    const response = await fetch(
      \`\${this.baseUrl}/sheets/\${sheetName}\`,
      {
        method: 'POST',
        headers: this.headers,
        body: JSON.stringify(data)
      }
    );
    return response.json();
  }
}

// Usage example
const api = new SheetAPIClient('YOUR_API_KEY');
const data = await api.getSheetData('MySheet');
console.log('Auto-detected format:', data);`
  },
  python: {
    title: 'Python',
    code: `import requests
import json
from typing import Dict, Any, Optional

class SheetAPIClient:
    """Enhanced Sheet API Client with Auto-Grouping Support"""

    def __init__(self, api_key: str, base_url: str = "http://localhost:8910/api"):
        self.api_key = api_key
        self.base_url = base_url
        self.headers = {
            'X-API-Key': api_key,
            'Content-Type': 'application/json'
        }

    def get_sheet_data(self, sheet_name: str, per_page: int = 10, offset: int = 1) -> Dict[str, Any]:
        """Get auto-grouped data (detects flat vs hierarchical)"""
        url = f"{self.base_url}/sheets/{sheet_name}"
        params = {'per_page': per_page, 'offset': offset}
        response = requests.get(url, headers=self.headers, params=params)
        response.raise_for_status()
        return response.json()

    def get_custom_sheet_data(self, sheet_id: str, sheet_name: str,
                            per_page: int = 10, offset: int = 1) -> Dict[str, Any]:
        """Access any Google Sheet with custom ID"""
        url = f"{self.base_url}/sheetId/{sheet_id}/{sheet_name}"
        params = {'per_page': per_page, 'offset': offset}
        response = requests.get(url, headers=self.headers, params=params)
        response.raise_for_status()
        return response.json()

    def get_schema(self, sheet_name: str) -> Dict[str, Any]:
        """Get auto-detected schema"""
        url = f"{self.base_url}/sheets/{sheet_name}/schema"
        response = requests.get(url, headers=self.headers)
        response.raise_for_status()
        return response.json()

    def add_row(self, sheet_name: str, data: Dict[str, Any]) -> Dict[str, Any]:
        """Add new row with automatic validation"""
        url = f"{self.base_url}/sheets/{sheet_name}"
        response = requests.post(url, headers=self.headers, json=data)
        response.raise_for_status()
        return response.json()

    def detect_data_structure(self, sheet_name: str) -> str:
        """Detect if data is flat or hierarchical"""
        data = self.get_sheet_data(sheet_name, per_page=1)
        return "hierarchical" if isinstance(data, dict) else "flat"

# Usage examples
api = SheetAPIClient('YOUR_API_KEY')

# Auto-detection in action
data = api.get_sheet_data('MySheet')
structure = api.detect_data_structure('MySheet')
print(f"Detected structure: {structure}")
print(f"Data: {json.dumps(data, indent=2)}")

# Custom sheet access
custom_data = api.get_custom_sheet_data(
    '1BxiMVs0XRA5nFMdKvBdBZjgmUUqptlbs74OgvE2upms',
    'MySheet'
)
print(f"Custom sheet data: {json.dumps(custom_data, indent=2)}"`
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
          Integration examples showcasing auto-hierarchical grouping and dynamic sheet ID features.
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

        <div className="mt-8 grid grid-cols-1 md:grid-cols-2 gap-4">
          <div className="p-4 bg-gray-50 rounded-lg border border-gray-200">
            <h4 className="text-sm font-medium text-gray-900 mb-2">Quick Tips</h4>
            <ul className="text-sm text-gray-700 space-y-1">
              <li>• Replace <code className="bg-gray-200 px-1 rounded">YOUR_API_KEY</code> with your generated API key</li>
              <li>• Make sure the server is running on port 8910</li>
              <li>• Check the <a href="/swagger" className="underline">API documentation</a> for complete endpoint details</li>
            </ul>
          </div>
          <div className="p-4 bg-gray-50 rounded-lg border border-gray-200">
            <h4 className="text-sm font-medium text-gray-900 mb-2">Key Features</h4>
            <ul className="text-sm text-gray-700 space-y-1">
              <li>• <strong>Auto-Grouping:</strong> API detects hierarchical patterns automatically</li>
              <li>• <strong>Dynamic Sheet ID:</strong> Access any Google Sheet via path parameters</li>
              <li>• <strong>Performance:</strong> Single API call optimization for faster responses</li>
            </ul>
          </div>
        </div>
      </div>
    </div>
  )
}