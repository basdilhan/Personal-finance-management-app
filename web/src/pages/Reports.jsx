import React, { useState, useEffect } from 'react';
import { useAuth } from '../context/AuthContext';
import apiClient from '../api/apiClient';
import { Radar, RadarChart, PolarGrid, PolarAngleAxis, PolarRadiusAxis, ResponsiveContainer, BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, Legend, PieChart, Pie, Cell } from 'recharts';
import { FileText, ShieldAlert, ShieldCheck, Shield, Activity, Download, Loader2 } from 'lucide-react';
import { jsPDF } from 'jspdf';
import autoTable from 'jspdf-autotable';

export default function Reports() {
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

  // Compute metrics
  const totalExpenses = expenses.reduce((acc, curr) => acc + curr.amount, 0);
  const totalIncome = incomes.reduce((acc, curr) => acc + curr.amount, 0);
  const paidBills = bills.filter(b => b.status && b.status.toLowerCase() === 'paid');
  const totalPaidBills = paidBills.reduce((acc, curr) => acc + curr.amount, 0);
  const trueTotalExpenses = totalExpenses + totalPaidBills;
  const savings = totalIncome - trueTotalExpenses;
  const savingsRate = totalIncome > 0 ? (savings / totalIncome) * 100 : 0;

  // Financial Health Score Algorithm
  let healthScore = 50; // Baseline
  if (totalIncome > 0) {
    if (savingsRate > 20) healthScore += 30;
    else if (savingsRate > 0) healthScore += 15;
    else healthScore -= 20;

    const fixedExpenses = expenses.filter(e => ['housing', 'utilities', 'insurance'].includes(e.category.toLowerCase())).reduce((acc, curr) => acc + curr.amount, 0);
    const fixedRatio = fixedExpenses / totalIncome;
    
    if (fixedRatio < 0.3) healthScore += 20;
    else if (fixedRatio > 0.5) healthScore -= 20;
  }
  healthScore = Math.max(0, Math.min(100, healthScore));

  const getHealthStatus = () => {
    if (healthScore >= 80) return { label: 'Excellent', color: 'var(--success)', icon: <ShieldCheck size={32} color="var(--success)" /> };
    if (healthScore >= 50) return { label: 'Fair', color: 'var(--warning)', icon: <Shield size={32} color="var(--warning)" /> };
    return { label: 'Critical', color: 'var(--danger)', icon: <ShieldAlert size={32} color="var(--danger)" /> };
  };
  const healthStatus = getHealthStatus();

  // Radar Chart Data (Category Breakdown)
  const getCategoryData = () => {
    const categories = {};
    expenses.forEach(e => {
      const cat = e.category || 'Other';
      categories[cat] = (categories[cat] || 0) + e.amount;
    });
    // Add bills as a category
    if (totalPaidBills > 0) categories['Paid Bills'] = totalPaidBills;

    return Object.keys(categories).map(key => ({
      category: key,
      amount: categories[key],
      fullMark: Math.max(...Object.values(categories)) * 1.2 || 1000
    }));
  };
  const radarData = getCategoryData();

  // Generate Professional PDF Report
  const generatePDF = () => {
    const doc = new jsPDF();
    const pageWidth = doc.internal.pageSize.getWidth();

    // Brand Header
    doc.setFillColor(37, 99, 235); // Accent blue
    doc.rect(0, 0, pageWidth, 40, 'F');
    doc.setTextColor(255, 255, 255);
    doc.setFontSize(24);
    doc.text('DreamSaver', 14, 25);
    
    doc.setFontSize(10);
    doc.text('Official Financial Statement', pageWidth - 14, 25, { align: 'right' });

    // Executive Summary
    doc.setTextColor(0, 0, 0);
    doc.setFontSize(16);
    doc.text('Executive Summary', 14, 55);
    doc.setFontSize(11);
    doc.text(`Account Holder: ${currentUser?.displayName || currentUser?.email || 'N/A'}`, 14, 65);
    doc.text(`Statement Generated: ${new Date().toLocaleDateString()}`, 14, 72);
    
    doc.text(`Total Income: LKR ${totalIncome.toLocaleString()}`, 14, 82);
    doc.text(`Total Expenses (Incl. Bills): LKR ${trueTotalExpenses.toLocaleString()}`, 14, 89);
    doc.text(`Net Savings: LKR ${savings.toLocaleString()}`, 14, 96);
    doc.text(`Financial Health Score: ${healthScore.toFixed(0)}/100 (${healthStatus.label})`, 14, 103);

    // Incomes Table
    autoTable(doc, {
      startY: 115,
      head: [['Date', 'Source', 'Amount (LKR)']],
      body: incomes.map(i => [new Date(i.date).toLocaleDateString(), i.source || 'N/A', i.amount.toLocaleString()]),
      headStyles: { fillColor: [16, 185, 129] }, // Success green
      margin: { top: 10 }
    });

    // Expenses Table
    let finalY = doc.lastAutoTable.finalY || 115;
    autoTable(doc, {
      startY: finalY + 15,
      head: [['Date', 'Category', 'Description', 'Amount (LKR)']],
      body: expenses.map(e => [new Date(e.date).toLocaleDateString(), e.category, e.description || 'N/A', e.amount.toLocaleString()]),
      headStyles: { fillColor: [239, 68, 68] }, // Danger red
    });

    // Bills Table
    finalY = doc.lastAutoTable.finalY || finalY + 15;
    if (bills.length > 0) {
      autoTable(doc, {
        startY: finalY + 15,
        head: [['Due Date', 'Bill Name', 'Status', 'Amount (LKR)']],
        body: bills.map(b => [new Date(b.dueDate).toLocaleDateString(), b.name, b.status || 'Pending', b.amount.toLocaleString()]),
        headStyles: { fillColor: [245, 158, 11] }, // Warning orange
      });
    }

    // Footer
    const pageCount = doc.internal.getNumberOfPages();
    for (let i = 1; i <= pageCount; i++) {
      doc.setPage(i);
      doc.setFontSize(8);
      doc.setTextColor(150);
      doc.text(`Page ${i} of ${pageCount} - DreamSaver Secure Document`, pageWidth / 2, doc.internal.pageSize.getHeight() - 10, { align: 'center' });
    }

    doc.save(`DreamSaver_Statement_${new Date().toISOString().slice(0,10)}.pdf`);
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
          <h1 style={{ fontSize: '32px', marginBottom: '8px' }}>Reports & Analytics</h1>
          <p className="text-muted">Deep dive into your financial health and cash flows.</p>
        </div>
        <button className="btn-primary" onClick={generatePDF} style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
          <Download size={18} /> Download Full Statement
        </button>
      </div>

      <div style={{ display: 'grid', gridTemplateColumns: '1fr 2fr', gap: '24px', marginBottom: '32px' }}>
        
        {/* Health Score Panel */}
        <div className="dashboard-panel" style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', textAlign: 'center' }}>
          <h3 style={{ marginBottom: '16px', alignSelf: 'flex-start' }}>Financial Health Score</h3>
          <div style={{ position: 'relative', width: '150px', height: '150px', display: 'flex', alignItems: 'center', justifyContent: 'center', marginBottom: '24px' }}>
            <svg width="150" height="150" viewBox="0 0 150 150">
              <circle cx="75" cy="75" r="65" fill="none" stroke="var(--border-light)" strokeWidth="12" />
              <circle cx="75" cy="75" r="65" fill="none" stroke={healthStatus.color} strokeWidth="12" strokeDasharray={`${(healthScore/100) * 408} 408`} transform="rotate(-90 75 75)" style={{ transition: 'stroke-dasharray 1s ease-in-out' }} />
            </svg>
            <div style={{ position: 'absolute', display: 'flex', flexDirection: 'column', alignItems: 'center' }}>
              <span style={{ fontSize: '36px', fontWeight: 'bold', color: 'var(--text-primary)' }}>{healthScore.toFixed(0)}</span>
              <span className="text-muted" style={{ fontSize: '12px' }}>/ 100</span>
            </div>
          </div>
          <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '8px' }}>
            {healthStatus.icon}
            <span style={{ fontSize: '24px', fontWeight: 'bold', color: healthStatus.color }}>{healthStatus.label}</span>
          </div>
          <p className="text-muted" style={{ fontSize: '14px', maxWidth: '200px' }}>
            Based on your savings rate and fixed expenses ratio.
          </p>
        </div>

        {/* Category Radar Chart */}
        <div className="dashboard-panel">
          <h3 style={{ marginBottom: '24px' }}>Spending Intensity Radar</h3>
          {radarData.length === 0 ? (
            <div style={{ height: '300px', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
              <p className="text-muted">Not enough expense data to generate radar.</p>
            </div>
          ) : (
            <div style={{ height: '300px' }}>
              <ResponsiveContainer width="100%" height="100%">
                <RadarChart cx="50%" cy="50%" outerRadius="80%" data={radarData}>
                  <PolarGrid stroke="var(--border-light)" />
                  <PolarAngleAxis dataKey="category" tick={{ fill: 'var(--text-secondary)', fontSize: 12 }} />
                  <PolarRadiusAxis angle={30} domain={[0, 'auto']} tick={{ fill: 'var(--text-muted)' }} />
                  <Radar name="Spending" dataKey="amount" stroke="var(--accent-blue)" fill="var(--accent-blue)" fillOpacity={0.5} />
                  <Tooltip contentStyle={{ borderRadius: '8px', border: '1px solid var(--border-light)', backgroundColor: 'var(--bg-primary)', color: 'var(--text-primary)' }} />
                </RadarChart>
              </ResponsiveContainer>
            </div>
          )}
        </div>
      </div>

      {/* Summary Cards */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: '24px' }}>
        <div className="dashboard-panel">
          <div style={{ display: 'flex', alignItems: 'center', gap: '12px', marginBottom: '16px' }}>
            <div style={{ background: 'rgba(16, 185, 129, 0.1)', padding: '10px', borderRadius: '8px' }}>
              <Activity size={20} color="var(--success)" />
            </div>
            <span style={{ fontWeight: 500 }}>Savings Rate</span>
          </div>
          <h2 style={{ fontSize: '28px', marginBottom: '4px' }}>{savingsRate.toFixed(1)}%</h2>
          <p className="text-muted" style={{ fontSize: '13px' }}>Target: &gt; 20%</p>
        </div>

        <div className="dashboard-panel">
          <div style={{ display: 'flex', alignItems: 'center', gap: '12px', marginBottom: '16px' }}>
            <div style={{ background: 'rgba(239, 68, 68, 0.1)', padding: '10px', borderRadius: '8px' }}>
              <FileText size={20} color="var(--danger)" />
            </div>
            <span style={{ fontWeight: 500 }}>True Expenses</span>
          </div>
          <h2 style={{ fontSize: '28px', marginBottom: '4px' }}>LKR {trueTotalExpenses.toLocaleString()}</h2>
          <p className="text-muted" style={{ fontSize: '13px' }}>Includes Paid Bills</p>
        </div>

        <div className="dashboard-panel">
          <div style={{ display: 'flex', alignItems: 'center', gap: '12px', marginBottom: '16px' }}>
            <div style={{ background: 'rgba(37, 99, 235, 0.1)', padding: '10px', borderRadius: '8px' }}>
              <Shield size={20} color="var(--accent-blue)" />
            </div>
            <span style={{ fontWeight: 500 }}>Net Flow</span>
          </div>
          <h2 style={{ fontSize: '28px', marginBottom: '4px', color: savings > 0 ? 'var(--success)' : 'var(--danger)' }}>
            {savings > 0 ? '+' : ''}LKR {savings.toLocaleString()}
          </h2>
          <p className="text-muted" style={{ fontSize: '13px' }}>Total Income - True Expenses</p>
        </div>
      </div>

    </div>
  );
}
