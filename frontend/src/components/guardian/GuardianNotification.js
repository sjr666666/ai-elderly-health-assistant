import React, { useState, useEffect } from 'react';
import { useToast } from '../Toast';
import { guardianApi } from '../../utils/guardianApi';
import { formatRelativeTime } from '../../utils/timeUtils';
import './guardian.css';

function GuardianNotification({ onRead }) {
  const { showToast } = useToast();
  const [notifications, setNotifications] = useState([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => { loadNotifications(); }, []);

  // 进入通知页时自动标记全部已读，角标清零
  useEffect(() => {
    const autoMarkAllRead = async () => {
      try {
        const data = await guardianApi.markAllAsRead();
        if (data.code === 200) {
          setNotifications(prev => prev.map(n => ({ ...n, isRead: true })));
          if (onRead) onRead();
        }
      } catch (e) { /* 静默失败，不影响用户体验 */ }
    };
    autoMarkAllRead();
  }, [onRead]);

  // 点击单条通知标记已读
  const handleItemClick = async (n) => {
    if (n.isRead) return;
    try {
      const data = await guardianApi.markOneAsRead(n.id);
      if (data.code === 200) {
        setNotifications(prev => prev.map(item => item.id === n.id ? { ...item, isRead: true } : item));
        if (onRead) onRead();
      }
    } catch (e) { /* 静默失败 */ }
  };

  const markAsRead = async () => {
    try {
      const data = await guardianApi.markAllAsRead();
      if (data.code === 200) {
        setNotifications(prev => prev.map(n => ({ ...n, isRead: true })));
        if (onRead) onRead();
        showToast('已全部标记为已读', 'success');
      } else {
        showToast(data.message || '操作失败', 'error');
      }
    } catch (e) { showToast(e.message || '操作失败', 'error'); }
  };

  const clearRead = async () => {
    try {
      const data = await guardianApi.clearReadNotifications();
      if (data.code === 200) {
        setNotifications(prev => prev.filter(n => !n.isRead));
        showToast(data.message || '已清除', 'success');
      } else {
        showToast(data.message || '清除失败', 'error');
      }
    } catch (e) { showToast(e.message || '清除失败', 'error'); }
  };

  const loadNotifications = async () => {
    setIsLoading(true); setError('');
    try {
      const data = await guardianApi.getNotifications();
      if (data.code === 200) setNotifications(data.data || []);
      else setError(data.message || '加载失败');
    } catch (e) { setError(e.message || '网络连接失败'); }
    finally { setIsLoading(false); }
  };

  const getEventTypeLabel = (type) => ({
    fall: '跌倒报警', sos: '紧急求助', abnormal: '异常行为',
    medication_missed: '漏服药物', missed_dose: '漏服药物', missed_dose_alert: '漏服药物',
    emergency_alert: '紧急报警', expiring_drug: '药品临期',
    expiring_drug_reminder: '药品临期', other: '其他',
  }[type] || type);

  // 时间格式化（使用统一工具函数，确保跨浏览器一致）
  const formatTime = (timeStr) => formatRelativeTime(timeStr);

  const getSendStatus = (status) => {
    switch (status) {
      case 'sent': return { text: '已发送', cls: 's-ok' };
      case 'failed': return { text: '发送失败', cls: 's-err' };
      case 'pending': return { text: '待发送', cls: 's-warn' };
      default: return { text: status, cls: '' };
    }
  };

  if (isLoading) return <div className="g-loading"><div className="g-spinner"></div><p>加载中...</p></div>;
  if (error) return <div className="g-error"><p>{error}</p><button className="g-btn g-btn-primary" onClick={loadNotifications}>重新加载</button></div>;

  return (
    <div className="g-notifications-page">
      <div className="g-notif-header">
        <h2>通知记录</h2>
        <div className="g-notif-header-actions">
          {notifications.some(n => !n.isRead) && (
            <button className="g-btn g-btn-text" onClick={markAsRead}>全部已读</button>
          )}
          {notifications.some(n => n.isRead) && (
            <button className="g-btn g-btn-text" onClick={clearRead}>清除已读</button>
          )}
          <button className="g-btn g-btn-text" onClick={loadNotifications}>刷新</button>
        </div>
      </div>

      {notifications.length === 0 ? (
        <div className="g-empty">
          <div className="g-empty-icon">
            <svg width="48" height="48" viewBox="0 0 24 24" fill="#CCC"><path d="M12 22c1.1 0 2-.9 2-2h-4c0 1.1.9 2 2 2zm6-6v-5c0-3.07-1.63-5.64-4.5-6.32V4c0-.83-.67-1.5-1.5-1.5s-1.5.67-1.5 1.5v.68C7.64 5.36 6 7.92 6 11v5l-2 2v1h16v-1l-2-2z"/></svg>
          </div>
          <p>暂无通知记录</p>
        </div>
      ) : (
        <div className="g-notif-list">
          {notifications.map((n) => {
            const send = getSendStatus(n.sendStatus);
            return (
              <div key={n.id} className={`g-notif-item ${n.isRead ? 'g-notif-item-read' : ''}`} onClick={() => handleItemClick(n)} style={{ cursor: 'pointer' }}>
                <div className="g-notif-top">
                  <span className="g-notif-type">{getEventTypeLabel(n.eventType)}</span>
                  <span className={`g-notif-send ${send.cls}`}>{send.text}</span>
                </div>
                <p className="g-notif-msg">{n.message}</p>
                <span className="g-notif-time">{formatTime(n.sentAt || n.createdAt)}</span>
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
}

export default GuardianNotification;
