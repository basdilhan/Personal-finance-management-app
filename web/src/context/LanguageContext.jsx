import React, { createContext, useContext, useState, useEffect } from 'react';

const LanguageContext = createContext();

export function useLanguage() {
  return useContext(LanguageContext);
}

export const translations = {
  en: {
    dashboard: 'Dashboard',
    reports: 'Reports & Analytics',
    profile: 'My Profile',
    aiInsights: 'AI Insights',
    chatbot: 'DreamSaver AI',
    blogs: 'Financial Hub',
    learn: 'Learn & Grow',
    welcome: 'Welcome back,',
    portfolioBalance: 'Portfolio Balance',
    cashIn: 'Cash In (This Month)',
    cashOut: 'Cash Out (This Month)',
    recentTransactions: 'Recent Transactions',
    noTransactions: 'No recent transactions',
    logout: 'Sign Out'
  },
  si: {
    dashboard: 'මුල් පිටුව',
    reports: 'වාර්තා සහ විශ්ලේෂණ',
    profile: 'මගේ ගිණුම',
    aiInsights: 'AI තොරතුරු',
    chatbot: 'DreamSaver AI',
    blogs: 'මූල්‍ය මධ්‍යස්ථානය',
    learn: 'ඉගෙන ගන්න',
    welcome: 'නැවත සාදරයෙන් පිළිගනිමු,',
    portfolioBalance: 'මුළු ශේෂය',
    cashIn: 'ආදායම් (මෙම මාසයේ)',
    cashOut: 'වියදම් (මෙම මාසයේ)',
    recentTransactions: 'මෑත ගනුදෙනු',
    noTransactions: 'මෑත ගනුදෙනු නොමැත',
    logout: 'පිටවීම'
  }
};

export function LanguageProvider({ children }) {
  const [language, setLanguage] = useState(() => {
    return localStorage.getItem('appLanguage') || 'en';
  });

  useEffect(() => {
    localStorage.setItem('appLanguage', language);
  }, [language]);

  const t = (key) => {
    return translations[language][key] || translations['en'][key] || key;
  };

  const toggleLanguage = () => {
    setLanguage(prev => prev === 'en' ? 'si' : 'en');
  };

  const value = {
    language,
    toggleLanguage,
    t
  };

  return (
    <LanguageContext.Provider value={value}>
      {children}
    </LanguageContext.Provider>
  );
}
