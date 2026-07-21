import React from 'react';
import { NavLink } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { LayoutDashboard, MessageSquare, BookOpen, LogOut, Wallet } from 'lucide-react';

export default function Sidebar() {
  const { logout, currentUser } = useAuth();

  return (
    <div className="sidebar">
      <div style={{ display: 'flex', alignItems: 'center', gap: '12px', marginBottom: '40px' }}>
        <div style={{ width: '32px', height: '32px', borderRadius: '8px', background: 'var(--accent-primary)', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
          <Wallet size={18} color="var(--bg-primary)" />
        </div>
        <h2 style={{ fontSize: '18px', fontWeight: 600 }}>Dream Saver</h2>
      </div>

      <nav style={{ flex: 1, display: 'flex', flexDirection: 'column', gap: '4px' }}>
        <NavLink 
          to="/dashboard" 
          className={({ isActive }) => `nav-item ${isActive ? 'active' : ''}`}
        >
          <LayoutDashboard size={18} />
          Overview
        </NavLink>
        
        <NavLink 
          to="/chatbot" 
          className={({ isActive }) => `nav-item ${isActive ? 'active' : ''}`}
        >
          <MessageSquare size={18} />
          AI Assistant
        </NavLink>
        
        <NavLink 
          to="/blogs" 
          className={({ isActive }) => `nav-item ${isActive ? 'active' : ''}`}
        >
          <BookOpen size={18} />
          Financial Blogs
        </NavLink>
      </nav>

      <div style={{ marginTop: 'auto', borderTop: '1px solid var(--border-light)', paddingTop: '24px' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '12px', marginBottom: '16px' }}>
          <div style={{ width: '32px', height: '32px', borderRadius: '50%', background: 'var(--surface-2)', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
            <span style={{ fontSize: '14px', fontWeight: 600, color: 'var(--text-primary)' }}>
              {currentUser?.email?.charAt(0).toUpperCase() || 'U'}
            </span>
          </div>
          <div style={{ overflow: 'hidden' }}>
            <p style={{ fontSize: '13px', fontWeight: 500, color: 'var(--text-primary)', whiteSpace: 'nowrap', textOverflow: 'ellipsis' }}>
              {currentUser?.email}
            </p>
          </div>
        </div>
        <button 
          onClick={logout}
          className="nav-item" 
          style={{ width: '100%', background: 'transparent', border: 'none', color: 'var(--danger)', cursor: 'pointer' }}
        >
          <LogOut size={18} />
          Sign Out
        </button>
      </div>
    </div>
  );
}
