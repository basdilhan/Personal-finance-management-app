import React, { useState, useEffect } from 'react';
import { useAuth } from '../context/AuthContext';
import apiClient from '../api/apiClient';
import { User, ShieldCheck, Mail, Phone, Calendar, Loader2, Save, Sparkles } from 'lucide-react';

export default function Profile() {
  const { currentUser } = useAuth();
  
  const [profile, setProfile] = useState({
    displayName: '',
    email: '',
    phone: '',
    age: ''
  });
  const [isEditing, setIsEditing] = useState(false);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  
  const [advice, setAdvice] = useState('');
  const [loadingAdvice, setLoadingAdvice] = useState(false);

  useEffect(() => {
    fetchProfile();
  }, []);

  async function fetchProfile() {
    try {
      const res = await apiClient.get('/users/me');
      setProfile({
        displayName: res.data.displayName || currentUser?.displayName || '',
        email: res.data.email || currentUser?.email || '',
        phone: res.data.phone || '',
        age: res.data.age || ''
      });
      generateAdvice();
    } catch (err) {
      console.error("Failed to fetch profile", err);
    } finally {
      setLoading(false);
    }
  }

  async function generateAdvice() {
    setLoadingAdvice(true);
    try {
      // We pass a hidden prompt to the chatbot endpoint to generate a personalized financial summary
      const prompt = `Based on my latest financial data in the system, please provide a 2-3 paragraph professional financial assessment and actionable advice for someone my age. Summarize my financial standing.`;
      const res = await apiClient.post('/chat', { message: prompt });
      setAdvice(res.data.reply);
    } catch (err) {
      console.error("Failed to fetch advice", err);
      setAdvice("We are currently unable to generate personalized financial advice. Please try again later.");
    } finally {
      setLoadingAdvice(false);
    }
  }

  async function handleSave(e) {
    e.preventDefault();
    setSaving(true);
    try {
      await apiClient.post('/users', {
        displayName: profile.displayName,
        email: profile.email,
        phone: profile.phone,
        age: profile.age ? parseInt(profile.age) : 0
      });
      setIsEditing(false);
    } catch (err) {
      console.error("Failed to save profile", err);
    } finally {
      setSaving(false);
    }
  }

  if (loading) {
    return (
      <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: '60vh' }}>
        <Loader2 className="lucide-spin" size={48} color="var(--accent-blue)" />
      </div>
    );
  }

  return (
    <div>
      <div style={{ marginBottom: '32px' }}>
        <h1 style={{ fontSize: '32px', marginBottom: '8px', display: 'flex', alignItems: 'center', gap: '12px' }}>
          <User color="var(--accent-blue)" /> My Profile
        </h1>
        <p className="text-muted">Manage your personal details and view your personalized financial assessment.</p>
      </div>

      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '24px' }}>
        
        {/* Profile Details Panel */}
        <div className="dashboard-panel">
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '24px' }}>
            <h3 style={{ margin: 0, display: 'flex', alignItems: 'center', gap: '8px' }}>
              <ShieldCheck size={20} color="var(--success)" /> Account Details
            </h3>
            {!isEditing && (
              <button onClick={() => setIsEditing(true)} className="btn-secondary" style={{ padding: '6px 12px', fontSize: '12px' }}>
                Edit Profile
              </button>
            )}
          </div>

          <form onSubmit={handleSave} style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
            <div>
              <label style={{ display: 'block', fontSize: '14px', marginBottom: '8px', color: 'var(--text-secondary)' }}>
                <User size={14} style={{ marginRight: '4px', verticalAlign: 'text-bottom' }}/> Full Name
              </label>
              <input 
                type="text" 
                className="input-field" 
                value={profile.displayName} 
                onChange={e => setProfile({...profile, displayName: e.target.value})} 
                disabled={!isEditing}
              />
            </div>
            
            <div>
              <label style={{ display: 'block', fontSize: '14px', marginBottom: '8px', color: 'var(--text-secondary)' }}>
                <Mail size={14} style={{ marginRight: '4px', verticalAlign: 'text-bottom' }}/> Email Address
              </label>
              <input 
                type="email" 
                className="input-field" 
                value={profile.email} 
                onChange={e => setProfile({...profile, email: e.target.value})} 
                disabled={!isEditing}
              />
            </div>

            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '16px' }}>
              <div>
                <label style={{ display: 'block', fontSize: '14px', marginBottom: '8px', color: 'var(--text-secondary)' }}>
                  <Phone size={14} style={{ marginRight: '4px', verticalAlign: 'text-bottom' }}/> Phone Number
                </label>
                <input 
                  type="tel" 
                  className="input-field" 
                  value={profile.phone} 
                  onChange={e => setProfile({...profile, phone: e.target.value})} 
                  disabled={!isEditing}
                  placeholder="+94 77 123 4567"
                />
              </div>
              
              <div>
                <label style={{ display: 'block', fontSize: '14px', marginBottom: '8px', color: 'var(--text-secondary)' }}>
                  <Calendar size={14} style={{ marginRight: '4px', verticalAlign: 'text-bottom' }}/> Age
                </label>
                <input 
                  type="number" 
                  className="input-field" 
                  value={profile.age} 
                  onChange={e => setProfile({...profile, age: e.target.value})} 
                  disabled={!isEditing}
                  placeholder="e.g. 25"
                />
              </div>
            </div>

            {isEditing && (
              <div style={{ display: 'flex', gap: '12px', marginTop: '16px' }}>
                <button type="submit" className="btn-primary" disabled={saving} style={{ flex: 1, display: 'flex', justifyContent: 'center', alignItems: 'center', gap: '8px' }}>
                  {saving ? <Loader2 className="lucide-spin" size={18} /> : <Save size={18} />} Save Changes
                </button>
                <button type="button" onClick={() => setIsEditing(false)} className="btn-secondary" disabled={saving}>
                  Cancel
                </button>
              </div>
            )}
          </form>
        </div>

        {/* Financial Assessment Panel */}
        <div className="dashboard-panel" style={{ display: 'flex', flexDirection: 'column' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '12px', marginBottom: '24px' }}>
            <div style={{ background: 'rgba(37, 99, 235, 0.1)', padding: '10px', borderRadius: '12px' }}>
              <Sparkles size={24} color="var(--accent-blue)" />
            </div>
            <div>
              <h3 style={{ margin: 0 }}>Automated Financial Assessment</h3>
              <p className="text-muted" style={{ fontSize: '14px', margin: 0 }}>Generated by DreamSaver AI</p>
            </div>
          </div>

          <div style={{ 
            background: 'var(--bg-tertiary)', 
            padding: '24px', 
            borderRadius: '12px', 
            flex: 1, 
            border: '1px solid var(--border-light)',
            overflowY: 'auto'
          }}>
            {loadingAdvice ? (
              <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', height: '100%', gap: '12px', color: 'var(--text-secondary)' }}>
                <Loader2 className="lucide-spin" size={32} color="var(--accent-blue)" />
                <span>Analyzing your financial standing...</span>
              </div>
            ) : (
              <div style={{ lineHeight: 1.6, whiteSpace: 'pre-wrap' }}>
                {advice}
              </div>
            )}
          </div>
          
          <button onClick={generateAdvice} className="btn-secondary" style={{ width: '100%', marginTop: '16px' }} disabled={loadingAdvice}>
            Refresh Assessment
          </button>
        </div>

      </div>
    </div>
  );
}
