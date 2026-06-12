import React, { useState, useEffect } from 'react';
import { useToast } from './Toast';

// 常见慢性病预设选项
const CHRONIC_DISEASE_OPTIONS = [
  '高血压', '糖尿病', '冠心病', '高血脂', '脑梗死',
  '慢性肾病', '慢性肝病', '哮喘', '慢阻肺', '痛风',
  '骨质疏松', '心律失常', '心力衰竭', '帕金森病', '类风湿关节炎'
];

function ProfileModal({ onComplete, onClose, userId }) {
  const [allergyHistory, setAllergyHistory] = useState('');
  const [chronicDiseases, setChronicDiseases] = useState([]);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [isLoading, setIsLoading] = useState(true);
  const showToast = useToast();

  // 获取用户当前信息
  useEffect(() => {
    const fetchUserProfile = async () => {
      if (!userId) {
        setIsLoading(false);
        return;
      }
      try {
        const response = await fetch(`/api/v1/user/profile?userId=${userId}`);
        const data = await response.json();
        if (response.ok && data.code === 200) {
          setAllergyHistory(data.data.allergyHistory || '');
          const raw = data.data.chronicDiseases || '';
          if (raw.trim()) {
            setChronicDiseases(raw.split(/[、,;，；]/).map(s => s.trim()).filter(Boolean));
          }
        } else {
          console.error('获取用户信息失败:', data.message);
        }
      } catch (err) {
        console.error('请求失败:', err);
      } finally {
        setIsLoading(false);
      }
    };

    fetchUserProfile();
  }, [userId]);

  const handleSubmit = async () => {
    if (!userId) {
      showToast('用户信息缺失，请重新登录', 'warning');
      return;
    }

    setIsSubmitting(true);
    try {
      const diseasesStr = chronicDiseases.length > 0 ? chronicDiseases.join('、') : null;
      const response = await fetch(`/api/v1/user/profile?userId=${userId}`, {
        method: 'PUT',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          allergyHistory: allergyHistory || null,
          chronicDiseases: diseasesStr
        }),
      });

      const data = await response.json();

      if (response.ok && data.code === 200) {
        onComplete({
          allergyHistory: allergyHistory || null,
          chronicDiseases: diseasesStr
        });
      } else {
        showToast(data.message || '更新失败，请重试', 'error');
      }
    } catch (err) {
      showToast('网络连接失败，请稍后重试', 'error');
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleSkip = () => {
    onComplete({
      allergyHistory: null,
      chronicDiseases: null
    });
  };

  if (isLoading) {
    return (
      <div style={{
        position: 'fixed',
        top: 0,
        left: 0,
        right: 0,
        bottom: 0,
        background: 'rgba(0, 0, 0, 0.5)',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        zIndex: 1000,
      }}>
        <div style={{
          background: 'white',
          padding: '48px',
          borderRadius: '36px',
          boxShadow: '0 20px 80px rgba(0, 0, 0, 0.25)',
          textAlign: 'center'
        }}>
          <p style={{ fontSize: '20px', color: '#6B6B6B' }}>加载中...</p>
        </div>
      </div>
    );
  }

  return (
    <div style={{
      position: 'fixed',
      top: 0,
      left: 0,
      right: 0,
      bottom: 0,
      background: 'rgba(0, 0, 0, 0.5)',
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'center',
      zIndex: 1000,
      animation: 'fadeIn 0.3s ease'
    }}>
      <div style={{
        background: 'white',
        padding: '48px',
        borderRadius: '36px',
        boxShadow: '0 20px 80px rgba(0, 0, 0, 0.25)',
        width: '100%',
        maxWidth: '520px',
        maxHeight: '90vh',
        overflowY: 'auto',
        margin: '20px',
        animation: 'slideUp 0.4s ease'
      }}>
        <div style={{ textAlign: 'center', marginBottom: '36px' }}>
          <div style={{
            width: '100px',
            height: '100px',
            background: 'linear-gradient(135deg, #4A90E2, #98D4BB)',
            borderRadius: '50%',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            margin: '0 auto 20px',
            fontSize: '48px',
            boxShadow: '0 8px 32px rgba(74, 144, 226, 0.3)'
          }}>💊</div>
          <h2 style={{ fontSize: '30px', fontWeight: '800', color: '#4A90E2', marginBottom: '8px' }}>完善健康信息</h2>
          <p style={{ fontSize: '18px', color: '#6B6B6B' }}>为了给您提供更准确的用药建议</p>
        </div>

        <div style={{ marginBottom: '28px' }}>
          <label style={{
            fontSize: '20px',
            fontWeight: '600',
            marginBottom: '12px',
            display: 'block',
            color: '#3D3D3D'
          }}>🚫 过敏史（选填）</label>
          <textarea
            value={allergyHistory}
            onChange={(e) => setAllergyHistory(e.target.value)}
            placeholder="例如：青霉素过敏、海鲜过敏"
            rows="3"
            style={{
              width: '100%',
              padding: '20px 24px',
              fontSize: '20px',
              border: '3px solid #F0EBE3',
              borderRadius: '20px',
              outline: 'none',
              resize: 'none',
              transition: 'all 0.3s ease',
              background: '#FAF7F2',
              fontFamily: 'inherit'
            }}
            onFocus={(e) => {
              e.target.style.borderColor = '#4A90E2';
              e.target.style.boxShadow = '0 0 0 6px rgba(74, 144, 226, 0.12)';
              e.target.style.background = 'white';
            }}
            onBlur={(e) => {
              e.target.style.borderColor = '#F0EBE3';
              e.target.style.boxShadow = 'none';
              e.target.style.background = '#FAF7F2';
            }}
          />
        </div>

        <div style={{ marginBottom: '32px' }}>
          <label style={{
            fontSize: '20px',
            fontWeight: '600',
            marginBottom: '12px',
            display: 'block',
            color: '#3D3D3D'
          }}>💊 慢性病史（选填，可多选）</label>
          <div style={{ display: 'flex', flexWrap: 'wrap', gap: '8px' }}>
            {CHRONIC_DISEASE_OPTIONS.map(disease => {
              const selected = chronicDiseases.includes(disease);
              return (
                <button
                  key={disease}
                  type="button"
                  onClick={() => {
                    setChronicDiseases(prev =>
                      selected ? prev.filter(d => d !== disease) : [...prev, disease]
                    );
                  }}
                  style={{
                    padding: '8px 16px',
                    borderRadius: '20px',
                    fontSize: '16px',
                    border: selected ? '2px solid #4A90E2' : '2px solid #ddd',
                    background: selected ? '#4A90E2' : '#fff',
                    color: selected ? '#fff' : '#555',
                    cursor: 'pointer',
                    transition: 'all 0.2s'
                  }}
                >
                  {disease}
                </button>
              );
            })}
            {chronicDiseases.filter(d => !CHRONIC_DISEASE_OPTIONS.includes(d)).map(disease => (
              <span
                key={disease}
                style={{
                  padding: '8px 16px',
                  borderRadius: '20px',
                  fontSize: '16px',
                  border: '2px solid #e8a735',
                  background: '#fff8e1',
                  color: '#b8860b',
                  display: 'inline-flex',
                  alignItems: 'center',
                  gap: '4px'
                }}
              >
                {disease}
                <button
                  type="button"
                  onClick={() => setChronicDiseases(prev => prev.filter(d => d !== disease))}
                  style={{
                    background: 'none', border: 'none', color: '#b8860b',
                    cursor: 'pointer', fontSize: '16px', padding: '0 2px', lineHeight: 1
                  }}
                >×</button>
              </span>
            ))}
          </div>
          {chronicDiseases.length > 0 && (
            <div style={{ marginTop: '10px', fontSize: '15px', color: '#888' }}>
              已选：{chronicDiseases.join('、')}
            </div>
          )}
        </div>

        <div style={{ display: 'flex', gap: '16px' }}>
          <button
            onClick={handleSkip}
            style={{
              flex: '1',
              padding: '20px',
              fontSize: '20px',
              fontWeight: '600',
              background: 'white',
              color: '#6B6B6B',
              border: '3px solid #F0EBE3',
              borderRadius: '20px',
              cursor: 'pointer',
              transition: 'all 0.3s ease'
            }}
            onMouseEnter={(e) => {
              e.target.style.borderColor = '#4A90E2';
              e.target.style.color = '#4A90E2';
            }}
            onMouseLeave={(e) => {
              e.target.style.borderColor = '#F0EBE3';
              e.target.style.color = '#6B6B6B';
            }}
          >
            稍后填写
          </button>
          <button
            onClick={handleSubmit}
            disabled={isSubmitting}
            style={{
              flex: '2',
              padding: '20px',
              fontSize: '20px',
              fontWeight: '700',
              background: isSubmitting
                ? 'linear-gradient(135deg, #7FB3F5 0%, #98D4BB 100%)'
                : 'linear-gradient(135deg, #4A90E2 0%, #98D4BB 100%)',
              color: 'white',
              border: 'none',
              borderRadius: '20px',
              cursor: isSubmitting ? 'not-allowed' : 'pointer',
              boxShadow: '0 8px 32px rgba(74, 144, 226, 0.3)',
              transition: 'all 0.3s ease',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              gap: '10px'
            }}
            onMouseEnter={(e) => {
              if (!isSubmitting) {
                e.target.style.transform = 'translateY(-3px) scale(1.02)';
                e.target.style.boxShadow = '0 12px 40px rgba(74, 144, 226, 0.4)';
              }
            }}
            onMouseLeave={(e) => {
              e.target.style.transform = 'translateY(0) scale(1)';
              e.target.style.boxShadow = '0 8px 32px rgba(74, 144, 226, 0.3)';
            }}
          >
            {isSubmitting ? '提交中...' : '确认保存'}
          </button>
        </div>

        <p style={{
          textAlign: 'center',
          fontSize: '14px',
          color: '#9B9B9B',
          marginTop: '24px'
        }}>
          您的信息将被严格保密，仅用于提供个性化用药建议
        </p>
      </div>

      <style>{`
        @keyframes fadeIn {
          from { opacity: 0; }
          to { opacity: 1; }
        }
        @keyframes slideUp {
          from {
            opacity: 0;
            transform: translateY(40px);
          }
          to {
            opacity: 1;
            transform: translateY(0);
          }
        }
      `}</style>
    </div>
  );
}

export default ProfileModal;