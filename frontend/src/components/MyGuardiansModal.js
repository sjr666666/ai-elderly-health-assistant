import React, { useState, useEffect } from 'react';
import { getToken } from '../utils/elderApi';

function MyGuardiansModal({ onClose, userId }) {
  const [loading, setLoading] = useState(true);
  const [guardianList, setGuardianList] = useState([]);
  const [error, setError] = useState('');

  useEffect(() => {
    if (!userId) {
      setError('用户ID不能为空，请重新登录');
      setLoading(false);
      return;
    }

    const loadGuardians = async () => {
      try {
        const response = await fetch(`/api/v1/guardian/by-elder?elderId=${userId}`, {
          headers: { 'Authorization': `Bearer ${getToken()}` },
        });
        const data = await response.json();
        if (response.ok && data.code === 200) {
          setGuardianList(data.data || []);
        } else {
          setError(data.message || '获取家属列表失败');
        }
      } catch (err) {
        console.error('获取家属列表异常:', err);
        setError('网络异常，请稍后重试');
      } finally {
        setLoading(false);
      }
    };

    loadGuardians();
  }, [userId]);

  // 性别转中文
  const getGenderLabel = (gender) => {
    if (gender === 'male') return '男';
    if (gender === 'female') return '女';
    return '';
  };

  // 性别图标
  const getGenderIcon = (gender) => {
    if (gender === 'male') return '👨';
    if (gender === 'female') return '👩';
    return '👤';
  };

  return (
    <div className="family-contacts-modal" onClick={onClose}>
      <div className="family-contacts-content" onClick={(e) => e.stopPropagation()}>
        <div className="family-contacts-header">
          <h2>👨‍👩‍👧 我的家属</h2>
          <button className="family-contacts-close" onClick={onClose}>✕</button>
        </div>

        <div className="family-contacts-body">
          {loading ? (
            <div className="empty-state">
              <p className="empty-text">加载中...</p>
            </div>
          ) : error ? (
            <div className="empty-state">
              <p className="empty-text">⚠️ {error}</p>
            </div>
          ) : guardianList.length === 0 ? (
            <div className="empty-state">
              <p className="empty-text">暂无家属绑定</p>
              <p className="empty-hint">请让家属在家属端搜索您的用户名进行绑定</p>
            </div>
          ) : (
            <div className="guardian-list">
              {guardianList.map((guardian) => (
                <div key={guardian.guardianId} className="guardian-card">
                  <div className="guardian-avatar">
                    <span className="guardian-avatar-icon">
                      {getGenderIcon(guardian.gender)}
                    </span>
                  </div>
                  <div className="guardian-info">
                    <div className="guardian-name-row">
                      <span className="guardian-name">{guardian.realName || '未知'}</span>
                      {guardian.relationType && (
                        <span className="guardian-relation-badge">{guardian.relationType}</span>
                      )}
                    </div>
                    <div className="guardian-details">
                      {guardian.phone && (
                        <span className="guardian-info-item">📞 {guardian.phone}</span>
                      )}
                      {guardian.age && (
                        <span className="guardian-info-item">{guardian.age}岁</span>
                      )}
                      {guardian.gender && (
                        <span className="guardian-info-item">{getGenderLabel(guardian.gender)}</span>
                      )}
                    </div>
                    {guardian.lastActiveTime && (
                      <div className="guardian-active-time">
                        最近活跃：{guardian.lastActiveTime}
                      </div>
                    )}
                  </div>
                </div>
              ))}
            </div>
          )}

          <div className="family-contacts-tip">
            <p className="tip-text">
              💡 家属由家属端主动绑定。如果您需要添加新的家属，请让家属在家属端搜索您的用户名进行绑定。
            </p>
          </div>
        </div>
      </div>
    </div>
  );
}

export default MyGuardiansModal;
