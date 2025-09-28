import { useState, useEffect } from 'react'
import JsonViewer from './JsonViewer'

interface ApiResponse {
  status: number
  data: unknown
}

export default function ApiDemo() {
  const [apiKey, setApiKey] = useState('')
  const [sheetName, setSheetName] = useState('')
  const [sheetNames, setSheetNames] = useState<string[]>([])
  const [endpoint, setEndpoint] = useState('GET /api/sheets/{sheetName}')
  const [response, setResponse] = useState<ApiResponse | null>(null)
  const [loading, setLoading] = useState(false)
  const [initialLoad, setInitialLoad] = useState(true)

  const secretKey = import.meta.env.VITE_API_SECRET_KEY || 'default-secret-key'

  useEffect(() => {
    if (apiKey === '') {
      const timestamp = Date.now().toString()
      const keyString = `${secretKey}:${timestamp}`
      const encodedKey = btoa(keyString)
      setApiKey(encodedKey)
    }
  }, [apiKey, secretKey])

  useEffect(() => {
    const fetchSheetNames = async () => {
      if (!apiKey) return

      try {
        const res = await fetch('/api/sheets', {
          headers: {
            'X-API-Key': apiKey,
          },
        })
        const data = await res.json()
        if (Array.isArray(data)) {
          setSheetNames(data)
          if (data.length > 0) {
            setSheetName(data[0])
          }
        }
      } catch (error) {
        console.error('Failed to fetch sheet names:', error)
      }
    }
    fetchSheetNames()
  }, [apiKey])

  useEffect(() => {
    if (apiKey && sheetName && initialLoad) {
      testEndpoint()
      setInitialLoad(false)
    }
  }, [apiKey, sheetName, initialLoad])

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
    <div className="w-full py-16 px-4 sm:px-6 lg:px-8">
      <div className="bg-white rounded-2xl shadow-xl p-8 w-full">
        <h2 className="text-3xl font-bold text-gray-900 mb-6 text-center">
          Live API Demo
        </h2>
        <p className="text-gray-600 text-center mb-8">
          Test the API endpoints directly from your browser. Watch how the API automatically detects hierarchical patterns in the Google Sheet and returns nested JSON.
        </p>

        <div className="grid grid-cols-1 lg:grid-cols-2 gap-8">
          {/* Left: Google Sheets Iframe */}
          <div 
            className="border rounded-lg overflow-hidden h-[600px] relative isolate-iframe"
            onClick={(e) => {
              e.stopPropagation();
              e.preventDefault();
            }}
            onFocus={(e) => {
              e.stopPropagation();
              e.preventDefault();
            }}
            onScroll={(e) => {
              e.stopPropagation();
              e.preventDefault();
            }}
            onWheel={(e) => {
              e.stopPropagation();
              e.preventDefault();
            }}
            onKeyDown={(e) => {
              e.stopPropagation();
              e.preventDefault();
            }}
            onMouseDown={(e) => e.stopPropagation()}
            tabIndex={-1}
          >
            <iframe
              src="https://docs.google.com/spreadsheets/d/1xemyHC3IG4TT0ubXXvi0FJdlDpHFueS3XSMB9yTBMqE/edit?usp=sharing"
              width="100%"
              height="100%"
              frameBorder="0"
              scrolling="no"
              className="w-full h-full"
              title="Live Google Sheet Preview"
              sandbox="allow-scripts allow-same-origin allow-forms"
              allow="fullscreen"
            ></iframe>
          </div>

          {/* Right: API Demo Container */}
          <div className="space-y-6">
            <div className="grid grid-cols-1 lg:grid-cols-2 gap-8">
              <div className="space-y-6">
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-2">
                    API Key (Auto-generated)
                  </label>
                  <input
                    type="text"
                    value={apiKey}
                    readOnly
                    className="w-full px-4 py-3 border border-gray-300 rounded-lg bg-gray-100 cursor-not-allowed font-mono text-sm"
                    placeholder="Auto-generating API key..."
                  />
                </div>

                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-2">
                    Sheet Name
                  </label>
                  <select
                    value={sheetName}
                    onChange={(e) => setSheetName(e.target.value)}
                    className="w-full px-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                  >
                    {sheetNames.map((name) => (
                      <option key={name} value={name}>
                        {name}
                      </option>
                    ))}
                  </select>
                </div>

                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-2">
                    Endpoint
                  </label>
                  <select
                    value={endpoint}
                    onChange={(e) => setEndpoint(e.target.value)}
                    className="w-full px-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-gray-500 focus:border-transparent"
                  >
                    <option value="GET /api/sheets/{sheetName}">GET /api/sheets/{sheetName} - Auto-grouped data</option>
                    <option value="GET /api/sheets/{sheetName}/schema">GET /api/sheets/{sheetName}/schema - Get schema</option>
                  </select>
                </div>

                <button
                  onClick={testEndpoint}
                  disabled={loading}
                  className="w-full bg-gray-900 text-white py-3 px-6 rounded-lg font-medium hover:bg-gray-800 disabled:bg-gray-400 transition-colors"
                >
                  {loading ? 'Testing...' : 'Test Endpoint'}
                </button>
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  Response
                </label>
                <div className="h-[600px] border border-gray-300 rounded-lg overflow-hidden">
                  {response ? (
                    <div className="h-full flex flex-col">
                      {/* Status Bar */}
                      <div className="bg-gray-100 px-4 py-2 border-b border-gray-200 flex items-center justify-between">
                        <div className={`flex items-center gap-2 ${response.status >= 200 && response.status < 300 ? 'text-green-600' : 'text-red-600'}`}>
                          <div className={`w-2 h-2 rounded-full ${response.status >= 200 && response.status < 300 ? 'bg-green-500' : 'bg-red-500'}`}></div>
                          <span className="text-sm font-medium">Status: {response.status}</span>
                          {response.status >= 200 && response.status < 300 && (
                            <span className="text-xs bg-green-100 text-green-800 px-2 py-1 rounded">Success</span>
                          )}
                        </div>
                        <div className="text-xs text-gray-500">
                          {Array.isArray(response.data) ? `${response.data.length} items` : typeof response.data === 'object' ? 'Object' : 'Value'}
                        </div>
                      </div>

                      {/* JSON Viewer */}
                      <div className="flex-1 overflow-hidden">
                        <JsonViewer data={response.data} />
                      </div>
                    </div>
                  ) : (
                    <div className="h-full bg-gray-50 flex items-center justify-center">
                      <div className="text-center text-gray-500">
                        <div className="w-16 h-16 bg-gray-200 rounded-lg flex items-center justify-center mx-auto mb-4">
                          <svg className="w-8 h-8 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13 10V3L4 14h7v7l9-11h-7z" />
                          </svg>
                        </div>
                        <p className="font-medium mb-2">Ready to test</p>
                        <p className="text-sm">
                          Click "Test Endpoint" to see the live API response here.
                          <br />
                          Watch how the API automatically detects if your data has hierarchical structure.
                        </p>
                      </div>
                    </div>
                  )}
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  )
}