import React, { useState, useEffect } from 'react';
import './guardian.css';

function GuardianElderDetail({ guardianId, elderId, onBack }) {
  const [elder, setElder] = useState(null);
  const [events, setEvents] = useState([]);
  const [expiringDrugs, setExpiringDrugs] = useState([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState('');
  const [showUnbindConfirm, setShowUnbindConfirm] = useState(false);
  const [isUnbinding, setIsUnbinding] = useState(false);

  useEffect(() => { if (elderId) loadAllData(); }, [elderId, guardianId]);

  const loadAllData = async () => {
    setIsLoading(true); setError('');
    try { await Promise.all([loadElderDetail(), loadEvents(), loadExpiringDrugs()]); }
    catch { setError('加载失败'); }
    finally { setIsLoading(false); }
  };

  const loadElderDetail = async () => {
    try {
      const res = await fetch(`/api/v1/guardian/elders/${elderId}?guardianId=${guardianId}`);
      const data = await res.json();
      if (data.code === 200) setElder(data.data);
    } catch {}
  };

  const loadEvents = async () => {
    try {
      const res = await fetch(`/api/v1/guardian/elders/${elderId}/events?guardianId=${guardianId}&limit=10`);
      const data = await res.json();
      if (data.code === 200) setEvents(data.data || []);
    } catch {}
  };

  const loadExpiringDrugs = async () => {
    try {
      const res = await fetch(`/api/v1/guardian/elders/${elderId}/expiring-drugs?guardianId=${guardianId}`);
      const data = await res.json();
      if (data.code === 200) setExpiringDrugs(data.data || []);
    } catch {}
  };

  const handleResolveEvent = async (eventId) => {
    try {
      const res = await fetch(`/api/v1/guardian/events/${eventId}/resolve?resolvedBy=${guardianId}`, { method: 'PUT' });
      const data = await res.json();
      if (data.code === 200) setEvents(events.map(e => e.eventId === eventId ? { ...e, status: 'resolved' } : e));
    } catch {}
  };

  const handleUnbind = async () => {
    setIsUnbinding(true);
    try {
      const res = await fetch(`/api/v1/guardian/unbind?guardianId=${guardianId}&elderId=${elderId}`, { method: 'DELETE' });
      const data = await res.json();
      if (data.code === 200) onBack();
      else alert(data.message || '解绑失败');
    } catch { alert('网络连接失败'); }
    finally { setIsUnbinding(false); setShowUnbindConfirm(false); }
  };

  const getEventTypeLabel = (type) => ({
    fall: '跌倒', sos: '紧急求助', abnormal: '异常行为',
    medication_missed: '漏服药物', other: '其他',
  }[type] || type);

  if (isLoading) return <div className="g-loading"><div className="g-spinner"></div><p>加载中...</p></div>;
  if (error) return <div className="g-error"><p>{error}</p><button className="g-btn g-btn-primary" onClick={loadAllData}>重新加载</button></div>;

  return (
    <div>
      <button className="g-btn g-btn-back" onClick={onBack}>&larr; 返回</button>

      {elder && (
        <div className="g-detail-card">
          <div className="g-detail-top">
            <span className="g-detail-avatar">
              <svg width="40" height="40" viewBox="0 0 24 24" fill={elder.gender === '女' ? '#E88BA8' : '#5B9BD5'}>
                <path d="M12 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm0 2c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z"/>
              </svg>
            </span>
            <div className="g-detail-info">
              <h2>{elder.realName}</h2>
              <p>{elder.age}岁 · {elder.gender}</p>
              <p>健康状态：{elder.healthStatus || '正常'}</p>
            </div>
            <button className="g-btn g-btn-danger" onClick={() => setShowUnbindConfirm(true)}>解绑</button>
          </div>
        </div>
      )}

      {showUnbindConfirm && (
        <div className="g-confirm-mask">
          <div className="g-confirm-box">
            <h3>确认解绑</h3>
            <p>确定要解除与 <strong>{elder?.realName}</strong> 的监护关系吗？解绑后将无法查看该老人信息。</p>
            <div className="g-confirm-actions">
              <button className="g-btn g-btn-text" onClick={() => setShowUnbindConfirm(false)} disabled={isUnbinding}>取消</button>
              <button className="g-btn g-btn-danger" onClick={handleUnbind} disabled={isUnbinding}>
                {isUnbinding ? '...' : '确认解绑'}
              </button>
            </div>
          </div>
        </div>
      )}

      <div className="g-section">
        <h3 className="g-section-title">紧急事件</h3>
        {events.length === 0 ? (
          <div className="g-empty-sm">暂无紧急事件</div>
        ) : (
          <div className="g-event-list">
            {events.map((event) => (
              <div key={event.eventId} className={`g-event-item ${event.status === 'resolved' ? 'g-event-resolved' : 'g-event-pending'}`}>
                <div className="g-event-top">
                  <span className="g-event-type">{getEventTypeLabel(event.eventType)}</span>
                  <span className={`g-event-status ${event.status === 'resolved' ? 's-ok' : 's-err'}`}>
                    {event.status === 'resolved' ? '已处理' : '待处理'}
                  </span>
                </div>
                <div className="g-event-body">
                  <p>{event.description}</p>
                  <p className="g-event-time">{event.createdAt}</p>
                </div>
                {event.status !== 'resolved' && (
                  <button className="g-btn g-btn-sm g-btn-primary" onClick={() => handleResolveEvent(event.eventId)} style={{marginTop:8}}>
                    标记已处理
                  </button>
                )}
              </div>
            ))}
          </div>
        )}
      </div>

      <div className="g-section">
        <h3 className="g-section-title">临期药品</h3>
        {expiringDrugs.length === 0 ? (
          <div className="g-empty-sm">暂无临期药品</div>
        ) : (
          <div className="g-drug-list">
            {expiringDrugs.map((drug) => (
              <div key={drug.drugId} className="g-drug-item">
                <div className="g-drug-top">
                  <span className="g-drug-name">{drug.drugName}</span>
                  <span className={`g-drug-tag ${drug.status === 'expired' ? 'g-tag-err' : 'g-tag-warn'}`}>
                    {drug.status === 'expired' ? '已过期' : '即将过期'}
                  </span>
                </div>
                <div className="g-drug-body">
                  <p>有效期至：{drug.expiryDate}</p>
                  <p>剩余天数：<strong>{drug.remainingDays}</strong> 天</p>
                  <p>剩余数量：{drug.remainingQuantity}</p>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}

export default GuardianElderDetail;
