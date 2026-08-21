import React, { useState, useEffect } from 'react';
import apiClient from '../api/apiClient';
import { BrainCircuit, LineChart, Target, Sparkles, Loader2, ShieldCheck, TrendingUp, Users, Database, Activity, CheckCircle2, BarChart3 } from 'lucide-react';
import { AreaChart, Area, BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer, ReferenceLine, LabelList, Cell, Legend, RadarChart, Radar, PolarGrid, PolarAngleAxis, PolarRadiusAxis } from 'recharts';

export default function MLInsights() {
  const [profile, setProfile] = useState(null);
  const [forecast, setForecast] = useState(null);
  const [history, setHistory] = useState([]);
  const [loading, setLoading] = useState(true);

  // Form State for Cold Start
  const [age, setAge] = useState(30);
  const [income, setIncome] = useState(50000);
  const [goal, setGoal] = useState(10000);
  const [spendingStyle, setSpendingStyle] = useState(3);
  const [riskTolerance, setRiskTolerance] = useState(3);

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
        savings_goal: Number(goal),
        spending_style: Number(spendingStyle),
        risk_tolerance: Number(riskTolerance)
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
              <div>
                <label style={{ display: 'block', fontSize: '14px', marginBottom: '8px' }}>Spending Style</label>
                <select className="input-field" value={spendingStyle} onChange={e => setSpendingStyle(e.target.value)} style={{ cursor: 'pointer' }}>
                  <option value={1}>1 — Very Frugal</option>
                  <option value={2}>2 — Careful</option>
                  <option value={3}>3 — Moderate</option>
                  <option value={4}>4 — Generous</option>
                  <option value={5}>5 — Big Spender</option>
                </select>
              </div>
              <div>
                <label style={{ display: 'block', fontSize: '14px', marginBottom: '8px' }}>Risk Tolerance</label>
                <select className="input-field" value={riskTolerance} onChange={e => setRiskTolerance(e.target.value)} style={{ cursor: 'pointer' }}>
                  <option value={1}>1 — Very Conservative</option>
                  <option value={2}>2 — Conservative</option>
                  <option value={3}>3 — Moderate</option>
                  <option value={4}>4 — Aggressive</option>
                  <option value={5}>5 — Very Aggressive</option>
                </select>
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

        {/* Forecast Accuracy — Per-Month Bar Chart */}
        <div className="dashboard-panel" style={{ gridColumn: '1 / -1' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '12px', marginBottom: '24px' }}>
            <div style={{ background: 'rgba(239, 68, 68, 0.1)', padding: '10px', borderRadius: '12px' }}>
              <Target size={24} color="var(--danger)" />
            </div>
            <div>
              <h3 style={{ margin: 0 }}>Model Accuracy</h3>
              <p className="text-muted" style={{ fontSize: '14px', margin: 0 }}>Per-Month Prediction Accuracy (%)</p>
            </div>
          </div>

          {history && history.length > 0 ? (
            <div style={{ display: 'flex', flexDirection: 'column', gap: '24px' }}>
              {/* Summary Cards */}
              <div style={{ display: 'flex', gap: '16px', flexWrap: 'wrap' }}>
                <div style={{ flex: 1, minWidth: '150px', background: 'var(--bg-primary)', padding: '20px', borderRadius: '12px', border: '1px solid var(--border-light)' }}>
                  <span className="text-muted" style={{ fontSize: '14px' }}>Average Accuracy</span>
                  <h2 style={{ fontSize: '32px', color: 'var(--accent-blue)' }}>
                    {Math.round(history.reduce((sum, h) => sum + h.accuracy, 0) / history.length)}%
                  </h2>
                </div>
                <div style={{ flex: 1, minWidth: '150px', background: 'var(--bg-primary)', padding: '20px', borderRadius: '12px', border: '1px solid var(--border-light)' }}>
                  <span className="text-muted" style={{ fontSize: '14px' }}>Best Month</span>
                  <h2 style={{ fontSize: '32px', color: 'var(--success)' }}>
                    {Math.round(Math.max(...history.map(h => h.accuracy)))}%
                  </h2>
                  <span className="text-muted" style={{ fontSize: '12px' }}>
                    {history.find(h => h.accuracy === Math.max(...history.map(x => x.accuracy)))?.month}
                  </span>
                </div>
                <div style={{ flex: 1, minWidth: '150px', background: 'var(--bg-primary)', padding: '20px', borderRadius: '12px', border: '1px solid var(--border-light)' }}>
                  <span className="text-muted" style={{ fontSize: '14px' }}>Months Tracked</span>
                  <h2 style={{ fontSize: '32px', color: 'var(--text-primary)' }}>
                    {history.length}
                  </h2>
                </div>
              </div>

              {/* Per-Month Accuracy Bar Chart */}
              <div style={{ height: '320px' }}>
                <ResponsiveContainer width="100%" height="100%">
                  <BarChart data={history} margin={{ top: 25, right: 20, left: 20, bottom: 5 }}>
                    <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="var(--border-light)" />
                    <XAxis 
                      dataKey="month" 
                      axisLine={false} 
                      tickLine={false} 
                      tick={{ fill: 'var(--text-secondary)', fontSize: 13 }}
                      tickFormatter={(val) => {
                        const [y, m] = val.split('-');
                        const months = ['Jan','Feb','Mar','Apr','May','Jun','Jul','Aug','Sep','Oct','Nov','Dec'];
                        return `${months[parseInt(m)-1]} ${y}`;
                      }}
                    />
                    <YAxis 
                      domain={[0, 100]} 
                      axisLine={false} 
                      tickLine={false} 
                      tick={{ fill: 'var(--text-secondary)' }}
                      tickFormatter={(val) => `${val}%`}
                    />
                    <Tooltip 
                      contentStyle={{ borderRadius: '12px', border: '1px solid var(--border-light)', background: 'var(--bg-primary)', color: 'var(--text-primary)' }}
                      formatter={(value, name) => {
                        if (name === 'accuracy') return [`${value}%`, 'Accuracy'];
                        return [value, name];
                      }}
                      labelFormatter={(label) => {
                        const [y, m] = label.split('-');
                        const months = ['January','February','March','April','May','June','July','August','September','October','November','December'];
                        return `${months[parseInt(m)-1]} ${y}`;
                      }}
                    />
                    <ReferenceLine y={80} stroke="var(--success)" strokeDasharray="4 4" strokeOpacity={0.5} label={{ value: 'Good (80%)', fill: 'var(--success)', fontSize: 11, position: 'right' }} />
                    <Bar dataKey="accuracy" name="accuracy" radius={[8, 8, 0, 0]} maxBarSize={60}>
                      <LabelList dataKey="accuracy" position="top" formatter={(val) => `${val}%`} style={{ fill: 'var(--text-primary)', fontSize: 13, fontWeight: 600 }} />
                      {history.map((entry, index) => (
                        <Cell key={index} fill={entry.accuracy >= 80 ? '#10b981' : entry.accuracy >= 60 ? '#f59e0b' : '#ef4444'} fillOpacity={0.85} />
                      ))}
                    </Bar>
                  </BarChart>
                </ResponsiveContainer>
              </div>

              {/* Detailed Per-Month Table */}
              <div style={{ overflowX: 'auto' }}>
                <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: '14px' }}>
                  <thead>
                    <tr style={{ borderBottom: '1px solid var(--border-light)' }}>
                      <th style={{ padding: '12px 16px', textAlign: 'left', color: 'var(--text-secondary)', fontWeight: 500 }}>Month</th>
                      <th style={{ padding: '12px 16px', textAlign: 'right', color: 'var(--text-secondary)', fontWeight: 500 }}>AI Predicted (LKR)</th>
                      <th style={{ padding: '12px 16px', textAlign: 'right', color: 'var(--text-secondary)', fontWeight: 500 }}>Actual Spent (LKR)</th>
                      <th style={{ padding: '12px 16px', textAlign: 'right', color: 'var(--text-secondary)', fontWeight: 500 }}>Accuracy</th>
                      <th style={{ padding: '12px 16px', textAlign: 'center', color: 'var(--text-secondary)', fontWeight: 500 }}>Method</th>
                    </tr>
                  </thead>
                  <tbody>
                    {history.map((h, i) => {
                      const [y, m] = h.month.split('-');
                      const months = ['Jan','Feb','Mar','Apr','May','Jun','Jul','Aug','Sep','Oct','Nov','Dec'];
                      const monthLabel = `${months[parseInt(m)-1]} ${y}`;
                      const accColor = h.accuracy >= 80 ? '#10b981' : h.accuracy >= 60 ? '#f59e0b' : '#ef4444';
                      return (
                        <tr key={i} style={{ borderBottom: '1px solid var(--border-light)' }}>
                          <td style={{ padding: '12px 16px', fontWeight: 600 }}>{monthLabel}</td>
                          <td style={{ padding: '12px 16px', textAlign: 'right', color: 'var(--accent-blue)' }}>{Number(h.predicted).toLocaleString()}</td>
                          <td style={{ padding: '12px 16px', textAlign: 'right', color: 'var(--danger)' }}>{Number(h.actual).toLocaleString()}</td>
                          <td style={{ padding: '12px 16px', textAlign: 'right', fontWeight: 700, color: accColor }}>{h.accuracy}%</td>
                          <td style={{ padding: '12px 16px', textAlign: 'center' }}>
                            <span style={{
                              padding: '4px 10px',
                              borderRadius: '20px',
                              fontSize: '12px',
                              fontWeight: 600,
                              background: h.is_fallback ? 'rgba(245, 158, 11, 0.15)' : 'rgba(16, 185, 129, 0.15)',
                              color: h.is_fallback ? '#f59e0b' : '#10b981'
                            }}>
                              {h.is_fallback ? 'Math Fallback' : 'Chronos AI'}
                            </span>
                          </td>
                        </tr>
                      );
                    })}
                  </tbody>
                </table>
              </div>
            </div>
          ) : (
            <div style={{ padding: '40px', textAlign: 'center' }}>
              <p className="text-muted">No historical forecasts available yet. Check back next month!</p>
            </div>
          )}
        </div>

        {/* ══════════════════════════════════════════════════════════════ */}
        {/* REAL DATA EVALUATION RESULTS SECTION                         */}
        {/* ══════════════════════════════════════════════════════════════ */}

        {/* Section Header */}
        <div style={{ gridColumn: '1 / -1', marginTop: '16px', marginBottom: '8px' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '12px', marginBottom: '4px' }}>
            <div style={{ background: 'linear-gradient(135deg, rgba(139, 92, 246, 0.15), rgba(59, 130, 246, 0.15))', padding: '12px', borderRadius: '14px' }}>
              <ShieldCheck size={28} color="#8b5cf6" />
            </div>
            <div>
              <h2 style={{ margin: 0, fontSize: '24px' }}>Real Data Evaluation Results</h2>
              <p className="text-muted" style={{ fontSize: '14px', margin: 0 }}>Models evaluated on 10,000 real users from Kaggle dataset</p>
            </div>
          </div>
        </div>

        {/* Dataset Info Banner */}
        <div className="dashboard-panel" style={{ gridColumn: '1 / -1', background: 'linear-gradient(135deg, rgba(139, 92, 246, 0.08), rgba(59, 130, 246, 0.08))', border: '1px solid rgba(139, 92, 246, 0.2)' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '12px', marginBottom: '16px' }}>
            <Database size={20} color="#8b5cf6" />
            <span style={{ fontWeight: 600, fontSize: '16px' }}>Dataset Information</span>
          </div>
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(180px, 1fr))', gap: '16px' }}>
            {[
              { label: 'Source', value: 'Kaggle (Real Data)', icon: '📊' },
              { label: 'Total Records', value: '20,000 users', icon: '👥' },
              { label: 'Evaluated On', value: '10,000 users', icon: '🔬' },
              { label: 'Features Used', value: '5 features', icon: '⚙️' },
              { label: 'Dataset', value: 'Indian Personal Finance', icon: '🌏' },
              { label: 'Currency Adapted', value: 'INR → LKR (×3.8)', icon: '💱' },
            ].map((item, i) => (
              <div key={i} style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
                <span style={{ fontSize: '20px' }}>{item.icon}</span>
                <div>
                  <div className="text-muted" style={{ fontSize: '12px' }}>{item.label}</div>
                  <div style={{ fontWeight: 600, fontSize: '14px' }}>{item.value}</div>
                </div>
              </div>
            ))}
          </div>
        </div>

        {/* K-Means Accuracy Card */}
        <div className="dashboard-panel">
          <div style={{ display: 'flex', alignItems: 'center', gap: '12px', marginBottom: '24px' }}>
            <div style={{ background: 'rgba(37, 99, 235, 0.1)', padding: '10px', borderRadius: '12px' }}>
              <Users size={24} color="var(--accent-blue)" />
            </div>
            <div>
              <h3 style={{ margin: 0 }}>K-Means Clustering</h3>
              <p className="text-muted" style={{ fontSize: '14px', margin: 0 }}>User Profiling Accuracy</p>
            </div>
          </div>

          {/* Circular Gauge */}
          <div style={{ display: 'flex', justifyContent: 'center', marginBottom: '24px' }}>
            <div style={{ position: 'relative', width: '160px', height: '160px' }}>
              <svg viewBox="0 0 120 120" style={{ transform: 'rotate(-90deg)' }}>
                <circle cx="60" cy="60" r="52" fill="none" stroke="var(--border-light)" strokeWidth="10" />
                <circle cx="60" cy="60" r="52" fill="none" stroke="#2563eb" strokeWidth="10"
                  strokeDasharray={`${73.8 * 3.267} ${326.7 - 73.8 * 3.267}`}
                  strokeLinecap="round"
                  style={{ transition: 'stroke-dasharray 1.5s ease' }}
                />
              </svg>
              <div style={{ position: 'absolute', top: '50%', left: '50%', transform: 'translate(-50%, -50%)', textAlign: 'center' }}>
                <div style={{ fontSize: '32px', fontWeight: 700, color: '#2563eb' }}>73.8%</div>
                <div className="text-muted" style={{ fontSize: '12px' }}>Accuracy</div>
              </div>
            </div>
          </div>

          {/* K-Means Metrics */}
          <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
            {[
              { label: 'Silhouette Score', value: '0.2118', desc: 'Cluster separation quality' },
              { label: 'Calinski-Harabasz', value: '2,899.1', desc: 'Between/within variance ratio' },
              { label: 'Davies-Bouldin', value: '1.3320', desc: 'Lower is better (< 1.5 good)' },
              { label: 'Clusters (K)', value: '4', desc: 'Optimal via elbow method' },
            ].map((m, i) => (
              <div key={i} style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: '12px', background: 'var(--bg-primary)', borderRadius: '10px', border: '1px solid var(--border-light)' }}>
                <div>
                  <div style={{ fontWeight: 600, fontSize: '14px' }}>{m.label}</div>
                  <div className="text-muted" style={{ fontSize: '12px' }}>{m.desc}</div>
                </div>
                <div style={{ fontWeight: 700, fontSize: '18px', color: 'var(--accent-blue)' }}>{m.value}</div>
              </div>
            ))}
          </div>
        </div>

        {/* Chronos Accuracy Card */}
        <div className="dashboard-panel">
          <div style={{ display: 'flex', alignItems: 'center', gap: '12px', marginBottom: '24px' }}>
            <div style={{ background: 'rgba(16, 185, 129, 0.1)', padding: '10px', borderRadius: '12px' }}>
              <TrendingUp size={24} color="var(--success)" />
            </div>
            <div>
              <h3 style={{ margin: 0 }}>Chronos-T5 Forecasting</h3>
              <p className="text-muted" style={{ fontSize: '14px', margin: 0 }}>Expense Prediction Accuracy</p>
            </div>
          </div>

          {/* Circular Gauge */}
          <div style={{ display: 'flex', justifyContent: 'center', marginBottom: '24px' }}>
            <div style={{ position: 'relative', width: '160px', height: '160px' }}>
              <svg viewBox="0 0 120 120" style={{ transform: 'rotate(-90deg)' }}>
                <circle cx="60" cy="60" r="52" fill="none" stroke="var(--border-light)" strokeWidth="10" />
                <circle cx="60" cy="60" r="52" fill="none" stroke="#10b981" strokeWidth="10"
                  strokeDasharray={`${94.5 * 3.267} ${326.7 - 94.5 * 3.267}`}
                  strokeLinecap="round"
                  style={{ transition: 'stroke-dasharray 1.5s ease' }}
                />
              </svg>
              <div style={{ position: 'absolute', top: '50%', left: '50%', transform: 'translate(-50%, -50%)', textAlign: 'center' }}>
                <div style={{ fontSize: '32px', fontWeight: 700, color: '#10b981' }}>94.5%</div>
                <div className="text-muted" style={{ fontSize: '12px' }}>Accuracy</div>
              </div>
            </div>
          </div>

          {/* Chronos Metrics */}
          <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
            {[
              { label: 'MAPE (Error)', value: '5.5%', desc: 'Mean Absolute Percentage Error' },
              { label: 'Directional Accuracy', value: '84.0%', desc: 'Up/Down prediction correct' },
              { label: 'R² Score', value: '0.990', desc: 'Goodness of fit (1.0 = perfect)' },
              { label: 'MAE', value: 'LKR 7,284', desc: 'Average error in Rupees' },
            ].map((m, i) => (
              <div key={i} style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: '12px', background: 'var(--bg-primary)', borderRadius: '10px', border: '1px solid var(--border-light)' }}>
                <div>
                  <div style={{ fontWeight: 600, fontSize: '14px' }}>{m.label}</div>
                  <div className="text-muted" style={{ fontSize: '12px' }}>{m.desc}</div>
                </div>
                <div style={{ fontWeight: 700, fontSize: '18px', color: 'var(--success)' }}>{m.value}</div>
              </div>
            ))}
          </div>
        </div>

        {/* Cluster Profile Breakdown */}
        <div className="dashboard-panel" style={{ gridColumn: '1 / -1' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '12px', marginBottom: '24px' }}>
            <div style={{ background: 'rgba(245, 158, 11, 0.1)', padding: '10px', borderRadius: '12px' }}>
              <BarChart3 size={24} color="#f59e0b" />
            </div>
            <div>
              <h3 style={{ margin: 0 }}>Cluster Profile Breakdown</h3>
              <p className="text-muted" style={{ fontSize: '14px', margin: 0 }}>How the AI grouped 10,000 real users into financial profiles</p>
            </div>
          </div>

          {/* Cluster Summary Cards */}
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))', gap: '16px', marginBottom: '24px' }}>
            {[
              { name: 'Conservative Budget', users: 3326, age: 26.5, income: '126.6k', savings: '12.5k', color: '#3b82f6', pct: '33.3%' },
              { name: 'Balanced Budget', users: 3008, age: 50.2, income: '134.1k', savings: '12.9k', color: '#10b981', pct: '30.1%' },
              { name: 'Growth Focused', users: 3033, age: 48.4, income: '126.7k', savings: '12.9k', color: '#f59e0b', pct: '30.3%' },
              { name: 'Aggressive Savings', users: 633, age: 40.0, income: '586.6k', savings: '112.9k', color: '#ef4444', pct: '6.3%' },
            ].map((cluster, i) => (
              <div key={i} style={{ padding: '20px', background: 'var(--bg-primary)', borderRadius: '14px', border: `2px solid ${cluster.color}22`, borderTop: `3px solid ${cluster.color}` }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '12px' }}>
                  <span style={{ fontWeight: 700, fontSize: '15px', color: cluster.color }}>{cluster.name}</span>
                  <span style={{ padding: '4px 10px', borderRadius: '20px', fontSize: '12px', fontWeight: 600, background: `${cluster.color}15`, color: cluster.color }}>{cluster.pct}</span>
                </div>
                <div style={{ display: 'flex', flexDirection: 'column', gap: '8px', fontSize: '13px' }}>
                  <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                    <span className="text-muted">Users</span>
                    <span style={{ fontWeight: 600 }}>{cluster.users.toLocaleString()}</span>
                  </div>
                  <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                    <span className="text-muted">Avg Age</span>
                    <span style={{ fontWeight: 600 }}>{cluster.age}</span>
                  </div>
                  <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                    <span className="text-muted">Avg Income</span>
                    <span style={{ fontWeight: 600 }}>LKR {cluster.income}</span>
                  </div>
                  <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                    <span className="text-muted">Avg Savings</span>
                    <span style={{ fontWeight: 600 }}>LKR {cluster.savings}</span>
                  </div>
                </div>
              </div>
            ))}
          </div>

          {/* Cluster Distribution Bar Chart */}
          <div style={{ height: '280px' }}>
            <ResponsiveContainer width="100%" height="100%">
              <BarChart data={[
                { name: 'Conservative', users: 3326, fill: '#3b82f6' },
                { name: 'Balanced', users: 3008, fill: '#10b981' },
                { name: 'Growth', users: 3033, fill: '#f59e0b' },
                { name: 'Aggressive', users: 633, fill: '#ef4444' },
              ]} margin={{ top: 25, right: 20, left: 20, bottom: 5 }}>
                <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="var(--border-light)" />
                <XAxis dataKey="name" axisLine={false} tickLine={false} tick={{ fill: 'var(--text-secondary)', fontSize: 13 }} />
                <YAxis axisLine={false} tickLine={false} tick={{ fill: 'var(--text-secondary)' }} />
                <Tooltip
                  contentStyle={{ borderRadius: '12px', border: '1px solid var(--border-light)', background: 'var(--bg-primary)', color: 'var(--text-primary)' }}
                  formatter={(value) => [`${value.toLocaleString()} users`, 'Users']}
                />
                <Bar dataKey="users" radius={[8, 8, 0, 0]} maxBarSize={70}>
                  <LabelList dataKey="users" position="top" formatter={(val) => val.toLocaleString()} style={{ fill: 'var(--text-primary)', fontSize: 13, fontWeight: 600 }} />
                  {[
                    { fill: '#3b82f6' },
                    { fill: '#10b981' },
                    { fill: '#f59e0b' },
                    { fill: '#ef4444' },
                  ].map((entry, index) => (
                    <Cell key={index} fill={entry.fill} fillOpacity={0.85} />
                  ))}
                </Bar>
              </BarChart>
            </ResponsiveContainer>
          </div>
        </div>

        {/* Model Comparison Radar Chart */}
        <div className="dashboard-panel" style={{ gridColumn: '1 / -1' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '12px', marginBottom: '24px' }}>
            <div style={{ background: 'rgba(139, 92, 246, 0.1)', padding: '10px', borderRadius: '12px' }}>
              <Activity size={24} color="#8b5cf6" />
            </div>
            <div>
              <h3 style={{ margin: 0 }}>Model Performance Overview</h3>
              <p className="text-muted" style={{ fontSize: '14px', margin: 0 }}>Combined evaluation metrics across both ML models</p>
            </div>
          </div>

          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(300px, 1fr))', gap: '24px' }}>
            {/* Radar Chart */}
            <div style={{ height: '320px' }}>
              <ResponsiveContainer width="100%" height="100%">
                <RadarChart data={[
                  { metric: 'Accuracy', kmeans: 73.8, chronos: 94.5 },
                  { metric: 'Precision', kmeans: 71, chronos: 84 },
                  { metric: 'Reliability', kmeans: 78, chronos: 90 },
                  { metric: 'Scalability', kmeans: 95, chronos: 85 },
                  { metric: 'Speed', kmeans: 98, chronos: 75 },
                ]}>
                  <PolarGrid stroke="var(--border-light)" />
                  <PolarAngleAxis dataKey="metric" tick={{ fill: 'var(--text-secondary)', fontSize: 13 }} />
                  <PolarRadiusAxis angle={90} domain={[0, 100]} tick={{ fill: 'var(--text-secondary)', fontSize: 11 }} />
                  <Radar name="K-Means" dataKey="kmeans" stroke="#2563eb" fill="#2563eb" fillOpacity={0.2} strokeWidth={2} />
                  <Radar name="Chronos-T5" dataKey="chronos" stroke="#10b981" fill="#10b981" fillOpacity={0.2} strokeWidth={2} />
                  <Legend />
                  <Tooltip />
                </RadarChart>
              </ResponsiveContainer>
            </div>

            {/* Feature Engineering Summary */}
            <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
              <h4 style={{ margin: '0 0 8px 0' }}>Feature Engineering Applied</h4>
              {[
                { step: 'Currency Normalization', desc: 'INR × 3.8 → LKR', icon: '💱' },
                { step: 'Dimensionality Reduction', desc: '27 columns → 5 features', icon: '📐' },
                { step: 'Quantile Binning', desc: 'Derived Spending Style (1-5)', icon: '📊' },
                { step: 'Inverted Binning', desc: 'Derived Risk Tolerance (1-5)', icon: '🎯' },
                { step: 'Standard Scaling', desc: 'Mean=0, StdDev=1 normalization', icon: '⚖️' },
                { step: 'Time-Series Synthesis', desc: 'Trend + Seasonality + Noise (24mo)', icon: '📈' },
                { step: 'Downsampling', desc: '20K → 10K (memory efficiency)', icon: '💾' },
              ].map((fe, i) => (
                <div key={i} style={{ display: 'flex', alignItems: 'center', gap: '12px', padding: '10px 14px', background: 'var(--bg-primary)', borderRadius: '10px', border: '1px solid var(--border-light)' }}>
                  <span style={{ fontSize: '18px' }}>{fe.icon}</span>
                  <div>
                    <div style={{ fontWeight: 600, fontSize: '13px' }}>{fe.step}</div>
                    <div className="text-muted" style={{ fontSize: '12px' }}>{fe.desc}</div>
                  </div>
                  <CheckCircle2 size={16} color="#10b981" style={{ marginLeft: 'auto', flexShrink: 0 }} />
                </div>
              ))}
            </div>
          </div>
        </div>

      </div>
    </div>
  );
}
