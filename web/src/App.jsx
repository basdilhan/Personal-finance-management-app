import React from 'react';
import { BrowserRouter as Router, Routes, Route, Navigate, useLocation } from 'react-router-dom';
import { AuthProvider, useAuth } from './context/AuthContext';
import { ThemeProvider } from './context/ThemeContext';
import { LanguageProvider } from './context/LanguageContext';
import { AnimatePresence, motion } from 'framer-motion';

// Components
import Sidebar from './components/Sidebar';
import { Menu } from 'lucide-react';

// Pages
import Login from './pages/Login';
import Dashboard from './pages/Dashboard';
import Chatbot from './pages/Chatbot';
import Blogs from './pages/Blogs';
import Learn from './pages/Learn';
import MLInsights from './pages/MLInsights';
import Profile from './pages/Profile';
import Reports from './pages/Reports';

// Protected Route Wrapper
const PrivateRoute = ({ children }) => {
  const { currentUser, loading } = useAuth();
  const [isMobileMenuOpen, setIsMobileMenuOpen] = React.useState(false);
  
  if (loading) return <div style={{ padding: '40px' }}>Loading DreamSaver...</div>;
  if (!currentUser) return <Navigate to="/login" />;
  
  return (
    <div className="layout-container">
      {/* Mobile Header */}
      <div className="mobile-header">
        <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
          <img src="/logo.svg" alt="Dream Saver Logo" style={{ width: '32px', height: '32px', borderRadius: '6px' }} />
          <span style={{ fontSize: '18px', fontWeight: 700, color: 'var(--text-primary)' }}>DreamSaver</span>
        </div>
        <button 
          onClick={() => setIsMobileMenuOpen(true)}
          style={{ background: 'none', border: 'none', color: 'var(--text-primary)', cursor: 'pointer' }}
        >
          <Menu size={24} />
        </button>
      </div>

      {/* Sidebar Overlay */}
      <div 
        className={`mobile-overlay ${isMobileMenuOpen ? 'open' : ''}`}
        onClick={() => setIsMobileMenuOpen(false)}
      ></div>

      <Sidebar isOpen={isMobileMenuOpen} setIsOpen={setIsMobileMenuOpen} />
      <div className="main-content">
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          exit={{ opacity: 0, y: -20 }}
          transition={{ duration: 0.3 }}
        >
          {children}
        </motion.div>
      </div>
    </div>
  );
};

// Animated Routes
const AnimatedRoutes = () => {
  const location = useLocation();

  return (
    <AnimatePresence mode="wait">
      <Routes location={location} key={location.pathname}>
        <Route path="/login" element={<Login />} />
        
        <Route path="/dashboard" element={
          <PrivateRoute>
            <Dashboard />
          </PrivateRoute>
        } />
        
        <Route path="/ai-insights" element={
          <PrivateRoute>
            <MLInsights />
          </PrivateRoute>
        } />

        <Route path="/chatbot" element={
          <PrivateRoute>
            <Chatbot />
          </PrivateRoute>
        } />
        
        <Route path="/blogs" element={
          <PrivateRoute>
            <Blogs />
          </PrivateRoute>
        } />
        
        <Route path="/learn" element={
          <PrivateRoute>
            <Learn />
          </PrivateRoute>
        } />

        <Route path="/reports" element={
          <PrivateRoute>
            <Reports />
          </PrivateRoute>
        } />

        <Route path="/profile" element={
          <PrivateRoute>
            <Profile />
          </PrivateRoute>
        } />

        <Route path="/" element={<Navigate to="/dashboard" />} />
      </Routes>
    </AnimatePresence>
  );
};

function App() {
  return (
    <ThemeProvider>
      <LanguageProvider>
        <AuthProvider>
          <Router>
            <AnimatedRoutes />
          </Router>
        </AuthProvider>
      </LanguageProvider>
    </ThemeProvider>
  );
}

export default App;
