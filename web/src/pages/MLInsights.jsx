import React, { useState, useEffect } from 'react';
import apiClient from '../api/apiClient';
import { BrainCircuit, LineChart, Target, Sparkles, Loader2 } from 'lucide-react';
import { AreaChart, Area, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer } from 'recharts';

export default function MLInsights() {
  const [profile, setProfile] = useState(null);
  const [forecast, setForecast] = useState(null);
  const [history, setHistory] = useState([]);
  const [loading, setLoading] = useState(true);

  // Form State for Cold Start
  const [age, setAge] = useState(30);
  const [income, setIncome] = useState(50000);
  const [goal, setGoal] = useState(10000);

  useEffect(() => {
    fetchForecast();
  }, []);

  async function fetchForecast() {
    try {
      const [forecastRes, historyRes] = await Promise.all([
        apiClient.get('/forecasts/ml-predict'),
        apiClient.get('/forecasts/history').catch(() => ({ data: [] }))
      ]);
      setForecast(forecastRes.data);
      setHistory(historyRes.data || []);
    } catch (err) {
      console.error("Failed to load forecast:", err);
    }
  }

  async function handleProfileAnalysis(e) {
    e.preventDefault();
    setLoading(true);
    try {
      const res = await apiClient.post('/users/profile-budget', {
        age: Number(age),
        income_bracket: String(income),
        savings_goal: Number(goal)
      });
      
      if (!res.data || !res.data.assigned_cluster) {
        alert("The ML Backend is currently starting up or offline. Please try again in 60 seconds.");
        return;
      }
      
      setProfile(res.data);
    } catch (err) {
      console.error("Failed to fetch ML Profile:", err);
      alert("Failed to connect to the ML Backend. Please try again later.");
    } finally {
      setLoading(false);
    }
  }

  // Format forecast data for Recharts
  const formatForecastData = () => {
    if (!forecast || !forecast.historical_data) return [];
    
    const data = [];
    const date = new Date();
    
    // Add 6 months of historical data
    for (let i = 5; i >= 0; i--) {
      const d = new Date(date.getFullYear(), date.getMonth() - i, 1);
      data.push({
        name: d.toLocaleString('default', { month: 'short' }),
        Actual: forecast.historical_data[5 - i]
      });
    }

    // Add predicted next month
    const nextMonth = new Date(date.getFullYear(), date.getMonth() + 1, 1);
    data.push({
      name: nextMonth.toLocaleString('default', { month: 'short' }),
      Predicted: forecast.predicted_next_month_expense
    });

    return data;
  };

  return (
    <div>
      <div style={{ marginBottom: '32px' }}>
        <h1 style={{ fontSize: '32px', marginBottom: '8px', display: 'flex', alignItems: 'center', gap: '12px' }}>
          <BrainCircuit color="var(--accent-blue)" /> AI Insights
        </h1>
        <p className="text-muted">Advanced machine learning analytics powered by DreamSaver AI.</p>
      </div>

      <div className="grid-1-1" style={{ marginBottom: '32px' }}>
        
        {/* Cold Start ML Clustering */}
        <div className="dashboard-panel">
          <div style={{ display: 'flex', alignItems: 'center', gap: '12px', marginBottom: '24px' }}>
            <div style={{ background: 'rgba(37, 99, 235, 0.1)', padding: '10px', borderRadius: '12px' }}>
              <Target size={24} color="var(--accent-blue)" />
            </div>
            <div>
              <h3 style={{ margin: 0 }}>Smart Budget Profiling</h3>
              <p className="text-muted" style={{ fontSize: '14px', margin: 0 }}>K-Means Clustering Analysis</p>
            </div>
          </div>

          {!profile ? (
            <form onSubmit={handleProfileAnalysis} style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
              <div>
                <label style={{ display: 'block', fontSize: '14px', marginBottom: '8px' }}>Your Age</label>
                <input type="number" className="input-field" value={age} onChange={e => setAge(e.target.value)} required />
              </div>
              <div>
                <label style={{ display: 'block', fontSize: '14px', marginBottom: '8px' }}>Monthly Income (LKR)</label>
                <input type="number" className="input-field" value={income} onChange={e => setIncome(e.target.value)} required />
              </div>
              <div>
                <label style={{ display: 'block', fontSize: '14px', marginBottom: '8px' }}>Monthly Savings Goal (LKR)</label>
                <input type="number" className="input-field" value={goal} onChange={e => setGoal(e.target.value)} required />
              </div>
              <button type="submit" className="btn-primary" style={{ marginTop: '8px' }}>Generate Profile</button>
            </form>
          ) : (
            <div style={{ animation: 'fadeIn 0.5s ease' }}>
              <div style={{ background: 'var(--bg-primary)', padding: '20px', borderRadius: '12px', border: '1px solid var(--border-light)' }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: '8px', color: 'var(--accent-blue)', fontWeight: 600, marginBottom: '12px' }}>
                  <Sparkles size={20} /> AI Assigned Profile
                </div>
                <h2 style={{ fontSize: '24px', marginBottom: '24px' }}>{profile.assigned_cluster}</h2>
                
                <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
                  <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                    <span className="text-muted">Recommended Budget</span>
                    <span style={{ fontWeight: 600 }}>LKR {profile.recommended_monthly_budget.toLocaleString()}</span>
                  </div>
                  <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                    <span className="text-muted">Recommended Savings</span>
                    <span style={{ fontWeight: 600 }}>LKR {profile.recommended_savings_goal.toLocaleString()}</span>
                  </div>
                </div>
                
                <button onClick={() => setProfile(null)} className="btn-secondary" style={{ width: '100%', marginTop: '24px' }}>
                  Recalculate
                </button>
              </div>
            </div>
          )}
        </div>

        {/* Time-Series Forecasting */}
        <div className="dashboard-panel">
          <div style={{ display: 'flex', alignItems: 'center', gap: '12px', marginBottom: '24px' }}>
            <div style={{ background: 'rgba(16, 185, 129, 0.1)', padding: '10px', borderRadius: '12px' }}>
              <LineChart size={24} color="var(--success)" />
            </div>
            <div>
              <h3 style={{ margin: 0 }}>Expense Forecasting</h3>
              <p className="text-muted" style={{ fontSize: '14px', margin: 0 }}>Chronos T5 Time-Series Prediction</p>
            </div>
          </div>

          {forecast ? (
            <div>
              <div style={{ marginBottom: '24px', background: 'var(--bg-primary)', padding: '20px', borderRadius: '12px', border: '1px solid var(--border-light)' }}>
                <span className="text-muted" style={{ fontSize: '14px' }}>Predicted Next Month Expense</span>
                <h2 style={{ fontSize: '32px', color: 'var(--success)' }}>
                  LKR {forecast.predicted_next_month_expense.toLocaleString()}
                </h2>
              </div>
              
              <div style={{ height: '200px' }}>
                <ResponsiveContainer width="100%" height="100%">
                  <AreaChart data={formatForecastData()}>
                    <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="var(--border-light)" />
                    <XAxis dataKey="name" axisLine={false} tickLine={false} tick={{fill: 'var(--text-secondary)'}} />
                    <Tooltip cursor={{stroke: 'var(--border-light)', strokeWidth: 1, strokeDasharray: '5 5'}} />
                    <Area type="monotone" dataKey="Actual" stroke="var(--text-secondary)" fill="var(--bg-tertiary)" strokeWidth={2} />
                    <Area type="monotone" dataKey="Predicted" stroke="var(--success)" fill="rgba(16, 185, 129, 0.1)" strokeWidth={2} strokeDasharray="5 5" />
                  </AreaChart>
                </ResponsiveContainer>
              </div>
            </div>
          ) : (
            <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: '200px' }}>
              <Loader2 className="lucide-spin" size={32} color="var(--text-secondary)" />
            </div>
          )}
        </div>

        {/* Forecast Accuracy Comparison */}
        <div className="dashboard-panel" style={{ gridColumn: '1 / -1' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '12px', marginBottom: '24px' }}>
            <div style={{ background: 'rgba(239, 68, 68, 0.1)', padding: '10px', borderRadius: '12px' }}>
              <Target size={24} color="var(--danger)" />
            </div>
            <div>
              <h3 style={{ margin: 0 }}>Model Accuracy</h3>
              <p className="text-muted" style={{ fontSize: '14px', margin: 0 }}>Predicted vs Actual Past Expenses</p>
            </div>
          </div>

          {history && history.length > 0 ? (
            <div style={{ display: 'flex', flexDirection: 'column', gap: '24px' }}>
              <div style={{ display: 'flex', gap: '16px' }}>
                <div style={{ flex: 1, background: 'var(--bg-primary)', padding: '20px', borderRadius: '12px', border: '1px solid var(--border-light)' }}>
                  <span className="text-muted" style={{ fontSize: '14px' }}>Average Accuracy</span>
                  <h2 style={{ fontSize: '32px', color: 'var(--accent-blue)' }}>
                    {Math.round(history.reduce((sum, h) => sum + h.accuracy, 0) / history.length)}%
                  </h2>
                </div>
              </div>
              
              <div style={{ height: '300px' }}>
                <ResponsiveContainer width="100%" height="100%">
                  <AreaChart data={history}>
                    <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="var(--border-light)" />
                    <XAxis dataKey="month" axisLine={false} tickLine={false} tick={{fill: 'var(--text-secondary)'}} />
                    <YAxis axisLine={false} tickLine={false} tick={{fill: 'var(--text-secondary)'}} />
                    <Tooltip contentStyle={{ borderRadius: '8px', border: '1px solid var(--border-light)', background: 'var(--bg-primary)', color: 'var(--text-primary)' }} />
                    <Area type="monotone" dataKey="actual" name="Actual Spent" stroke="var(--danger)" fillOpacity={0.1} fill="var(--danger)" strokeWidth={2} />
                    <Area type="monotone" dataKey="predicted" name="AI Predicted" stroke="var(--accent-blue)" fillOpacity={0.1} fill="var(--accent-blue)" strokeWidth={2} strokeDasharray="5 5" />
                  </AreaChart>
                </ResponsiveContainer>
              </div>
            </div>
          ) : (
            <div style={{ padding: '40px', textAlign: 'center' }}>
              <p className="text-muted">No historical forecasts available yet. Check back next month!</p>
            </div>
          )}
        </div>

      </div>
    </div>
  );
}
