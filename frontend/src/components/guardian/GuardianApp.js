import React, { useState, useEffect } from 'react';
import GuardianLogin from './GuardianLogin';
import GuardianDashboard from './GuardianDashboard';
import GuardianElderDetail from './GuardianElderDetail';
import GuardianNotification from './GuardianNotification';
import './guardian.css';

function GuardianApp({ user: propUser, onLogout: propOnLogout }) {
  const [localUser, setLocalUser] = useState(null);
  const [activeTab, setActiveTab] = useState('dashboard');
  const [selectedElderId, setSelectedElderId] = useState(null);

  const user = propUser || localUser;

  useEffect(() => {
    if (!propUser) {
      const savedUser = localStorage.getItem('guardianUser');
      if (savedUser) {
        try {
          const userData = JSON.parse(savedUser);
          if (userData.role === 'family') setLocalUser(userData);
        } catch (e) { localStorage.removeItem('guardianUser'); }
      }
    }
  }, [propUser]);

  const handleLogin = (userData) => { setLocalUser(userData); };

  const handleLogout = () => {
    setLocalUser(null);
    setActiveTab('dashboard');
    setSelectedElderId(null);
    localStorage.removeItem('guardianUser');
    if (propOnLogout) propOnLogout();
  };

  const handleViewElder = (elderId) => {
    setSelectedElderId(elderId);
    setActiveTab('elderDetail');
  };

  const handleBackToDashboard = () => {
    setSelectedElderId(null);
    setActiveTab('dashboard');
  };

  if (!user) return <GuardianLogin onLogin={handleLogin} />;

  const renderContent = () => {
    switch (activeTab) {
      case 'dashboard':
        return <GuardianDashboard guardianId={user.id} onViewElder={handleViewElder} />;
      case 'elderDetail':
        return <GuardianElderDetail guardianId={user.id} elderId={selectedElderId} onBack={handleBackToDashboard} />;
      case 'notification':
        return <GuardianNotification guardianId={user.id} />;
      default:
        return <GuardianDashboard guardianId={user.id} onViewElder={handleViewElder} />;
    }
  };

  return (
    <div className="g-app">
      <div className="g-header">
        <div className="g-header-left">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="white"><path d="M16 11c1.66 0 2.99-1.34 2.99-3S17.66 5 16 5c-1.66 0-3 1.34-3 3s1.34 3 3 3zm-8 0c1.66 0 2.99-1.34 2.99-3S9.66 5 8 5C6.34 5 5 6.34 5 8s1.34 3 3 3zm0 2c-2.33 0-7 1.17-7 3.5V19h14v-2.5c0-2.33-4.67-3.5-7-3.5zm8 0c-.29 0-.62.02-.97.05 1.16.84 1.97 1.97 1.97 3.45V19h6v-2.5c0-2.33-4.67-3.5-7-3.5z"/></svg>
          <span className="g-header-title">家属守护</span>
        </div>
        <div className="g-header-right">
          <span className="g-header-user">{user.realName || user.username}</span>
          <button className="g-header-logout" onClick={handleLogout}>退出</button>
        </div>
      </div>

      <div className="g-body">{renderContent()}</div>

      <div className="g-tab-bar">
        <button className={`g-tab-item ${activeTab === 'dashboard' || activeTab === 'elderDetail' ? 'active' : ''}`}
          onClick={() => { setSelectedElderId(null); setActiveTab('dashboard'); }}>
          <svg viewBox="0 0 24 24" fill="currentColor"><path d="M10 20v-6h4v6h5v-8h3L12 3 2 12h3v8z"/></svg>
          <span>首页</span>
        </button>
        <button className={`g-tab-item ${activeTab === 'notification' ? 'active' : ''}`}
          onClick={() => setActiveTab('notification')}>
          <svg viewBox="0 0 24 24" fill="currentColor"><path d="M12 22c1.1 0 2-.9 2-2h-4c0 1.1.9 2 2 2zm6-6v-5c0-3.07-1.63-5.64-4.5-6.32V4c0-.83-.67-1.5-1.5-1.5s-1.5.67-1.5 1.5v.68C7.64 5.36 6 7.92 6 11v5l-2 2v1h16v-1l-2-2z"/></svg>
          <span>通知</span>
        </button>
        <button className="g-tab-item" onClick={handleLogout}>
          <svg viewBox="0 0 24 24" fill="currentColor"><path d="M17 7l-1.41 1.41L18.17 11H8v2h10.17l-2.58 2.58L17 17l5-5zM4 5h8V3H4c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h8v-2H4V5z"/></svg>
          <span>退出</span>
        </button>
      </div>
    </div>
  );
}

export default GuardianApp;
