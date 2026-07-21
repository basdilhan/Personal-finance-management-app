import React from 'react';
import { ArrowRight, BookOpen } from 'lucide-react';

export default function Blogs() {
  const articles = [
    {
      id: 1,
      category: 'Investing',
      title: 'The 50/30/20 Rule: A Simple Guide to Budgeting',
      excerpt: 'Learn how to split your income between needs, wants, and savings to build long-term financial stability without feeling restricted.',
      readTime: '5 min read',
      date: 'Oct 20, 2026',
      image: 'https://images.unsplash.com/photo-1579621970563-ebec7560ff3e?auto=format&fit=crop&q=80&w=800'
    },
    {
      id: 2,
      category: 'Saving',
      title: 'How to Build an Emergency Fund in 6 Months',
      excerpt: 'Unexpected expenses happen. Discover actionable strategies to build a 3-month emergency safety net even on a tight budget.',
      readTime: '8 min read',
      date: 'Oct 15, 2026',
      image: 'https://images.unsplash.com/photo-1601597111158-2fceff292cdc?auto=format&fit=crop&q=80&w=800'
    },
    {
      id: 3,
      category: 'Debt',
      title: 'Snowball vs. Avalanche: Paying Off Debt Fast',
      excerpt: 'Compare the two most popular debt payoff strategies and find out which psychological approach works best for your personal situation.',
      readTime: '6 min read',
      date: 'Oct 10, 2026',
      image: 'https://images.unsplash.com/photo-1554224155-8d04cb21cd6c?auto=format&fit=crop&q=80&w=800'
    }
  ];

  return (
    <div>
      <div style={{ marginBottom: '32px' }}>
        <h1 style={{ fontSize: '28px', marginBottom: '8px' }}>Financial Education</h1>
        <p className="text-muted">Master your money with our curated guides and articles.</p>
      </div>

      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(300px, 1fr))', gap: '24px' }}>
        {articles.map(article => (
          <div key={article.id} className="dashboard-panel" style={{ padding: 0, overflow: 'hidden', display: 'flex', flexDirection: 'column' }}>
            <div style={{ width: '100%', height: '200px', backgroundImage: `url(${article.image})`, backgroundSize: 'cover', backgroundPosition: 'center' }}></div>
            
            <div style={{ padding: '24px', flex: 1, display: 'flex', flexDirection: 'column' }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '12px' }}>
                <span style={{ color: 'var(--accent-blue)', fontSize: '13px', fontWeight: 600, textTransform: 'uppercase', letterSpacing: '0.05em' }}>
                  {article.category}
                </span>
                <span className="text-muted" style={{ fontSize: '13px' }}>
                  {article.readTime}
                </span>
              </div>
              
              <h2 style={{ fontSize: '20px', marginBottom: '12px' }}>{article.title}</h2>
              <p className="text-muted" style={{ fontSize: '14px', marginBottom: '24px', flex: 1 }}>
                {article.excerpt}
              </p>
              
              <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', borderTop: '1px solid var(--border-light)', paddingTop: '16px', marginTop: 'auto' }}>
                <span className="text-muted" style={{ fontSize: '13px' }}>{article.date}</span>
                <button style={{ background: 'none', border: 'none', color: 'var(--text-primary)', display: 'flex', alignItems: 'center', gap: '8px', fontSize: '14px', fontWeight: 500, cursor: 'pointer' }}>
                  Read More <ArrowRight size={16} />
                </button>
              </div>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}
