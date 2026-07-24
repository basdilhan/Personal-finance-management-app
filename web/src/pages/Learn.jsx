import React from 'react';
import { BookOpen, TrendingUp, ShieldCheck } from 'lucide-react';

export default function Learn() {
  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '32px' }}>
        <div>
          <h1 style={{ fontSize: '32px', marginBottom: '8px' }}>Financial Education</h1>
          <p className="text-muted">Master your money with our curated financial tips.</p>
        </div>
      </div>

      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(300px, 1fr))', gap: '24px' }}>
        <div className="dashboard-panel">
          <div style={{ display: 'flex', alignItems: 'center', gap: '16px', marginBottom: '16px' }}>
            <div style={{ background: 'rgba(20, 241, 149, 0.1)', padding: '12px', borderRadius: '12px' }}>
              <TrendingUp size={24} color="var(--success)" />
            </div>
            <h3 style={{ fontSize: '20px' }}>The 50/30/20 Rule</h3>
          </div>
          <p className="text-muted" style={{ marginBottom: '16px' }}>
            A simple and effective budgeting rule. Divide your after-tax income into three categories: 
            50% for needs, 30% for wants, and 20% for savings or paying off debt.
          </p>
          <button className="btn-primary" style={{ padding: '8px 16px', fontSize: '14px' }}>Read More</button>
        </div>

        <div className="dashboard-panel">
          <div style={{ display: 'flex', alignItems: 'center', gap: '16px', marginBottom: '16px' }}>
            <div style={{ background: 'rgba(0, 240, 255, 0.1)', padding: '12px', borderRadius: '12px' }}>
              <ShieldCheck size={24} color="var(--accent-blue)" />
            </div>
            <h3 style={{ fontSize: '20px' }}>Emergency Fund Basics</h3>
          </div>
          <p className="text-muted" style={{ marginBottom: '16px' }}>
            Aim to save 3 to 6 months of living expenses in a highly liquid account to protect against 
            unexpected job loss or medical bills. Start small if you need to!
          </p>
          <button className="btn-primary" style={{ padding: '8px 16px', fontSize: '14px' }}>Read More</button>
        </div>

        <div className="dashboard-panel">
          <div style={{ display: 'flex', alignItems: 'center', gap: '16px', marginBottom: '16px' }}>
            <div style={{ background: 'rgba(255, 0, 128, 0.1)', padding: '12px', borderRadius: '12px' }}>
              <BookOpen size={24} color="var(--danger)" />
            </div>
            <h3 style={{ fontSize: '20px' }}>Debt: Snowball vs Avalanche</h3>
          </div>
          <p className="text-muted" style={{ marginBottom: '16px' }}>
            The Snowball method focuses on paying off the smallest balances first for psychological wins. 
            The Avalanche method tackles the highest interest rates first to save money mathematically.
          </p>
          <button className="btn-primary" style={{ padding: '8px 16px', fontSize: '14px' }}>Read More</button>
        </div>
      </div>
    </div>
  );
}
