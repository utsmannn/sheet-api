export default function Footer() {
  return (
    <footer className="bg-gray-900 text-white py-12">
      <div className="max-w-6xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="grid grid-cols-1 md:grid-cols-3 gap-8">
          <div>
            <h3 className="text-lg font-semibold mb-4">Sheet API</h3>
            <p className="text-gray-400 text-sm">
              A powerful Kotlin REST API for managing Google Sheets data with automatic
              hierarchical grouping, schema detection, and secure authentication.
            </p>
          </div>

          <div>
            <h3 className="text-lg font-semibold mb-4">Documentation</h3>
            <ul className="space-y-2 text-sm">
              <li>
                <a href="/swagger" className="text-gray-400 hover:text-white transition-colors">
                  API Documentation
                </a>
              </li>
              <li>
                <a href="/openapi.json" className="text-gray-400 hover:text-white transition-colors">
                  OpenAPI Spec
                </a>
              </li>
              <li>
                <a href="https://github.com/utsmannn/sheet-api" className="text-gray-400 hover:text-white transition-colors">
                  GitHub Repository
                </a>
              </li>
              <li>
                <a href="https://deepwiki.com/utsmannn/sheet-api" className="text-gray-400 hover:text-white transition-colors">
                  DeepWiki Documentation
                </a>
              </li>
            </ul>
          </div>

          <div>
            <h3 className="text-lg font-semibold mb-4">Features</h3>
            <ul className="space-y-2 text-sm text-gray-400">
              <li>• Auto Hierarchical Grouping</li>
              <li>• Dynamic Sheet ID Support</li>
              <li>• Smart Data Detection</li>
              <li>• Secure API Authentication</li>
              <li>• Performance Optimized</li>
              <li>• JSON Schema Validation</li>
            </ul>
          </div>
        </div>

        <div className="border-t border-gray-800 mt-8 pt-8 text-center">
          <p className="text-gray-400 text-sm">
            Built with ❤️ using Ktor, React, and TypeScript
          </p>
        </div>
      </div>
    </footer>
  )
}