import React, { useState } from 'react';
import { BookOpen, TrendingUp, ShieldCheck, Wallet, AlertTriangle, Zap, ChevronDown, ChevronUp } from 'lucide-react';

const tips = [
  {
    icon: TrendingUp,
    color: 'var(--success)',
    bg: 'rgba(20, 241, 149, 0.1)',
    title: 'The 50/30/20 Rule',
    summary: 'A simple and effective budgeting rule. Divide your after-tax income: 50% needs, 30% wants, 20% savings.',
    detail: 'Needs include rent, food, utilities, and transport. Wants include dining out, entertainment, and subscriptions. The 20% goes to savings or paying off debt. This rule works at any income level and gives you a guilt-free framework for spending. Track your categories in DreamSaver to see exactly which bucket each expense falls into.'
  },
  {
    icon: ShieldCheck,
    color: 'var(--accent-blue)',
    bg: 'rgba(0, 240, 255, 0.1)',
    title: 'Emergency Fund Basics',
    summary: 'Aim to save 3–6 months of living expenses in a liquid account for unexpected events.',
    detail: 'An emergency fund protects you from unexpected job loss, medical bills, or urgent home repairs without taking on debt. Start with a small goal — even LKR 50,000 is a meaningful cushion. Keep it in a separate account so you are not tempted to spend it. Use DreamSaver Goals to create a dedicated Emergency Fund target.'
  },
  {
    icon: BookOpen,
    color: 'var(--danger)',
    bg: 'rgba(255, 0, 128, 0.1)',
    title: 'Debt: Snowball vs Avalanche',
    summary: 'Choose your strategy: Snowball builds momentum. Avalanche saves the most money in interest.',
    detail: 'Snowball Method: Pay off your smallest debt first while making minimum payments on others. Once cleared, roll the payment to the next debt. This creates psychological wins and momentum. Avalanche Method: Pay off the highest-interest debt first. This is mathematically optimal — you pay less total interest over time. Which to choose? Snowball for motivation, Avalanche for efficiency.'
  },
  {
    icon: Wallet,
    color: '#a78bfa',
    bg: 'rgba(167, 139, 250, 0.1)',
    title: 'Pay Yourself First',
    summary: 'Transfer savings to a separate account the moment your salary arrives — before anything else.',
    detail: 'Most people save what is left after spending. Pay Yourself First flips this: you save first, then spend what remains. Even a small fixed amount each month builds a powerful habit and ensures savings actually happen. Set up a recurring savings deposit in DreamSaver on every income entry to make this automatic.'
  },
  {
    icon: AlertTriangle,
    color: '#f59e0b',
    bg: 'rgba(245, 158, 11, 0.1)',
    title: 'Avoid Lifestyle Inflation',
    summary: 'When income rises, resist the urge to scale up spending at the same rate.',
    detail: 'Lifestyle inflation (or lifestyle creep) is the tendency to increase spending whenever income rises. You get a raise and immediately upgrade your phone, eat out more, and rent a bigger apartment — so you never feel richer. Counter it by keeping fixed expenses stable when income grows, and directing extra income straight to savings or debt repayment. Check your month-over-month trends in the DreamSaver Dashboard.'
  },
  {
    icon: Zap,
    color: '#ec4899',
    bg: 'rgba(236, 72, 153, 0.1)',
    title: 'The Power of Compound Interest',
    summary: 'Your savings earn interest on their interest. Time is your most powerful financial asset.',
    detail: 'Compound interest means your money grows exponentially rather than linearly. LKR 10,000 saved at age 22 is worth far more by retirement than the same amount saved at 35 — simply because of time. The earlier you start, even with tiny amounts, the harder your money works. Start your savings goals as early as possible in DreamSaver, no matter the amount.'
  }
];

function TipCard({ icon: Icon, color, bg, title, summary, detail }) {
  const [expanded, setExpanded] = useState(false);

  return (
    <div className="dashboard-panel" style={{ marginBottom: '20px', cursor: 'pointer' }} onClick={() => setExpanded(e => !e)}>
      <div style={{ display: 'flex', alignItems: 'flex-start', gap: '16px' }}>
        <div style={{ background: bg, padding: '12px', borderRadius: '12px', flexShrink: 0 }}>
          <Icon size={24} color={color} />
        </div>
        <div style={{ flex: 1, minWidth: 0 }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', gap: '8px' }}>
            <h3 style={{ fontSize: '17px', marginBottom: '6px' }}>{title}</h3>
            {expanded
              ? <ChevronUp size={18} color="var(--text-muted)" style={{ flexShrink: 0 }} />
              : <ChevronDown size={18} color="var(--text-muted)" style={{ flexShrink: 0 }} />
            }
          </div>
          <p className="text-muted" style={{ fontSize: '14px', lineHeight: '1.5' }}>{summary}</p>

          {expanded && (
            <div style={{
              marginTop: '16px',
              paddingTop: '16px',
              borderTop: '1px solid var(--border)',
              animation: 'fadeIn 0.2s ease'
            }}>
              <p style={{ fontSize: '14px', lineHeight: '1.7', color: 'var(--text-secondary)' }}>{detail}</p>
              <div style={{
                marginTop: '14px',
                padding: '10px 14px',
                background: bg,
                borderRadius: '10px',
                fontSize: '13px',
                color: color,
                fontStyle: 'italic'
              }}>
                💡 Apply this in DreamSaver to track and act on this principle.
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}

export default function Learn() {
  return (
    <div>
      <div style={{ marginBottom: '32px' }}>
        <h1 style={{ fontSize: '32px', marginBottom: '8px' }}>Financial Education</h1>
        <p className="text-muted">6 essential principles to master your personal finances. Tap any card to learn more.</p>
      </div>

      <style>{`
        @keyframes fadeIn {
          from { opacity: 0; transform: translateY(-6px); }
          to   { opacity: 1; transform: translateY(0); }
        }
      `}</style>

      {tips.map((tip, i) => (
        <TipCard key={i} {...tip} />
      ))}
    </div>
  );
}
