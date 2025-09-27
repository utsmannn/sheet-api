import { useState, useEffect } from 'react'

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
          API Demo
        </h2>
        <p className="text-gray-600 text-center mb-8">
          Test the API endpoints directly from your browser.
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
                    className="w-full px-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                  >
                    <option value="GET /api/sheets/{sheetName}">GET /api/sheets/{sheetName} - Get sheet data</option>
                    <option value="GET /api/sheets/{sheetName}/schema">GET /api/sheets/{sheetName}/schema - Get schema</option>
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
                <div className="bg-gray-900 text-green-400 p-4 rounded-lg h-[600px] overflow-auto font-mono text-sm">
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
      </div>
    </div>
  )
}