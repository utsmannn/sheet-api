import { useState } from 'react'

export default function ApiKeyGenerator() {
  const [secretKey, setSecretKey] = useState('my-secret-key')
  const [apiKey, setApiKey] = useState('')
  const [timestamp, setTimestamp] = useState('')

  const generateApiKey = () => {
    const currentTimestamp = Date.now().toString()
    const keyString = `${secretKey}:${currentTimestamp}`
    const encodedKey = btoa(keyString)

    setTimestamp(currentTimestamp)
    setApiKey(encodedKey)
  }

  const copyToClipboard = (text: string) => {
    if (navigator.clipboard) {
      navigator.clipboard.writeText(text).catch(err => {
        console.error('Failed to copy with navigator: ', err)
      })
    } else {
      // Fallback for non-secure contexts
      const textArea = document.createElement('textarea')
      textArea.value = text
      textArea.style.position = 'fixed' // Prevent scrolling to bottom of page in MS Edge.
      textArea.style.top = '0'
      textArea.style.left = '0'
      document.body.appendChild(textArea)
      textArea.focus()
      textArea.select()
      try {
        document.execCommand('copy')
      } catch (err) {
        console.error('Fallback: Oops, unable to copy', err)
      }
      document.body.removeChild(textArea)
    }
  }

  return (
    <div className="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 py-16">
      <div className="bg-white rounded-2xl shadow-xl p-8">
        <h2 className="text-3xl font-bold text-gray-900 mb-6 text-center">
          API Key Generator
        </h2>
        <p className="text-gray-600 text-center mb-8">
          Generate a secure API key for accessing the Sheet API endpoints.
        </p>

        <div className="space-y-6">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">
              Secret Key (from API_SECRET_KEY environment variable)
            </label>
            <input
              type="text"
              value={secretKey}
              onChange={(e) => setSecretKey(e.target.value)}
              className="w-full px-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
              placeholder="Enter your secret key"
            />
          </div>

          <button
            onClick={generateApiKey}
            className="w-full bg-blue-600 text-white py-3 px-6 rounded-lg font-semibold hover:bg-blue-700 transition-colors"
          >
            Generate API Key
          </button>

          {apiKey && (
            <div className="space-y-4 p-4 bg-gray-50 rounded-lg">
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  Generated Timestamp
                </label>
                <div className="flex items-center space-x-2">
                  <input
                    type="text"
                    value={timestamp}
                    readOnly
                    className="flex-1 px-3 py-2 bg-white border border-gray-300 rounded text-sm"
                  />
                  <button
                    onClick={() => copyToClipboard(timestamp)}
                    className="px-3 py-2 bg-gray-200 text-gray-700 rounded text-sm hover:bg-gray-300 transition-colors"
                  >
                    Copy
                  </button>
                </div>
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  API Key (Base64 Encoded)
                </label>
                <div className="flex items-center space-x-2">
                  <input
                    type="text"
                    value={apiKey}
                    readOnly
                    className="flex-1 px-3 py-2 bg-white border border-gray-300 rounded text-sm font-mono"
                  />
                  <button
                    onClick={() => copyToClipboard(apiKey)}
                    className="px-3 py-2 bg-blue-600 text-white rounded text-sm hover:bg-blue-700 transition-colors"
                  >
                    Copy
                  </button>
                </div>
              </div>

              <div className="text-sm text-gray-600 space-y-2">
                <p><strong>Usage:</strong></p>
                <div className="bg-gray-800 text-gray-300 p-3 rounded font-mono text-xs overflow-x-auto">
                  curl -H "X-API-Key: {apiKey}" http://localhost:8910/api/sheets/names
                </div>
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  )
}