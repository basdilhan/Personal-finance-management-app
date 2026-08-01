import React from 'react';
import { useAuth } from '../context/AuthContext';
import { useTheme } from '../context/ThemeContext';
import { useLanguage } from '../context/LanguageContext';
import { NavLink, useNavigate } from 'react-router-dom';
import { LayoutDashboard, MessageSquare, BookOpen, LogOut, BrainCircuit, Moon, Sun, User, Activity, Globe } from 'lucide-react';

export default function Sidebar({ isOpen, setIsOpen }) {
  const { currentUser, logout } = useAuth();
  const { theme, toggleTheme } = useTheme();
  const { language, toggleLanguage, t } = useLanguage();
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
    { name: t('dashboard'), path: '/dashboard', icon: <LayoutDashboard size={20} /> },
    { name: t('reports'), path: '/reports', icon: <Activity size={20} /> },
    { name: t('profile'), path: '/profile', icon: <User size={20} /> },
    { name: t('aiInsights'), path: '/ai-insights', icon: <BrainCircuit size={20} /> },
    { name: t('chatbot'), path: '/chatbot', icon: <MessageSquare size={20} /> },
    { name: t('blogs'), path: '/blogs', icon: <BookOpen size={20} /> },
    { name: t('learn'), path: '/learn', icon: <BookOpen size={20} /> },
  ];

  return (
    <div className={`sidebar-glass ${isOpen ? 'open' : ''}`} style={{
      width: '260px',
      height: '100vh',
      position: 'fixed',
      left: 0,
      top: 0,
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
            onClick={() => setIsOpen && setIsOpen(false)}
            className={({ isActive }) => isActive ? 'nav-item-active' : ''}
            style={{
              display: 'flex',
              alignItems: 'center',
              gap: '12px',
              padding: '12px 16px',
              borderRadius: '8px',
              color: 'var(--text-secondary)',
              textDecoration: 'none',
              fontWeight: 500,
              transition: 'all 0.3s ease'
            }}
          >
            {item.icon}
            {item.name}
          </NavLink>
        ))}
      </div>

      {/* User Profile Footer */}
      <div style={{ padding: '0 24px', marginTop: 'auto' }}>
        
        <div style={{ display: 'flex', gap: '16px', marginBottom: '16px' }}>
          <button 
            onClick={toggleLanguage}
            style={{ display: 'flex', alignItems: 'center', gap: '8px', background: 'none', border: 'none', color: 'var(--text-secondary)', cursor: 'pointer', fontSize: '14px', fontWeight: 500 }}
          >
            <Globe size={16} />
            {language === 'en' ? 'සිංහල' : 'English'}
          </button>

          <button 
            onClick={toggleTheme}
            style={{ display: 'flex', alignItems: 'center', gap: '8px', background: 'none', border: 'none', color: 'var(--text-secondary)', cursor: 'pointer', fontSize: '14px', fontWeight: 500 }}
          >
            {theme === 'dark' ? <Sun size={16} /> : <Moon size={16} />}
            {theme === 'dark' ? t('lightMode') || 'Light Mode' : t('darkMode') || 'Dark Mode'}
          </button>
        </div>

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
