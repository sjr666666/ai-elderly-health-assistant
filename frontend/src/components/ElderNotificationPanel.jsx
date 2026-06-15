import React, { useState, useEffect, useCallback } from 'react';
import { useToast } from './Toast';

/**
 * 老人端通知面板组件
 * 右侧滑出面板，展示通知列表
 */
const ElderNotificationPanel = ({ elderId, isOpen, onClose, onUnreadCountChange, onContactAdded }) => {
  const { showToast } = useToast();
  const [notifications, setNotifications] = useState([]);
  const [unreadCount, setUnreadCount] = useState(0);
  const [loading, setLoading] = useState(false);

  // 加载通知列表
  const loadNotifications = useCallback(async () => {
    if (!elderId) return;
    try {
      const res = await fetch(`/api/v1/elder/notifications?elderId=${elderId}&limit=20`);
      const data = await res.json();
      if (data.code === 200) {
        setNotifications(data.data || []);
      }
    } catch (e) {
      console.error('加载通知失败', e);
    }
  }, [elderId]);

  // 加载未读数
  const fetchUnreadCount = useCallback(async () => {
    if (!elderId) return;
    try {
      const res = await fetch(`/api/v1/elder/notifications/unread-count?elderId=${elderId}`);
      const data = await res.json();
      if (data.code === 200) {
        setUnreadCount(data.data || 0);
        if (onUnreadCountChange) onUnreadCountChange(data.data || 0);
      }
    } catch (e) {
      // 静默失败
    }
  }, [elderId, onUnreadCountChange]);

  // 面板打开时加载数据并标记已读
  useEffect(() => {
    if (isOpen && elderId) {
      loadNotifications();
      // 延迟标记已读，让用户先看到未读状态
      const timer = setTimeout(async () => {
        try {
          await fetch(`/api/v1/elder/notifications/read-all?elderId=${elderId}`, { method: 'PUT' });
          fetchUnreadCount();
        } catch (e) {
          // 静默失败
        }
      }, 1500);
      return () => clearTimeout(timer);
    }
  }, [isOpen, elderId, loadNotifications, fetchUnreadCount]);

  // 定时轮询未读数
  useEffect(() => {
    if (!elderId) return;
    fetchUnreadCount();
    const timer = setInterval(fetchUnreadCount, 5000);
    return () => clearInterval(timer);
  }, [elderId, fetchUnreadCount]);

  // 添加紧急联系人
  const handleAddContact = async (notificationId) => {
    try {
      const res = await fetch(`/api/v1/elder/notifications/${notificationId}/add-contact`, {
        method: 'POST',
      });
      const data = await res.json();
      if (data.code === 200) {
        // 更新本地通知状态
        setNotifications(prev =>
          prev.map(n => n.id === notificationId ? { ...n, isHandled: 1, isRead: 1 } : n)
        );
        showToast('已添加为紧急联系人', 'success');
        // 通知父组件刷新紧急联系人列表
        if (onContactAdded) onContactAdded();
      } else {
        showToast(data.message || '添加失败', data.message && data.message.includes('重复') ? 'warning' : 'error');
      }
    } catch (e) {
      showToast('添加失败，请重试', 'error');
    }
  };

  // 忽略通知
  const handleDismiss = async (notificationId) => {
    try {
      await fetch(`/api/v1/elder/notifications/${notificationId}/read`, { method: 'PUT' });
      setNotifications(prev =>
        prev.map(n => n.id === notificationId ? { ...n, isHandled: 1, isRead: 1 } : n)
      );
    } catch (e) {
      // 静默失败
    }
  };

  // 更新紧急联系人电话
  const handleUpdatePhone = async (notificationId) => {
    try {
      const res = await fetch(`/api/v1/elder/notifications/${notificationId}/update-phone`, {
        method: 'POST',
      });
      const data = await res.json();
      if (data.code === 200) {
        setNotifications(prev =>
          prev.map(n => n.id === notificationId ? { ...n, isHandled: 1, isRead: 1 } : n)
        );
        showToast('紧急联系人电话已更新', 'success');
        if (onContactAdded) onContactAdded();
      } else {
        showToast(data.message || '更新失败', 'error');
      }
    } catch (e) {
      showToast('更新失败，请重试', 'error');
    }
  };

  // 一键已读
  const handleMarkAllRead = async () => {
    try {
      await fetch(`/api/v1/elder/notifications/read-all?elderId=${elderId}`, { method: 'PUT' });
      setNotifications(prev => prev.map(n => ({ ...n, isRead: 1 })));
      fetchUnreadCount();
      showToast('已全部标记为已读', 'success');
    } catch (e) {
      showToast('操作失败', 'error');
    }
  };

  // 清除已读通知
  const handleClearRead = async () => {
    try {
      const res = await fetch(`/api/v1/elder/notifications/read?elderId=${elderId}`, { method: 'DELETE' });
      const data = await res.json();
      if (data.code === 200) {
        setNotifications(prev => prev.filter(n => n.isRead === 0));
        showToast(data.message || '已清除', 'success');
      } else {
        showToast(data.message || '清除失败', 'error');
      }
    } catch (e) {
      showToast('清除失败', 'error');
    }
  };

  // 通知类型图标
  const getTypeIcon = (type) => {
    switch (type) {
      case 'bind_request':
        return (
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="#4CAF50" strokeWidth="2">
            <path d="M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2" />
            <circle cx="9" cy="7" r="4" />
            <path d="M22 21v-2a4 4 0 0 0-3-3.87" />
            <path d="M16 3.13a4 4 0 0 1 0 7.75" />
          </svg>
        );
      case 'phone_update':
        return (
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="#3A7BC8" strokeWidth="2">
            <path d="M6.62 10.79c1.44 2.83 3.76 5.14 6.59 6.59l2.2-2.2c.27-.27.67-.36 1.02-.24 1.12.37 2.33.57 3.57.57.55 0 1 .45 1 1V20c0 .55-.45 1-1 1-9.39 0-17-7.61-17-17 0-.55.45-1 1-1h3.5c.55 0 1 .45 1 1 0 1.25.2 2.45.57 3.57.11.35.03.74-.25 1.02l-2.2 2.2z" />
          </svg>
        );
      case 'system':
        return (
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="#F5A623" strokeWidth="2">
            <circle cx="12" cy="12" r="10" />
            <line x1="12" y1="16" x2="12" y2="12" />
            <line x1="12" y1="8" x2="12.01" y2="8" />
          </svg>
        );
      default:
        return (
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="#4CAF50" strokeWidth="2">
            <path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9" />
            <path d="M13.73 21a2 2 0 0 1-3.46 0" />
          </svg>
        );
    }
  };

  // 时间格式化
  const formatTime = (timeStr) => {
    if (!timeStr) return '';
    const date = new Date(timeStr);
    const now = new Date();
    const diff = now - date;
    const minutes = Math.floor(diff / 60000);
    if (minutes < 1) return '刚刚';
    if (minutes < 60) return `${minutes}分钟前`;
    const hours = Math.floor(minutes / 60);
    if (hours < 24) return `${hours}小时前`;
    const days = Math.floor(hours / 24);
    if (days < 7) return `${days}天前`;
    return `${date.getMonth() + 1}月${date.getDate()}日`;
  };

  return (
    <>
      {/* 遮罩层 */}
      {isOpen && <div className="notification-overlay" onClick={onClose} />}

      {/* 通知面板 */}
      <div className={`notification-panel ${isOpen ? 'notification-panel-open' : ''}`}>
        <div className="notification-panel-header">
          <h3>消息通知</h3>
          <div className="notification-panel-actions">
            {notifications.some(n => n.isRead === 0) && (
              <button className="notification-action-link" onClick={handleMarkAllRead}>全部已读</button>
            )}
            {notifications.some(n => n.isRead === 1) && (
              <button className="notification-action-link" onClick={handleClearRead}>清除已读</button>
            )}
            <button className="notification-panel-close" onClick={onClose}>
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="#666" strokeWidth="2">
                <line x1="18" y1="6" x2="6" y2="18" />
                <line x1="6" y1="6" x2="18" y2="18" />
              </svg>
            </button>
          </div>
        </div>

        <div className="notification-panel-body">
          {loading ? (
            <div className="notification-empty">加载中...</div>
          ) : notifications.length === 0 ? (
            <div className="notification-empty">
              <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="#D4C5B0" strokeWidth="1.5">
                <path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9" />
                <path d="M13.73 21a2 2 0 0 1-3.46 0" />
              </svg>
              <p>暂无通知</p>
            </div>
          ) : (
            notifications.map(notification => (
              <div
                key={notification.id}
                className={`notification-item ${notification.isRead === 0 ? 'notification-unread' : ''}`}
              >
                <div className="notification-item-header">
                  <div className="notification-item-icon">
                    {getTypeIcon(notification.notificationType)}
                  </div>
                  <div className="notification-item-info">
                    <span className="notification-item-title">{notification.title}</span>
                    <span className="notification-item-time">{formatTime(notification.createdAt)}</span>
                  </div>
                  {notification.isRead === 0 && <div className="notification-unread-dot" />}
                </div>
                <p className="notification-item-content">{notification.content}</p>

                {/* 绑定通知的操作按钮 */}
                {notification.notificationType === 'bind_request' && notification.isHandled === 0 && (
                  <div className="notification-item-actions">
                    <button
                      className="notification-action-btn notification-action-primary"
                      onClick={() => handleAddContact(notification.id)}
                    >
                      添加为紧急联系人
                    </button>
                    <button
                      className="notification-action-btn notification-action-secondary"
                      onClick={() => handleDismiss(notification.id)}
                    >
                      知道了
                    </button>
                  </div>
                )}

                {/* 已处理的绑定通知提示 */}
                {notification.notificationType === 'bind_request' && notification.isHandled === 1 && (
                  <div className="notification-item-handled">
                    {notification.isRead === 1 ? '已处理' : '已添加为紧急联系人'}
                  </div>
                )}

                {/* 电话变更通知的操作按钮 */}
                {notification.notificationType === 'phone_update' && notification.isHandled === 0 && (
                  <div className="notification-item-actions">
                    <button
                      className="notification-action-btn notification-action-primary"
                      onClick={() => handleUpdatePhone(notification.id)}
                    >
                      更新电话
                    </button>
                    <button
                      className="notification-action-btn notification-action-secondary"
                      onClick={() => handleDismiss(notification.id)}
                    >
                      知道了
                    </button>
                  </div>
                )}

                {/* 已处理的电话变更通知提示 */}
                {notification.notificationType === 'phone_update' && notification.isHandled === 1 && (
                  <div className="notification-item-handled">已处理</div>
                )}
              </div>
            ))
          )}
        </div>
      </div>
    </>
  );
};

export default ElderNotificationPanel;
