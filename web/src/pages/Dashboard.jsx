import React from 'react';
import { ArrowUpRight, ArrowDownRight, DollarSign, CreditCard } from 'lucide-react';

export default function Dashboard() {
  return (
    <div>
      <div style={{ marginBottom: '32px' }}>
        <h1 style={{ fontSize: '28px', marginBottom: '8px' }}>Financial Overview</h1>
        <p className="text-muted">Welcome back! Here's what's happening with your finances today.</p>
      </div>

      {/* KPI Cards */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(240px, 1fr))', gap: '24px', marginBottom: '40px' }}>
        
        <div className="dashboard-panel">
          <div className="flex-between" style={{ marginBottom: '16px' }}>
            <span className="text-muted" style={{ fontSize: '14px', fontWeight: 500 }}>Total Balance</span>
            <DollarSign size={18} className="text-muted" />
          </div>
          <h2 style={{ fontSize: '32px', marginBottom: '8px' }}>Rs. 124,500.00</h2>
          <div style={{ display: 'flex', alignItems: 'center', gap: '4px', color: 'var(--success)', fontSize: '14px', fontWeight: 500 }}>
            <ArrowUpRight size={16} />
            <span>+12.5% from last month</span>
          </div>
        </div>

        <div className="dashboard-panel">
          <div className="flex-between" style={{ marginBottom: '16px' }}>
            <span className="text-muted" style={{ fontSize: '14px', fontWeight: 500 }}>Monthly Expenses</span>
            <CreditCard size={18} className="text-muted" />
          </div>
          <h2 style={{ fontSize: '32px', marginBottom: '8px' }}>Rs. 42,300.00</h2>
          <div style={{ display: 'flex', alignItems: 'center', gap: '4px', color: 'var(--danger)', fontSize: '14px', fontWeight: 500 }}>
            <ArrowDownRight size={16} />
            <span>+4.2% from last month</span>
          </div>
        </div>

        <div className="dashboard-panel">
          <div className="flex-between" style={{ marginBottom: '16px' }}>
            <span className="text-muted" style={{ fontSize: '14px', fontWeight: 500 }}>Savings Goal</span>
            <div style={{ width: '8px', height: '8px', borderRadius: '50%', background: 'var(--success)' }}></div>
          </div>
          <h2 style={{ fontSize: '32px', marginBottom: '8px' }}>68%</h2>
          <div style={{ width: '100%', height: '6px', background: 'var(--surface-2)', borderRadius: '4px', overflow: 'hidden', marginTop: '12px' }}>
            <div style={{ width: '68%', height: '100%', background: 'var(--text-primary)' }}></div>
          </div>
        </div>
      </div>

      {/* Transactions Table */}
      <h2 style={{ fontSize: '20px', marginBottom: '16px' }}>Recent Transactions</h2>
      <div className="dashboard-panel" style={{ padding: 0, overflow: 'hidden' }}>
        <table style={{ width: '100%', borderCollapse: 'collapse', textAlign: 'left' }}>
          <thead>
            <tr style={{ borderBottom: '1px solid var(--border-light)', background: 'var(--surface-1)' }}>
              <th style={{ padding: '16px 24px', fontWeight: 500, fontSize: '13px', color: 'var(--text-secondary)' }}>Transaction</th>
              <th style={{ padding: '16px 24px', fontWeight: 500, fontSize: '13px', color: 'var(--text-secondary)' }}>Category</th>
              <th style={{ padding: '16px 24px', fontWeight: 500, fontSize: '13px', color: 'var(--text-secondary)' }}>Date</th>
              <th style={{ padding: '16px 24px', fontWeight: 500, fontSize: '13px', color: 'var(--text-secondary)', textAlign: 'right' }}>Amount</th>
            </tr>
          </thead>
          <tbody>
            {[
              { id: 1, name: 'Keells Supermarket', category: 'Groceries', date: 'Oct 24, 2026', amount: -4500 },
              { id: 2, name: 'Salary Deposit', category: 'Income', date: 'Oct 23, 2026', amount: 150000 },
              { id: 3, name: 'Uber Rides', category: 'Transport', date: 'Oct 21, 2026', amount: -1250 },
              { id: 4, name: 'Dialog Broadband', category: 'Bills', date: 'Oct 19, 2026', amount: -3900 },
            ].map((tx) => (
              <tr key={tx.id} style={{ borderBottom: '1px solid var(--border-light)' }}>
                <td style={{ padding: '16px 24px', fontSize: '14px', fontWeight: 500 }}>{tx.name}</td>
                <td style={{ padding: '16px 24px' }}>
                  <span style={{ padding: '4px 8px', background: 'var(--surface-2)', borderRadius: '4px', fontSize: '12px', fontWeight: 500 }}>
                    {tx.category}
                  </span>
                </td>
                <td style={{ padding: '16px 24px', fontSize: '14px', color: 'var(--text-secondary)' }}>{tx.date}</td>
                <td style={{ padding: '16px 24px', fontSize: '14px', fontWeight: 600, textAlign: 'right', color: tx.amount > 0 ? 'var(--success)' : 'var(--text-primary)' }}>
                  {tx.amount > 0 ? '+' : ''}Rs. {Math.abs(tx.amount).toLocaleString()}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}
