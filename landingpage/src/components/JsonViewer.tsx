import { useState } from 'react'
import { JsonView, darkStyles } from 'react-json-view-lite'
import 'react-json-view-lite/dist/index.css'

interface JsonViewerProps {
  data: unknown
}

export default function JsonViewer({ data }: JsonViewerProps) {
  const [viewMode, setViewMode] = useState<'pretty' | 'raw'>('pretty')

  const copyToClipboard = () => {
    navigator.clipboard.writeText(JSON.stringify(data, null, 2))
  }

  return (
    <div className="bg-gray-900 rounded-lg h-full">
      {/* Header */}
      <div className="flex items-center justify-between px-4 py-2 bg-gray-800 border-b border-gray-700">
        <div className="flex items-center space-x-2">
          <div className="w-3 h-3 bg-red-500 rounded-full"></div>
          <div className="w-3 h-3 bg-yellow-500 rounded-full"></div>
          <div className="w-3 h-3 bg-green-500 rounded-full"></div>
          <span className="text-gray-400 text-sm ml-2">Response</span>
        </div>

        <div className="flex items-center space-x-2">
          <div className="flex bg-gray-700 rounded">
            <button
              onClick={() => setViewMode('pretty')}
              className={`px-3 py-1 text-xs rounded-l ${
                viewMode === 'pretty'
                  ? 'bg-gray-600 text-white'
                  : 'text-gray-400 hover:text-gray-200'
              }`}
            >
              Pretty
            </button>
            <button
              onClick={() => setViewMode('raw')}
              className={`px-3 py-1 text-xs rounded-r ${
                viewMode === 'raw'
                  ? 'bg-gray-600 text-white'
                  : 'text-gray-400 hover:text-gray-200'
              }`}
            >
              Raw
            </button>
          </div>

          <button
            onClick={copyToClipboard}
            className="text-gray-400 hover:text-gray-200 p-1"
            title="Copy JSON"
          >
            <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M8 16H6a2 2 0 01-2-2V6a2 2 0 012-2h8a2 2 0 012 2v2m-6 12h8a2 2 0 002-2v-8a2 2 0 00-2-2h-8a2 2 0 00-2 2v8a2 2 0 002 2z" />
            </svg>
          </button>
        </div>
      </div>

      {/* Content */}
      <div className="p-4 h-full overflow-auto whitespace-nowrap">
        {viewMode === 'pretty' ? (
          <div className="text-sm font-mono">
            <JsonView
              data={data as object}
              shouldExpandNode={(level) => level < 3}
              style={{
                ...darkStyles,
                container: `!bg-transparent`
              }}
            />
          </div>
        ) : (
          <pre className="text-gray-300 text-sm whitespace-pre font-mono">
            {JSON.stringify(data, null, 2)}
          </pre>
        )}
      </div>
    </div>
  )
}