import React from 'react';
import { useAuth } from '../context/AuthContext';
import { useTheme } from '../context/ThemeContext';
import { NavLink, useNavigate } from 'react-router-dom';
import { LayoutDashboard, MessageSquare, BookOpen, LogOut, BrainCircuit, Moon, Sun } from 'lucide-react';

export default function Sidebar() {
  const { currentUser, logout } = useAuth();
  const { theme, toggleTheme } = useTheme();
  const navigate = useNavigate();

  async function handleLogout() {
    try {
      await logout();
      navigate('/login');
    } catch (error) {
      console.error('Failed to log out', error);
    }
  }

  const navItems = [
    { name: 'Dashboard', path: '/dashboard', icon: <LayoutDashboard size={20} /> },
    { name: 'AI Insights', path: '/ai-insights', icon: <BrainCircuit size={20} /> },
    { name: 'DreamSaver AI', path: '/chatbot', icon: <MessageSquare size={20} /> },
    { name: 'Financial Hub', path: '/blogs', icon: <BookOpen size={20} /> },
  ];

  return (
    <div style={{
      width: '260px',
      height: '100vh',
      position: 'fixed',
      left: 0,
      top: 0,
      background: 'var(--bg-secondary)',
      borderRight: '1px solid var(--border-light)',
      display: 'flex',
      flexDirection: 'column',
      padding: '24px 0',
      zIndex: 100
    }}>
      
      {/* Brand & Logo */}
      <div style={{ padding: '0 24px', marginBottom: '40px', display: 'flex', alignItems: 'center', gap: '12px' }}>
        <img src="/logo.svg" alt="Dream Saver Logo" style={{ width: '40px', height: '40px', borderRadius: '8px', boxShadow: '0 2px 4px rgba(0,0,0,0.1)' }} />
        <span style={{ fontSize: '20px', fontWeight: 700, letterSpacing: '-0.02em', color: 'var(--text-primary)' }}>DreamSaver</span>
      </div>

      {/* Navigation */}
      <div style={{ display: 'flex', flexDirection: 'column', gap: '8px', flex: 1, padding: '0 16px' }}>
        {navItems.map((item) => (
          <NavLink 
            key={item.path} 
            to={item.path}
            style={({ isActive }) => ({
              display: 'flex',
              alignItems: 'center',
              gap: '12px',
              padding: '12px 16px',
              borderRadius: '8px',
              color: isActive ? 'var(--accent-blue)' : 'var(--text-secondary)',
              background: isActive ? 'var(--bg-tertiary)' : 'transparent',
              textDecoration: 'none',
              fontWeight: 500,
              transition: 'all 0.2s'
            })}
          >
            {item.icon}
            {item.name}
          </NavLink>
        ))}
      </div>

      {/* Theme Toggle & User Profile Footer */}
      <div style={{ padding: '0 24px', marginTop: 'auto' }}>
        <button 
          onClick={toggleTheme}
          style={{ 
            display: 'flex', alignItems: 'center', gap: '12px', width: '100%', 
            padding: '12px', marginBottom: '16px', borderRadius: '8px', 
            background: 'var(--bg-tertiary)', border: '1px solid var(--border-light)', 
            color: 'var(--text-primary)', cursor: 'pointer', fontWeight: 500 
          }}
        >
          {theme === 'light' ? <Moon size={18} /> : <Sun size={18} />}
          {theme === 'light' ? 'Dark Mode' : 'Light Mode'}
        </button>

        <div style={{ display: 'flex', alignItems: 'center', gap: '12px', padding: '16px 0', borderTop: '1px solid var(--border-light)' }}>
          <div style={{ width: '40px', height: '40px', borderRadius: '50%', background: 'var(--accent-blue)', color: 'white', display: 'flex', alignItems: 'center', justifyContent: 'center', fontWeight: 600 }}>
            {currentUser?.email?.charAt(0).toUpperCase()}
          </div>
          <div style={{ flex: 1, overflow: 'hidden' }}>
            <div style={{ fontSize: '14px', fontWeight: 600, textOverflow: 'ellipsis', overflow: 'hidden', color: 'var(--text-primary)' }}>
              {currentUser?.displayName || 'User'}
            </div>
            <div style={{ fontSize: '12px', color: 'var(--text-secondary)', textOverflow: 'ellipsis', overflow: 'hidden' }}>
              {currentUser?.email}
            </div>
          </div>
          <button 
            onClick={handleLogout}
            style={{ background: 'none', border: 'none', color: 'var(--text-secondary)', cursor: 'pointer', transition: 'color 0.2s' }}
            onMouseOver={(e) => e.currentTarget.style.color = 'var(--danger)'}
            onMouseOut={(e) => e.currentTarget.style.color = 'var(--text-secondary)'}
            title="Sign Out"
          >
            <LogOut size={20} />
          </button>
        </div>
      </div>

    </div>
  );
}
