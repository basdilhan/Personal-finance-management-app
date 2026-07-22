import React, { useState } from 'react';
import { useAuth } from '../context/AuthContext';
import { useNavigate } from 'react-router-dom';
import { ShieldCheck, TrendingUp } from 'lucide-react';

export default function Login() {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  
  const { login, loginWithGoogle } = useAuth();
  const navigate = useNavigate();

  async function handleSubmit(e) {
    e.preventDefault();
    setError('');
    setLoading(true);

    try {
      await login(email, password);
      navigate('/dashboard');
    } catch (err) {
      setError(err.message || 'Failed to authenticate');
    }
    setLoading(false);
  }

  async function handleGoogleLogin() {
    setError('');
    setLoading(true);
    try {
      await loginWithGoogle();
      navigate('/dashboard');
    } catch (err) {
      setError(err.message || 'Failed to authenticate with Google');
    }
    setLoading(false);
  }

  return (
    <div style={{ display: 'flex', minHeight: '100vh', width: '100vw', background: 'var(--bg-primary)' }}>
      
      {/* Left Branding Panel */}
      <div style={{ 
        flex: 1, 
        background: 'linear-gradient(135deg, #000000 0%, #0a192f 100%)',
        position: 'relative',
        display: 'flex',
        flexDirection: 'column',
        justifyContent: 'center',
        padding: '60px',
        borderRight: '1px solid var(--border-light)',
        overflow: 'hidden'
      }}>
        {/* Decorative elements */}
        <div style={{ position: 'absolute', top: '-10%', left: '-10%', width: '500px', height: '500px', background: 'var(--accent-blue)', opacity: 0.15, filter: 'blur(100px)', borderRadius: '50%' }}></div>
        <div style={{ position: 'absolute', bottom: '-10%', right: '-10%', width: '400px', height: '400px', background: '#3291ff', opacity: 0.1, filter: 'blur(80px)', borderRadius: '50%' }}></div>

        <div style={{ position: 'relative', zIndex: 1, maxWidth: '500px', color: 'white' }}>
          <div style={{ display: 'inline-flex', alignItems: 'center', gap: '12px', marginBottom: '40px', background: 'rgba(255,255,255,0.05)', padding: '12px 24px', borderRadius: '100px', border: '1px solid rgba(255,255,255,0.1)' }}>
            <img src="/logo.svg" alt="Dream Saver" style={{ width: '24px', height: '24px', borderRadius: '4px' }} />
            <span style={{ fontWeight: 600, letterSpacing: '0.05em', color: 'white' }}>DREAM SAVER</span>
          </div>

          <h1 style={{ fontSize: '48px', lineHeight: 1.1, marginBottom: '24px', fontWeight: 700, color: 'white' }}>
            Master your money,<br/>
            <span className="text-gradient">secure your future.</span>
          </h1>

          <p style={{ fontSize: '18px', color: 'rgba(255,255,255,0.7)', marginBottom: '48px', lineHeight: 1.6 }}>
            The all-in-one financial dashboard designed to help you track expenses, manage budgets, and chat with an AI assistant for personalized financial advice.
          </p>

          <div style={{ display: 'flex', flexDirection: 'column', gap: '24px' }}>
            <div style={{ display: 'flex', alignItems: 'flex-start', gap: '16px' }}>
              <div style={{ background: 'rgba(0, 112, 243, 0.1)', padding: '12px', borderRadius: '12px' }}>
                <TrendingUp size={24} color="var(--accent-blue)" />
              </div>
              <div>
                <h3 style={{ fontSize: '16px', marginBottom: '4px', color: 'white' }}>Advanced Analytics</h3>
                <p style={{ fontSize: '14px', color: 'rgba(255,255,255,0.7)' }}>Get a bird's eye view of your spending patterns.</p>
              </div>
            </div>

            <div style={{ display: 'flex', alignItems: 'flex-start', gap: '16px' }}>
              <div style={{ background: 'rgba(255, 255, 255, 0.05)', padding: '12px', borderRadius: '12px' }}>
                <ShieldCheck size={24} color="white" />
              </div>
              <div>
                <h3 style={{ fontSize: '16px', marginBottom: '4px', color: 'white' }}>Bank-grade Security</h3>
                <p style={{ fontSize: '14px', color: 'rgba(255,255,255,0.7)' }}>Your financial data is encrypted and securely stored.</p>
              </div>
            </div>
          </div>
        </div>
      </div>

      {/* Right Login Panel */}
      <div style={{ 
        flex: 1, 
        display: 'flex', 
        alignItems: 'center', 
        justifyContent: 'center',
        padding: '40px',
        flexDirection: 'column'
      }}>
        <div style={{ width: '100%', maxWidth: '420px' }}>
          <div className="dashboard-panel" style={{ padding: '40px', background: 'var(--bg-secondary)', border: '1px solid var(--border-light)', borderRadius: '16px', boxShadow: '0 20px 40px rgba(0,0,0,0.4)' }}>
            <div style={{ textAlign: 'center', marginBottom: '32px' }}>
              <h2 style={{ fontSize: '28px', marginBottom: '8px' }}>
                Welcome Back
              </h2>
              <p className="text-muted">Enter your details below to sign in.</p>
            </div>

            {error && (
              <div style={{ background: 'var(--danger-bg)', color: 'var(--danger)', padding: '12px', borderRadius: '8px', marginBottom: '24px', fontSize: '14px', border: '1px solid rgba(255,0,0,0.2)' }}>
                {error}
              </div>
            )}

            <form onSubmit={handleSubmit} style={{ display: 'flex', flexDirection: 'column', gap: '20px' }}>
              <div>
                <label style={{ display: 'block', marginBottom: '8px', fontSize: '14px', fontWeight: 500, color: 'var(--text-secondary)' }}>Email</label>
                <input 
                  type="email" 
                  className="input-field" 
                  placeholder="name@example.com"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  required
                />
              </div>
              
              <div>
                <label style={{ display: 'block', marginBottom: '8px', fontSize: '14px', fontWeight: 500, color: 'var(--text-secondary)' }}>Password</label>
                <input 
                  type="password" 
                  className="input-field" 
                  placeholder="••••••••"
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  required
                />
              </div>

              <button disabled={loading} type="submit" className="btn-primary" style={{ width: '100%', marginTop: '8px', padding: '14px' }}>
                Sign In
              </button>
            </form>

            <div style={{ margin: '32px 0', display: 'flex', alignItems: 'center', gap: '12px' }}>
              <div style={{ flex: 1, height: '1px', background: 'var(--border-light)' }}></div>
              <span className="text-muted" style={{ fontSize: '12px', textTransform: 'uppercase', letterSpacing: '0.05em' }}>Or continue with</span>
              <div style={{ flex: 1, height: '1px', background: 'var(--border-light)' }}></div>
            </div>

            <button disabled={loading} onClick={handleGoogleLogin} className="btn-google">
              <svg viewBox="0 0 24 24" width="18" height="18" xmlns="http://www.w3.org/2000/svg">
                <path d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z" fill="#4285F4"/>
                <path d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z" fill="#34A853"/>
                <path d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l2.85-2.22.81-.62z" fill="#FBBC05"/>
                <path d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z" fill="#EA4335"/>
              </svg>
              Google
            </button>
          </div>
        </div>
      </div>

    </div>
  );
}
