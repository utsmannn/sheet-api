import Hero from './components/Hero'
import ApiKeyGenerator from './components/ApiKeyGenerator'
import ApiDemo from './components/ApiDemo'
import CodeExamples from './components/CodeExamples'
import Footer from './components/Footer'

function App() {
  return (
    <div className="min-h-screen bg-gradient-to-br from-blue-50 to-indigo-100">
      <Hero />
      <ApiKeyGenerator />
      <ApiDemo />
      <CodeExamples />
      <Footer />
    </div>
  )
}

export default App