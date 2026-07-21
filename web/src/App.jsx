import React from 'react'

function App() {
  return (
    <div className="container animate-fade-in" style={{ paddingTop: '80px', paddingBottom: '80px' }}>
      <header style={{ textAlign: 'center', marginBottom: '60px' }}>
        <h1 style={{ fontSize: '3.5rem', marginBottom: '16px' }}>
          Welcome to <span className="text-gradient">Dream Saver</span>
        </h1>
        <p style={{ color: 'var(--text-secondary)', fontSize: '1.25rem', maxWidth: '600px', margin: '0 auto' }}>
          Your intelligent AI-powered personal finance manager. Track, analyze, and optimize your spending with ease.
        </p>
      </header>

      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(300px, 1fr))', gap: '24px', marginBottom: '40px' }}>
        
        <div className="glass-panel" style={{ padding: '32px' }}>
          <div style={{ width: '48px', height: '48px', borderRadius: '12px', background: 'rgba(99, 102, 241, 0.1)', display: 'flex', alignItems: 'center', justifyContent: 'center', marginBottom: '20px' }}>
            <span style={{ fontSize: '24px' }}>🤖</span>
          </div>
          <h3 style={{ fontSize: '1.25rem', marginBottom: '12px' }}>AI Financial Assistant</h3>
          <p style={{ color: 'var(--text-secondary)' }}>
            Ask questions about your budget, predict future expenses, and get personalized financial advice powered by Gemini.
          </p>
        </div>

        <div className="glass-panel" style={{ padding: '32px' }}>
          <div style={{ width: '48px', height: '48px', borderRadius: '12px', background: 'rgba(236, 72, 153, 0.1)', display: 'flex', alignItems: 'center', justifyContent: 'center', marginBottom: '20px' }}>
            <span style={{ fontSize: '24px' }}>📸</span>
          </div>
          <h3 style={{ fontSize: '1.25rem', marginBottom: '12px' }}>Smart Receipt Scanner</h3>
          <p style={{ color: 'var(--text-secondary)' }}>
            Simply snap a picture of your receipt. Our AI automatically extracts the amount and categorizes the expense for you.
          </p>
        </div>

        <div className="glass-panel" style={{ padding: '32px' }}>
          <div style={{ width: '48px', height: '48px', borderRadius: '12px', background: 'rgba(16, 185, 129, 0.1)', display: 'flex', alignItems: 'center', justifyContent: 'center', marginBottom: '20px' }}>
            <span style={{ fontSize: '24px' }}>📊</span>
          </div>
          <h3 style={{ fontSize: '1.25rem', marginBottom: '12px' }}>Advanced Analytics</h3>
          <p style={{ color: 'var(--text-secondary)' }}>
            Visualize your spending patterns with interactive charts and track your progress towards financial goals.
          </p>
        </div>

      </div>

      <div className="flex-center" style={{ gap: '16px' }}>
        <button className="btn-primary">
          Get Started
        </button>
        <button className="btn-secondary">
          View Dashboard
        </button>
      </div>
    </div>
  )
}

export default App
