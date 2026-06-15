import React, { useState, useEffect, useCallback } from 'react';
import GuardianLogin from './GuardianLogin';
import GuardianDashboard from './GuardianDashboard';
import GuardianElderDetail from './GuardianElderDetail';
import GuardianNotification from './GuardianNotification';
import GuardianProfile from './GuardianProfile';
import './guardian.css';

function GuardianApp({ user: propUser, onLogout: propOnLogout }) {
  const [localUser, setLocalUser] = useState(null);
  const [activeTab, setActiveTab] = useState('dashboard');
  const [selectedElderId, setSelectedElderId] = useState(null);
  const [unreadCount, setUnreadCount] = useState(0);

  const user = propUser || localUser;

  const fetchUnreadCount = useCallback(() => {
    if (!user) return;
    fetch(`/api/v1/guardian/notifications/unread-count?guardianId=${user.id}`)
      .then(res => res.json())
      .then(data => { if (data.code === 200) setUnreadCount(data.data); })
      .catch(() => {});
  }, [user]);

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

  // 登录后轮询未读数（3秒间隔，近实时更新）
  useEffect(() => {
    if (!user) return;
    fetchUnreadCount();
    const timer = setInterval(fetchUnreadCount, 3000);
    // 页面聚焦时立即刷新
    const onFocus = () => fetchUnreadCount();
    window.addEventListener('focus', onFocus);
    return () => { clearInterval(timer); window.removeEventListener('focus', onFocus); };
  }, [user, fetchUnreadCount]);

  const handleLogin = (userData) => { setLocalUser(userData); };

  const handleLogout = () => {
    setLocalUser(null);
    setActiveTab('dashboard');
    setSelectedElderId(null);
    setUnreadCount(0);
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

  const handleTabNotification = () => {
    setActiveTab('notification');
  };

  // 通知页标记已读后的回调
  const handleNotificationsRead = () => {
    fetchUnreadCount();
  };

  if (!user) return <GuardianLogin onLogin={handleLogin} />;

  const renderContent = () => {
    switch (activeTab) {
      case 'dashboard':
        return <GuardianDashboard guardianId={user.id} onViewElder={handleViewElder} />;
      case 'elderDetail':
        return <GuardianElderDetail guardianId={user.id} elderId={selectedElderId} onBack={handleBackToDashboard} />;
      case 'notification':
        return <GuardianNotification guardianId={user.id} onRead={handleNotificationsRead} />;
      case 'profile':
        return <GuardianProfile user={user} onLogout={handleLogout} />;
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
          onClick={handleTabNotification} style={{ position: 'relative' }}>
          <svg viewBox="0 0 24 24" fill="currentColor"><path d="M12 22c1.1 0 2-.9 2-2h-4c0 1.1.9 2 2 2zm6-6v-5c0-3.07-1.63-5.64-4.5-6.32V4c0-.83-.67-1.5-1.5-1.5s-1.5.67-1.5 1.5v.68C7.64 5.36 6 7.92 6 11v5l-2 2v1h16v-1l-2-2z"/></svg>
          <span>通知</span>
          {unreadCount > 0 && <span className="g-tab-badge">{unreadCount > 99 ? '99+' : unreadCount}</span>}
        </button>
        <button className={`g-tab-item ${activeTab === 'profile' ? 'active' : ''}`}
          onClick={() => setActiveTab('profile')}>
          <svg viewBox="0 0 24 24" fill="currentColor"><path d="M12 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm0 2c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z"/></svg>
          <span>个人</span>
        </button>
      </div>
    </div>
  );
}

export default GuardianApp;
