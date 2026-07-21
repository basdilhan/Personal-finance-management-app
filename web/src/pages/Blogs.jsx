import React from 'react';
import { BookOpen, Clock, ArrowRight } from 'lucide-react';

export default function Blogs() {
  const blogs = [
    {
      id: 1,
      title: "Mastering the 50/30/20 Budgeting Rule",
      excerpt: "Learn how to divide your income between needs, wants, and savings to achieve financial freedom faster.",
      category: "Budgeting",
      readTime: "5 min read",
      image: "https://images.unsplash.com/photo-1554224155-8d04cb21cd6c?auto=format&fit=crop&q=80&w=600",
      url: "https://www.investopedia.com/ask/answers/022916/what-502030-budget-rule.asp"
    },
    {
      id: 2,
      title: "Emergency Funds: Your Financial Safety Net",
      excerpt: "Why you need 6 months of living expenses saved up, and exactly how to build it without feeling broke.",
      category: "Savings",
      readTime: "4 min read",
      image: "https://images.unsplash.com/photo-1633158829585-23ba8f7c8caf?auto=format&fit=crop&q=80&w=600",
      url: "https://www.nerdwallet.com/article/banking/emergency-fund-why-it-matters"
    },
    {
      id: 3,
      title: "The Avalanche vs. Snowball Method",
      excerpt: "Two proven strategies for paying off debt. Find out which psychological approach works best for you.",
      category: "Debt",
      readTime: "6 min read",
      image: "https://images.unsplash.com/photo-1579621970588-a35d0e7ab9b6?auto=format&fit=crop&q=80&w=600",
      url: "https://www.forbes.com/advisor/debt-relief/debt-snowball-vs-debt-avalanche/"
    }
  ];

  return (
    <div>
      <div style={{ marginBottom: '32px' }}>
        <h1 style={{ fontSize: '32px', marginBottom: '8px', display: 'flex', alignItems: 'center', gap: '12px' }}>
          <BookOpen color="var(--accent-blue)" /> Financial Hub
        </h1>
        <p className="text-muted">Expert articles to help you make smarter financial decisions.</p>
      </div>

      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(350px, 1fr))', gap: '24px' }}>
        {blogs.map(blog => (
          <div key={blog.id} className="dashboard-panel" style={{ padding: 0, overflow: 'hidden', display: 'flex', flexDirection: 'column' }}>
            <div style={{ height: '200px', width: '100%', overflow: 'hidden' }}>
              <img 
                src={blog.image} 
                alt={blog.title} 
                style={{ width: '100%', height: '100%', objectFit: 'cover', transition: 'transform 0.3s ease' }}
                onMouseOver={(e) => e.currentTarget.style.transform = 'scale(1.05)'}
                onMouseOut={(e) => e.currentTarget.style.transform = 'scale(1)'}
              />
            </div>
            
            <div style={{ padding: '24px', display: 'flex', flexDirection: 'column', flex: 1 }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '12px' }}>
                <span style={{ 
                  background: 'rgba(37, 99, 235, 0.1)', 
                  color: 'var(--accent-blue)', 
                  padding: '4px 12px', 
                  borderRadius: '100px', 
                  fontSize: '12px', 
                  fontWeight: 600 
                }}>
                  {blog.category}
                </span>
                <span className="text-muted" style={{ display: 'flex', alignItems: 'center', gap: '4px', fontSize: '12px' }}>
                  <Clock size={14} /> {blog.readTime}
                </span>
              </div>
              
              <h3 style={{ fontSize: '20px', marginBottom: '12px', lineHeight: 1.3 }}>{blog.title}</h3>
              <p className="text-muted" style={{ fontSize: '14px', marginBottom: '24px', flex: 1 }}>{blog.excerpt}</p>
              
              <a href={blog.url} target="_blank" rel="noopener noreferrer" style={{ textDecoration: 'none' }}>
                <button className="btn-secondary" style={{ width: '100%', display: 'flex', justifyContent: 'center', gap: '8px' }}>
                  Read Article <ArrowRight size={16} />
                </button>
              </a>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}
