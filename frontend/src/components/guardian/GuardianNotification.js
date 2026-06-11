import React, { useState, useEffect } from 'react';
import './guardian.css';

function GuardianNotification({ guardianId, onRead }) {
  const [notifications, setNotifications] = useState([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => { loadNotifications(); markAsRead(); }, [guardianId]);

  const markAsRead = async () => {
    try {
      await fetch(`/api/v1/guardian/notifications/read-all?guardianId=${guardianId}`, { method: 'PUT' });
      if (onRead) onRead();
    } catch {}
  };

  const loadNotifications = async () => {
    setIsLoading(true); setError('');
    try {
      const res = await fetch(`/api/v1/guardian/notifications?guardianId=${guardianId}&limit=20`);
      const data = await res.json();
      if (data.code === 200) setNotifications(data.data || []);
      else setError(data.message || '加载失败');
    } catch { setError('网络连接失败'); }
    finally { setIsLoading(false); }
  };

  const getEventTypeLabel = (type) => ({
    fall: '跌倒报警', sos: '紧急求助', abnormal: '异常行为',
    medication_missed: '漏服药物', missed_dose: '漏服药物', missed_dose_alert: '漏服药物',
    emergency_alert: '紧急报警', expiring_drug: '药品临期',
    expiring_drug_reminder: '药品临期', other: '其他',
  }[type] || type);

  const translateMessage = (msg) => {
    if (!msg) return msg;
    return msg
      .replace(/morning/g, '早晨')
      .replace(/noon/g, '中午')
      .replace(/evening/g, '傍晚')
      .replace(/before_bed/g, '睡前');
  };

  const formatTime = (timeStr) => {
    if (!timeStr) return '';
    const date = new Date(timeStr);
    if (isNaN(date.getTime())) return timeStr;
    const now = new Date();
    const diff = Math.floor((now - date) / 1000);
    if (diff < 60) return '刚刚';
    if (diff < 3600) return Math.floor(diff / 60) + '分钟前';
    if (diff < 86400) return Math.floor(diff / 3600) + '小时前';
    if (diff < 172800) return '昨天 ' + date.getHours().toString().padStart(2, '0') + ':' + date.getMinutes().toString().padStart(2, '0');
    const m = date.getMonth() + 1;
    const d = date.getDate();
    const h = date.getHours().toString().padStart(2, '0');
    const min = date.getMinutes().toString().padStart(2, '0');
    return m + '月' + d + '日 ' + h + ':' + min;
  };

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
    <div>
      <div className="g-notif-header">
        <h2>通知记录</h2>
        <button className="g-btn g-btn-text" onClick={loadNotifications}>刷新</button>
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
              <div key={n.id} className="g-notif-item">
                <div className="g-notif-top">
                  <span className="g-notif-type">{getEventTypeLabel(n.eventType)}</span>
                  <span className={`g-notif-send ${send.cls}`}>{send.text}</span>
                </div>
                <p className="g-notif-msg">{translateMessage(n.message)}</p>
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
