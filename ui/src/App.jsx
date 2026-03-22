import React from 'react'
import { BrowserRouter, Routes, Route } from 'react-router-dom'
import Nav from './components/Nav'
import Home from './pages/Home'
import MyUrls from './pages/MyUrls'

export default function App() {
  return (
    <BrowserRouter>
      <div
        style={{
          display: 'flex',
          flexDirection: 'column',
          minHeight: '100vh',
        }}
      >
        <Nav />

        <Routes>
          <Route path="/" element={<Home />} />
          <Route path="/urls" element={<MyUrls />} />
        </Routes>

        {/* Footer */}
        <footer
          style={{
            marginTop: 'auto',
            borderTop: '1px solid var(--stone-100)',
            padding: '20px 24px',
            textAlign: 'center',
            fontSize: 12,
            color: 'var(--stone-400)',
            fontFamily: 'var(--font-sans)',
          }}
        >
          snip · self-hosted URL shortener · built with Spring Boot + React
        </footer>
      </div>
    </BrowserRouter>
  )
}
