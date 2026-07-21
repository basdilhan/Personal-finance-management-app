import React from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider, useAuth } from './context/AuthContext';

import Sidebar from './components/Sidebar';
import Login from './pages/Login';
import Dashboard from './pages/Dashboard';
import Chatbot from './pages/Chatbot';
import Blogs from './pages/Blogs';

// Protected Route Wrapper
const PrivateRoute = ({ children }) => {
  const { currentUser } = useAuth();
  return currentUser ? children : <Navigate to="/login" />;
};

// Layout Wrapper with Sidebar
const DashboardLayout = ({ children }) => {
  return (
    <div className="layout-container">
      <Sidebar />
      <div className="main-content">
        {children}
      </div>
    </div>
  );
};

export default function App() {
  return (
    <AuthProvider>
      <BrowserRouter>
        <Routes>
          <Route path="/login" element={<Login />} />
          
          <Route path="/" element={<Navigate to="/dashboard" />} />
          
          <Route path="/dashboard" element={
            <PrivateRoute>
              <DashboardLayout>
                <Dashboard />
              </DashboardLayout>
            </PrivateRoute>
          } />
          
          <Route path="/chatbot" element={
            <PrivateRoute>
              <DashboardLayout>
                <Chatbot />
              </DashboardLayout>
            </PrivateRoute>
          } />

          <Route path="/blogs" element={
            <PrivateRoute>
              <DashboardLayout>
                <Blogs />
              </DashboardLayout>
            </PrivateRoute>
          } />
        </Routes>
      </BrowserRouter>
    </AuthProvider>
  );
}
