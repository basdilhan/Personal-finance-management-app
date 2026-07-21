import React, { useState, useRef, useEffect } from 'react';
import { Send, Bot, User, Loader2 } from 'lucide-react';
import { useAuth } from '../context/AuthContext';
import apiClient from '../api/apiClient';

export default function Chatbot() {
  const { currentUser } = useAuth();
  const [messages, setMessages] = useState([
    { id: 1, text: 'Hello! I am DreamSaver AI. I can analyze your income, expenses, and budgets to give you personalized financial advice. How can I help you today?', sender: 'bot' }
  ]);
  const [input, setInput] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const messagesEndRef = useRef(null);

  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: "smooth" });
  };

  useEffect(() => {
    scrollToBottom();
  }, [messages]);

  async function handleSend(e) {
    e.preventDefault();
    if (!input.trim()) return;

    const userMessage = { id: Date.now(), text: input, sender: 'user' };
    setMessages(prev => [...prev, userMessage]);
    setInput('');
    setIsLoading(true);

    try {
      // Use our secure apiClient that automatically attaches Firebase Tokens and X-User-Id
      const res = await apiClient.post('/chat', {
        message: userMessage.text
      });

      const botMessage = {
        id: Date.now() + 1,
        text: res.data.reply || "I'm sorry, I couldn't process that request.",
        sender: 'bot'
      };
      setMessages(prev => [...prev, botMessage]);
    } catch (error) {
      const errorMessage = {
        id: Date.now() + 1,
        text: "Sorry, I am having trouble connecting to the backend. Please try again.",
        sender: 'bot'
      };
      setMessages(prev => [...prev, errorMessage]);
    } finally {
      setIsLoading(false);
    }
  }

  return (
    <div style={{ height: 'calc(100vh - 80px)', display: 'flex', flexDirection: 'column' }}>
      
      <div style={{ marginBottom: '24px' }}>
        <h1 style={{ fontSize: '32px', marginBottom: '8px', display: 'flex', alignItems: 'center', gap: '12px' }}>
          <Bot color="var(--accent-blue)" /> DreamSaver AI
        </h1>
        <p className="text-muted">Chat with your personalized financial assistant, trained on your real budget data.</p>
      </div>

      <div className="dashboard-panel" style={{ flex: 1, display: 'flex', flexDirection: 'column', overflow: 'hidden', padding: 0 }}>
        
        {/* Messages Area */}
        <div style={{ flex: 1, overflowY: 'auto', padding: '24px', display: 'flex', flexDirection: 'column', gap: '16px' }}>
          {messages.map((msg) => (
            <div key={msg.id} style={{ display: 'flex', gap: '16px', flexDirection: msg.sender === 'user' ? 'row-reverse' : 'row' }}>
              <div style={{ 
                width: '36px', height: '36px', borderRadius: '50%', 
                background: msg.sender === 'user' ? 'var(--accent-blue)' : 'var(--bg-tertiary)',
                color: msg.sender === 'user' ? 'white' : 'var(--text-primary)',
                display: 'flex', alignItems: 'center', justifyContent: 'center', flexShrink: 0
              }}>
                {msg.sender === 'user' ? <User size={18} /> : <Bot size={18} />}
              </div>
              
              <div style={{
                background: msg.sender === 'user' ? 'var(--accent-blue)' : 'var(--bg-tertiary)',
                color: msg.sender === 'user' ? 'white' : 'var(--text-primary)',
                padding: '12px 16px',
                borderRadius: '16px',
                borderTopRightRadius: msg.sender === 'user' ? '4px' : '16px',
                borderTopLeftRadius: msg.sender === 'bot' ? '4px' : '16px',
                maxWidth: '70%',
                lineHeight: 1.5,
                boxShadow: '0 2px 4px rgba(0,0,0,0.05)'
              }}>
                {msg.text}
              </div>
            </div>
          ))}
          {isLoading && (
            <div style={{ display: 'flex', gap: '16px' }}>
              <div style={{ width: '36px', height: '36px', borderRadius: '50%', background: 'var(--bg-tertiary)', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                <Bot size={18} />
              </div>
              <div style={{ background: 'var(--bg-tertiary)', padding: '12px 16px', borderRadius: '16px', borderTopLeftRadius: '4px' }}>
                <Loader2 className="lucide-spin" size={18} color="var(--text-secondary)" />
              </div>
            </div>
          )}
          <div ref={messagesEndRef} />
        </div>

        {/* Input Area */}
        <div style={{ padding: '24px', borderTop: '1px solid var(--border-light)', background: 'var(--bg-secondary)' }}>
          <form onSubmit={handleSend} style={{ display: 'flex', gap: '12px' }}>
            <input
              type="text"
              value={input}
              onChange={(e) => setInput(e.target.value)}
              placeholder="Ask about your expenses, budgets, or get financial advice..."
              className="input-field"
              style={{ flex: 1, padding: '16px', borderRadius: '100px' }}
              disabled={isLoading}
            />
            <button 
              type="submit" 
              className="btn-primary" 
              disabled={isLoading || !input.trim()}
              style={{ borderRadius: '100px', width: '52px', height: '52px', padding: 0, display: 'flex', alignItems: 'center', justifyContent: 'center' }}
            >
              <Send size={20} />
            </button>
          </form>
        </div>
        
      </div>
    </div>
  );
}
