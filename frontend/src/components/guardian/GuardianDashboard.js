import React, { useState, useEffect } from 'react';
import './guardian.css';

function GuardianDashboard({ guardianId, onViewElder }) {
  const [dashboard, setDashboard] = useState(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState('');
  const [showBindForm, setShowBindForm] = useState(false);
  const [elderUsername, setElderUsername] = useState('');
  const [relationType, setRelationType] = useState('');
  const [isBinding, setIsBinding] = useState(false);
  const [bindError, setBindError] = useState('');

  useEffect(() => { loadDashboard(); }, [guardianId]);

  const loadDashboard = async () => {
    setIsLoading(true); setError('');
    try {
      const res = await fetch(`/api/v1/guardian/dashboard?guardianId=${guardianId}`);
      const data = await res.json();
      if (data.code === 200) setDashboard(data.data);
      else setError(data.message || '加载失败');
    } catch { setError('网络连接失败'); }
    finally { setIsLoading(false); }
  };

  const handleBind = async () => {
    if (!elderUsername.trim()) { setBindError('请输入老人用户名'); return; }
    if (!relationType) { setBindError('请选择与老人的关系'); return; }
    setIsBinding(true); setBindError('');
    try {
      const res = await fetch('/api/v1/guardian/bind', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ guardianId, elderUsername: elderUsername.trim(), relationType }),
      });
      const data = await res.json();
      if (data.code === 200) { setElderUsername(''); setRelationType(''); setShowBindForm(false); loadDashboard(); }
      else setBindError(data.message || '绑定失败');
    } catch { setBindError('网络连接失败'); }
    finally { setIsBinding(false); }
  };

  const getMedStatus = (elder) => {
    const pending = elder.todayPendingCount || 0;
    const taken = elder.todayTakenCount || 0;
    const missed = elder.todayMissedCount || 0;
    const total = pending + taken + missed;
    if (total === 0) return { text: '暂无计划', cls: '' };
    if (missed > 0) return { text: `漏服${missed}次`, cls: 's-err' };
    if (pending === 0) return { text: '已完成', cls: 's-ok' };
    if (taken > 0) return { text: `已服${taken}/${total}`, cls: 's-warn' };
    return { text: `待服用${pending}次`, cls: 's-err' };
  };

  if (isLoading) return <div className="g-loading"><div className="g-spinner"></div><p>加载中...</p></div>;
  if (error) return <div className="g-error"><p>{error}</p><button className="g-btn g-btn-primary" onClick={loadDashboard}>重新加载</button></div>;

  const elders = dashboard?.elders || [];
  const eventCount = elders.reduce((s, e) => s + (e.emergencyEventCount || 0), 0);
  const drugCount = elders.reduce((s, e) => s + (e.expiringDrugCount || 0), 0);

  return (
    <div>
      <div className="g-stats">
        <div className="g-stat-card">
          <div className="g-stat-num">{dashboard?.elderCount || 0}</div>
          <div className="g-stat-label">关联老人</div>
        </div>
        <div className="g-stat-card">
          <div className="g-stat-num" style={{ color: eventCount > 0 ? 'var(--danger)' : 'var(--primary)' }}>{eventCount}</div>
          <div className="g-stat-label">紧急事件</div>
        </div>
        <div className="g-stat-card">
          <div className="g-stat-num" style={{ color: drugCount > 0 ? 'var(--warn)' : 'var(--primary)' }}>{drugCount}</div>
          <div className="g-stat-label">临期药品</div>
        </div>
      </div>

      <div className="g-action-bar">
        <h2>关联老人</h2>
        <button className="g-bind-btn" onClick={() => setShowBindForm(!showBindForm)}>
          + 绑定
        </button>
      </div>

      {showBindForm && (
        <div className="g-bind-form">
          <h3>绑定老人账号</h3>
          <p className="g-bind-form-desc">输入老人用户名进行绑定</p>
          {bindError && <div className="g-error-tip">{bindError}</div>}
          <div className="g-bind-input">
            <input type="text" value={elderUsername} onChange={(e) => setElderUsername(e.target.value)}
              placeholder="老人用户名" onKeyDown={(e) => e.key === 'Enter' && handleBind()} />
          </div>
          <div className="g-bind-input" style={{ marginTop: '8px' }}>
            <select value={relationType} onChange={(e) => setRelationType(e.target.value)}
              style={{ flex: 1, padding: '10px 12px', border: '1px solid #ddd', borderRadius: '8px', fontSize: '14px', color: relationType ? '#333' : '#999', background: 'white' }}>
              <option value="">请选择与老人的关系</option>
              <option value="儿子">儿子</option>
              <option value="女儿">女儿</option>
              <option value="配偶">配偶</option>
              <option value="兄弟">兄弟</option>
              <option value="姐妹">姐妹</option>
              <option value="护工">护工</option>
              <option value="其他">其他</option>
            </select>
          </div>
          <div style={{ display: 'flex', gap: '8px', marginTop: '12px' }}>
            <button className="g-btn g-btn-primary" onClick={handleBind} disabled={isBinding}>
              {isBinding ? '...' : '绑定'}
            </button>
            <button className="g-btn g-btn-text" onClick={() => { setShowBindForm(false); setBindError(''); setRelationType(''); }}>取消</button>
          </div>
        </div>
      )}

      {elders.length === 0 ? (
        <div className="g-empty">
          <div className="g-empty-icon">
            <svg width="48" height="48" viewBox="0 0 24 24" fill="#CCC"><path d="M16 11c1.66 0 2.99-1.34 2.99-3S17.66 5 16 5c-1.66 0-3 1.34-3 3s1.34 3 3 3zm-8 0c1.66 0 2.99-1.34 2.99-3S9.66 5 8 5C6.34 5 5 6.34 5 8s1.34 3 3 3zm0 2c-2.33 0-7 1.17-7 3.5V19h14v-2.5c0-2.33-4.67-3.5-7-3.5zm8 0c-.29 0-.62.02-.97.05 1.16.84 1.97 1.97 1.97 3.45V19h6v-2.5c0-2.33-4.67-3.5-7-3.5z"/></svg>
          </div>
          <p>暂无关联老人</p>
          <p className="g-empty-desc">点击上方"绑定"按钮添加</p>
        </div>
      ) : (
        <div className="g-elder-list">
          {elders.map((elder) => {
            const med = getMedStatus(elder);
            return (
              <div key={elder.elderId} className="g-elder-card" onClick={() => onViewElder(elder.elderId)}>
                <div className="g-elder-top">
                  <span className="g-elder-avatar">
                    <svg width="32" height="32" viewBox="0 0 24 24" fill={elder.gender === '女' ? '#E88BA8' : '#5B9BD5'}>
                      <path d="M12 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm0 2c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z"/>
                    </svg>
                  </span>
                  <div className="g-elder-info">
                    <h3>{elder.realName}</h3>
                    <p>{elder.age}岁 · {elder.gender}</p>
                  </div>
                  {elder.emergencyEventCount > 0 && (
                    <span className="g-elder-badge">{elder.emergencyEventCount}条紧急</span>
                  )}
                </div>
                <div className="g-elder-rows">
                  <div className="g-elder-row">
                    <span className="g-elder-label">健康状态</span>
                    <span className="g-elder-value">{elder.healthStatus || '正常'}</span>
                  </div>
                  <div className="g-elder-row">
                    <span className="g-elder-label">今日用药</span>
                    <span className={`g-elder-value ${med.cls}`}>{med.text}</span>
                  </div>
                  <div className="g-elder-row">
                    <span className="g-elder-label">临期药品</span>
                    <span className={`g-elder-value ${(elder.expiringDrugCount || 0) > 0 ? 's-warn' : 's-ok'}`}>
                      {elder.expiringDrugCount || 0}种
                    </span>
                  </div>
                  <div className="g-elder-row">
                    <span className="g-elder-label">最后活跃</span>
                    <span className="g-elder-value" style={{color:'#BBB'}}>{elder.lastActiveTime || '暂无'}</span>
                  </div>
                </div>
                <div className="g-elder-footer">查看详情 &rsaquo;</div>
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
}

export default GuardianDashboard;
