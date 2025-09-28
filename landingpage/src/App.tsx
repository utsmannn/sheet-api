import Header from './components/Header'
import Hero from './components/Hero'
import ApiKeyGenerator from './components/ApiKeyGenerator'
import ApiDemo from './components/ApiDemo'
import CodeExamples from './components/CodeExamples'
import Footer from './components/Footer'

function App() {
  return (
    <div className="min-h-screen bg-gray-50 font-ubuntu">
      <Header />
      <Hero />
      <div id="demo">
        <ApiDemo />
      </div>
      <ApiKeyGenerator />
      <div id="examples">
        <CodeExamples />
      </div>
      <Footer />
    </div>
  )
}

export default App