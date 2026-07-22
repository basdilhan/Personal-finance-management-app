import React, { useState, useEffect } from 'react';
import { useAuth } from '../context/AuthContext';
import apiClient from '../api/apiClient';
import { AreaChart, Area, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer, BarChart, Bar } from 'recharts';
import { Download, TrendingUp, TrendingDown, Wallet, Loader2, Calendar } from 'lucide-react';
import { jsPDF } from 'jspdf';
import autoTable from 'jspdf-autotable';

export default function Dashboard() {
  const { currentUser } = useAuth();
  
  const [expenses, setExpenses] = useState([]);
  const [incomes, setIncomes] = useState([]);
  const [bills, setBills] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    async function fetchData() {
      try {
        const [expenseRes, incomeRes, billRes] = await Promise.all([
          apiClient.get('/expenses'),
          apiClient.get('/incomes'),
          apiClient.get('/bills').catch(() => ({ data: [] }))
        ]);
        setExpenses(expenseRes.data || []);
        setIncomes(incomeRes.data || []);
        setBills(billRes.data || []);
      } catch (error) {
        console.error("Error fetching financial data:", error);
      } finally {
        setLoading(false);
      }
    }
    fetchData();
  }, []);

  // Calculate KPIs
  const paidBillsTotal = bills.filter(b => b.status === 'paid').reduce((acc, curr) => acc + curr.amount, 0);
  const totalExpenses = expenses.reduce((acc, curr) => acc + curr.amount, 0) + paidBillsTotal;
  const totalIncome = incomes.reduce((acc, curr) => acc + curr.amount, 0);
  const balance = totalIncome - totalExpenses;

  // Process data for charts
  const getMonthlyData = () => {
    const data = {};
    
    // Add regular expenses
    expenses.forEach(e => {
      const date = new Date(e.date);
      const month = date.toLocaleString('default', { month: 'short' });
      if (!data[month]) data[month] = { name: month, Expenses: 0, Income: 0 };
      data[month].Expenses += e.amount;
    });

    // Add paid bills as expenses
    bills.filter(b => b.status === 'paid').forEach(b => {
      const date = new Date(b.dueDate);
      const month = date.toLocaleString('default', { month: 'short' });
      if (!data[month]) data[month] = { name: month, Expenses: 0, Income: 0 };
      data[month].Expenses += b.amount;
    });

    // Add incomes
    incomes.forEach(i => {
      const date = new Date(i.date);
      const month = date.toLocaleString('default', { month: 'short' });
      if (!data[month]) data[month] = { name: month, Expenses: 0, Income: 0 };
      data[month].Income += i.amount;
    });
    
    return Object.values(data);
  };

  const chartData = getMonthlyData();

  const exportPDF = () => {
    const doc = new jsPDF();
    doc.setFontSize(20);
    doc.text('DreamSaver Financial Report', 14, 22);
    
    doc.setFontSize(12);
    doc.text(`Generated on: ${new Date().toLocaleDateString()}`, 14, 32);
    doc.text(`Total Income: LKR ${totalIncome.toLocaleString()}`, 14, 42);
    doc.text(`Total Expenses: LKR ${totalExpenses.toLocaleString()}`, 14, 48);
    doc.text(`Net Balance: LKR ${balance.toLocaleString()}`, 14, 54);

    const tableData = expenses.map(e => [
      new Date(e.date).toLocaleDateString(),
      e.category,
      e.description || 'N/A',
      `LKR ${e.amount.toLocaleString()}`
    ]);

    autoTable(doc, {
      startY: 65,
      head: [['Date', 'Category', 'Description', 'Amount']],
      body: tableData,
    });

    doc.save('DreamSaver_Financial_Report.pdf');
  };

  if (loading) {
    return (
      <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: '60vh' }}>
        <Loader2 className="lucide-spin" size={48} color="var(--accent-blue)" />
      </div>
    );
  }

  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '32px' }}>
        <div>
          <h1 style={{ fontSize: '32px', marginBottom: '8px' }}>Dashboard Overview</h1>
          <p className="text-muted">Welcome back, {currentUser?.displayName || currentUser?.email}</p>
        </div>
        <button className="btn-primary" onClick={exportPDF} style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
          <Download size={18} /> Export PDF Report
        </button>
      </div>

      {/* KPI Cards */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(300px, 1fr))', gap: '24px', marginBottom: '32px' }}>
        
        <div className="dashboard-panel">
          <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '16px' }}>
            <span className="text-muted" style={{ fontWeight: 500 }}>Total Balance</span>
            <div style={{ background: 'rgba(37, 99, 235, 0.1)', padding: '8px', borderRadius: '8px' }}>
              <Wallet size={20} color="var(--accent-blue)" />
            </div>
          </div>
          <h2 style={{ fontSize: '36px', marginBottom: '8px' }}>LKR {balance.toLocaleString()}</h2>
        </div>

        <div className="dashboard-panel">
          <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '16px' }}>
            <span className="text-muted" style={{ fontWeight: 500 }}>Total Income</span>
            <div style={{ background: 'rgba(16, 185, 129, 0.1)', padding: '8px', borderRadius: '8px' }}>
              <TrendingUp size={20} color="var(--success)" />
            </div>
          </div>
          <h2 style={{ fontSize: '36px', marginBottom: '8px' }}>LKR {totalIncome.toLocaleString()}</h2>
        </div>

        <div className="dashboard-panel">
          <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '16px' }}>
            <span className="text-muted" style={{ fontWeight: 500 }}>Total Expenses</span>
            <div style={{ background: 'rgba(239, 68, 68, 0.1)', padding: '8px', borderRadius: '8px' }}>
              <TrendingDown size={20} color="var(--danger)" />
            </div>
          </div>
          <h2 style={{ fontSize: '36px', marginBottom: '8px' }}>LKR {totalExpenses.toLocaleString()}</h2>
        </div>
      </div>

      {/* Charts Section */}
      <div style={{ display: 'grid', gridTemplateColumns: '2fr 1fr', gap: '24px', marginBottom: '32px' }}>
        <div className="dashboard-panel">
          <h3 style={{ marginBottom: '24px' }}>Cash Flow Trend</h3>
          <div style={{ height: '300px' }}>
            <ResponsiveContainer width="100%" height="100%">
              <AreaChart data={chartData}>
                <defs>
                  <linearGradient id="colorIncome" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="5%" stopColor="var(--success)" stopOpacity={0.3}/>
                    <stop offset="95%" stopColor="var(--success)" stopOpacity={0}/>
                  </linearGradient>
                  <linearGradient id="colorExpense" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="5%" stopColor="var(--danger)" stopOpacity={0.3}/>
                    <stop offset="95%" stopColor="var(--danger)" stopOpacity={0}/>
                  </linearGradient>
                </defs>
                <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="var(--border-light)" />
                <XAxis dataKey="name" axisLine={false} tickLine={false} tick={{fill: 'var(--text-secondary)'}} />
                <YAxis axisLine={false} tickLine={false} tick={{fill: 'var(--text-secondary)'}} />
                <Tooltip contentStyle={{ borderRadius: '8px', border: '1px solid var(--border-light)', background: 'var(--bg-primary)', color: 'var(--text-primary)' }} />
                <Area type="monotone" dataKey="Income" stroke="var(--success)" fillOpacity={1} fill="url(#colorIncome)" />
                <Area type="monotone" dataKey="Expenses" stroke="var(--danger)" fillOpacity={1} fill="url(#colorExpense)" />
              </AreaChart>
            </ResponsiveContainer>
          </div>
        </div>

        <div className="dashboard-panel">
          <h3 style={{ marginBottom: '24px' }}>Monthly Comparison</h3>
          <div style={{ height: '300px' }}>
            <ResponsiveContainer width="100%" height="100%">
              <BarChart data={chartData}>
                <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="var(--border-light)" />
                <XAxis dataKey="name" axisLine={false} tickLine={false} tick={{fill: 'var(--text-secondary)'}} />
                <Tooltip cursor={{fill: 'rgba(255,255,255,0.05)'}} contentStyle={{ borderRadius: '8px', border: '1px solid var(--border-light)', background: 'var(--bg-primary)', color: 'var(--text-primary)' }} />
                <Bar dataKey="Income" fill="var(--success)" radius={[4, 4, 0, 0]} />
                <Bar dataKey="Expenses" fill="var(--danger)" radius={[4, 4, 0, 0]} />
              </BarChart>
            </ResponsiveContainer>
          </div>
        </div>
      </div>

      {/* Bottom Section: Transactions & Bills */}
      <div style={{ display: 'grid', gridTemplateColumns: '2fr 1fr', gap: '24px' }}>
        
        {/* Recent Transactions List */}
        <div className="dashboard-panel">
          <h3 style={{ marginBottom: '24px' }}>Recent Transactions</h3>
          {expenses.length === 0 ? (
            <p className="text-muted">No transactions found. Start using the mobile app to sync data!</p>
          ) : (
            <table style={{ width: '100%', borderCollapse: 'collapse' }}>
              <thead>
                <tr style={{ borderBottom: '2px solid var(--border-light)', textAlign: 'left' }}>
                  <th style={{ padding: '12px', color: 'var(--text-secondary)', fontWeight: 500 }}>Date</th>
                  <th style={{ padding: '12px', color: 'var(--text-secondary)', fontWeight: 500 }}>Category</th>
                  <th style={{ padding: '12px', color: 'var(--text-secondary)', fontWeight: 500, textAlign: 'right' }}>Amount</th>
                </tr>
              </thead>
              <tbody>
                {expenses.slice(0, 5).map((tx, idx) => (
                  <tr key={idx} style={{ borderBottom: '1px solid var(--border-light)' }}>
                    <td style={{ padding: '16px 12px' }}>{new Date(tx.date).toLocaleDateString()}</td>
                    <td style={{ padding: '16px 12px' }}>
                      <span style={{ background: 'var(--bg-tertiary)', padding: '4px 12px', borderRadius: '100px', fontSize: '12px', fontWeight: 500 }}>
                        {tx.category}
                      </span>
                    </td>
                    <td style={{ padding: '16px 12px', textAlign: 'right', fontWeight: 600, color: 'var(--danger)' }}>
                      - LKR {tx.amount.toLocaleString()}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>

        {/* Upcoming Bills List */}
        <div className="dashboard-panel">
          <h3 style={{ marginBottom: '24px' }}>Upcoming Bills</h3>
          {bills.length === 0 ? (
            <p className="text-muted">No upcoming bills found.</p>
          ) : (
            <div style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
              {bills.slice(0, 5).map((bill, idx) => (
                <div key={idx} style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: '12px', background: 'rgba(255,255,255,0.02)', borderRadius: '12px', border: '1px solid var(--border-light)' }}>
                  <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
                    <div style={{ background: 'rgba(245, 158, 11, 0.1)', padding: '8px', borderRadius: '8px' }}>
                      <Calendar size={18} color="var(--warning, #f59e0b)" />
                    </div>
                    <div>
                      <p style={{ margin: 0, fontWeight: 500 }}>{bill.name}</p>
                      <p className="text-muted" style={{ margin: 0, fontSize: '12px' }}>Due: {new Date(bill.dueDate).toLocaleDateString()}</p>
                    </div>
                  </div>
                  <span style={{ fontWeight: 600, color: 'var(--text-primary)' }}>
                    LKR {bill.amount.toLocaleString()}
                  </span>
                </div>
              ))}
            </div>
          )}
        </div>

      </div>

    </div>
  );
}
