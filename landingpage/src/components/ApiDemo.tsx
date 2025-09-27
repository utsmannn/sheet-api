import { useState } from 'react'

interface ApiResponse {
  status: number
  data: unknown
}

export default function ApiDemo() {
  const [apiKey, setApiKey] = useState('')
  const [sheetName, setSheetName] = useState('MySheet')
  const [endpoint, setEndpoint] = useState('GET /api/sheets/{sheetName}')
  const [response, setResponse] = useState<ApiResponse | null>(null)
  const [loading, setLoading] = useState(false)

  const testEndpoint = async () => {
    if (!apiKey.trim()) {
      alert('Please enter an API key')
      return
    }

    setLoading(true)
    try {
      let url = ''
      let method = 'GET'
      let body = null

      if (endpoint.includes('GET')) {
        if (endpoint.includes('schema')) {
          url = `/api/sheets/${sheetName}/schema`
        } else {
          url = `/api/sheets/${sheetName}?per_page=5&offset=1`
        }
      } else if (endpoint.includes('POST')) {
        url = `/api/sheets/${sheetName}`
        method = 'POST'
        body = JSON.stringify({
          id: 123,
          name: "John Doe",
          email: "john@example.com",
          active: "true"
        })
      }

      const headers: Record<string, string> = {
        'X-API-Key': apiKey,
      }

      if (method === 'POST') {
        headers['Content-Type'] = 'application/json'
      }

      const res = await fetch(url, {
        method,
        headers,
        body
      })

      const data = await res.json()
      setResponse({
        status: res.status,
        data
      })
    } catch (error) {
      setResponse({
        status: 0,
        data: { error: 'Network error or server not running' }
      })
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="max-w-6xl mx-auto px-4 sm:px-6 lg:px-8 py-16">
      <div className="bg-white rounded-2xl shadow-xl p-8">
        <h2 className="text-3xl font-bold text-gray-900 mb-6 text-center">
          API Demo
        </h2>
        <p className="text-gray-600 text-center mb-8">
          Test the API endpoints directly from your browser.
        </p>

        <div className="grid grid-cols-1 lg:grid-cols-2 gap-8">
          <div className="space-y-6">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">
                API Key
              </label>
              <input
                type="text"
                value={apiKey}
                onChange={(e) => setApiKey(e.target.value)}
                className="w-full px-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent font-mono text-sm"
                placeholder="Enter your API key"
              />
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">
                Sheet Name
              </label>
              <input
                type="text"
                value={sheetName}
                onChange={(e) => setSheetName(e.target.value)}
                className="w-full px-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                placeholder="Sheet name"
              />
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">
                Endpoint
              </label>
              <select
                value={endpoint}
                onChange={(e) => setEndpoint(e.target.value)}
                className="w-full px-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
              >
                <option value="GET /api/sheets/{sheetName}">GET /api/sheets/{sheetName} - Get sheet data</option>
                <option value="GET /api/sheets/{sheetName}/schema">GET /api/sheets/{sheetName}/schema - Get schema</option>
                <option value="POST /api/sheets/{sheetName}">POST /api/sheets/{sheetName} - Add new row</option>
              </select>
            </div>

            <button
              onClick={testEndpoint}
              disabled={loading}
              className="w-full bg-green-600 text-white py-3 px-6 rounded-lg font-semibold hover:bg-green-700 disabled:bg-gray-400 transition-colors"
            >
              {loading ? 'Testing...' : 'Test Endpoint'}
            </button>
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">
              Response
            </label>
            <div className="bg-gray-900 text-green-400 p-4 rounded-lg h-96 overflow-auto font-mono text-sm">
              {response ? (
                <div>
                  <div className={`mb-2 ${response.status >= 200 && response.status < 300 ? 'text-green-400' : 'text-red-400'}`}>
                    Status: {response.status}
                  </div>
                  <pre className="whitespace-pre-wrap text-gray-300">
                    {JSON.stringify(response.data, null, 2)}
                  </pre>
                </div>
              ) : (
                <div className="text-gray-500">
                  Click "Test Endpoint" to see the response here.
                </div>
              )}
            </div>
          </div>
        </div>
      </div>
    </div>
  )
}