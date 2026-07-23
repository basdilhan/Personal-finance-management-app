import React from 'react';
import { Loader2, AlertCircle, FileX } from 'lucide-react';

export const LoadingSpinner = ({ size = 48, fullHeight = true }) => (
  <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: fullHeight ? '60vh' : '100%' }}>
    <Loader2 className="lucide-spin" size={size} color="var(--accent-blue)" />
  </div>
);

export const ErrorState = ({ message = "An error occurred." }) => (
  <div style={{ display: 'flex', flexDirection: 'column', justifyContent: 'center', alignItems: 'center', height: '100%', padding: '24px', textAlign: 'center' }}>
    <AlertCircle size={48} color="var(--danger)" style={{ marginBottom: '16px' }} />
    <h3 style={{ marginBottom: '8px' }}>Something went wrong</h3>
    <p className="text-muted">{message}</p>
  </div>
);

export const EmptyState = ({ title = "No data found", message = "Get started by adding some records.", icon: Icon = FileX }) => (
  <div style={{ display: 'flex', flexDirection: 'column', justifyContent: 'center', alignItems: 'center', height: '100%', padding: '32px', textAlign: 'center' }}>
    <div style={{ background: 'var(--bg-tertiary)', padding: '16px', borderRadius: '50%', marginBottom: '16px' }}>
      <Icon size={32} color="var(--text-secondary)" />
    </div>
    <h4 style={{ marginBottom: '8px' }}>{title}</h4>
    <p className="text-muted" style={{ maxWidth: '250px' }}>{message}</p>
  </div>
);
