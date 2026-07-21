import React, { useState } from 'react';
import { Send, Bot, User } from 'lucide-react';

export default function Chatbot() {
  const [messages, setMessages] = useState([
    { id: 1, sender: 'bot', text: 'Hello! I am your AI Financial Assistant. Ask me anything about budgeting, saving, or investing.' }
  ]);
  const [input, setInput] = useState('');
  const [loading, setLoading] = useState(false);

  const handleSend = async (e) => {
    e.preventDefault();
    if (!input.trim()) return;

    const userMsg = { id: Date.now(), sender: 'user', text: input };
    setMessages(prev => [...prev, userMsg]);
    setInput('');
    setLoading(true);

    try {
      // Connect to the Render Java Backend Gemini API
      const response = await fetch('https://personal-finance-management-app-backend.onrender.com/api/ai/chat', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ prompt: userMsg.text })
      });
      const data = await response.json();
      
      const botMsg = { id: Date.now(), sender: 'bot', text: data.response || "I couldn't process that." };
      setMessages(prev => [...prev, botMsg]);
    } catch (error) {
      setMessages(prev => [...prev, { id: Date.now(), sender: 'bot', text: "Network error. Please try again later." }]);
    }
    
    setLoading(false);
  };

  return (
    <div style={{ display: 'flex', flexDirection: 'column', height: 'calc(100vh - 80px)' }}>
      <div style={{ marginBottom: '24px' }}>
        <h1 style={{ fontSize: '28px', marginBottom: '8px' }}>AI Financial Assistant</h1>
        <p className="text-muted">Powered by Google Gemini 3.5 Flash</p>
      </div>

      <div className="dashboard-panel" style={{ flex: 1, display: 'flex', flexDirection: 'column', padding: 0, overflow: 'hidden' }}>
        
        {/* Chat History */}
        <div style={{ flex: 1, overflowY: 'auto', padding: '24px', display: 'flex', flexDirection: 'column', gap: '20px' }}>
          {messages.map(msg => (
            <div key={msg.id} style={{ display: 'flex', gap: '16px', alignItems: 'flex-start', flexDirection: msg.sender === 'user' ? 'row-reverse' : 'row' }}>
              
              <div style={{ 
                width: '36px', height: '36px', borderRadius: '50%', flexShrink: 0,
                background: msg.sender === 'bot' ? 'var(--surface-2)' : 'var(--accent-blue)',
                display: 'flex', alignItems: 'center', justifyContent: 'center'
              }}>
                {msg.sender === 'bot' ? <Bot size={18} /> : <User size={18} color="white" />}
              </div>

              <div style={{ 
                background: msg.sender === 'bot' ? 'var(--surface-1)' : 'var(--accent-blue-hover)',
                color: msg.sender === 'bot' ? 'var(--text-primary)' : 'white',
                padding: '12px 16px', 
                borderRadius: '12px',
                borderTopLeftRadius: msg.sender === 'bot' ? 0 : '12px',
                borderTopRightRadius: msg.sender === 'user' ? 0 : '12px',
                maxWidth: '75%',
                lineHeight: 1.5,
                fontSize: '14px'
              }}>
                {msg.text}
              </div>
            </div>
          ))}
          {loading && (
            <div style={{ display: 'flex', gap: '16px', alignItems: 'flex-start' }}>
               <div style={{ width: '36px', height: '36px', borderRadius: '50%', background: 'var(--surface-2)', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                <Bot size={18} />
              </div>
              <div style={{ background: 'var(--surface-1)', padding: '12px 16px', borderRadius: '12px', borderTopLeftRadius: 0, fontSize: '14px', color: 'var(--text-secondary)' }}>
                Thinking...
              </div>
            </div>
          )}
        </div>

        {/* Input Area */}
        <form onSubmit={handleSend} style={{ borderTop: '1px solid var(--border-light)', padding: '20px', display: 'flex', gap: '12px', background: 'var(--bg-secondary)' }}>
          <input 
            type="text" 
            className="input-field" 
            placeholder="Ask about saving for a house..." 
            value={input}
            onChange={e => setInput(e.target.value)}
            disabled={loading}
            style={{ flex: 1, border: '1px solid var(--border-focus)' }}
          />
          <button type="submit" disabled={loading || !input.trim()} className="btn-primary" style={{ padding: '0 20px' }}>
            <Send size={18} />
          </button>
        </form>

      </div>
    </div>
  );
}
