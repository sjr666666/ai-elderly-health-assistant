import React, { useState } from 'react';

function ProfileModal({ onComplete, onClose }) {
  const [realName, setRealName] = useState('');
  const [age, setAge] = useState('');
  const [allergyHistory, setAllergyHistory] = useState('');
  const [chronicDiseases, setChronicDiseases] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);

  const handleSubmit = () => {
    if (!realName) {
      alert('请输入您的称呼');
      return;
    }

    setIsSubmitting(true);
    setTimeout(() => {
      onComplete({
        realName,
        age: age ? parseInt(age) : null,
        allergyHistory: allergyHistory || null,
        chronicDiseases: chronicDiseases || null
      });
      setIsSubmitting(false);
    }, 800);
  };

  const handleSkip = () => {
    onComplete({
      realName: '用户',
      age: null,
      allergyHistory: null,
      chronicDiseases: null
    });
  };

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
          }}>👤</div>
          <h2 style={{ fontSize: '30px', fontWeight: '800', color: '#4A90E2', marginBottom: '8px' }}>完善个人档案</h2>
          <p style={{ fontSize: '18px', color: '#6B6B6B' }}>为了给您提供更准确的用药建议</p>
        </div>

        <div style={{ marginBottom: '28px' }}>
          <label style={{
            fontSize: '20px',
            fontWeight: '600',
            marginBottom: '12px',
            display: 'block',
            color: '#3D3D3D'
          }}>
            <span style={{ color: '#E74C3C' }}>*</span> 您的称呼
          </label>
          <input
            type="text"
            value={realName}
            onChange={(e) => setRealName(e.target.value)}
            placeholder="请输入您的姓名或称呼"
            style={{
              width: '100%',
              padding: '20px 24px',
              fontSize: '20px',
              border: '3px solid #F0EBE3',
              borderRadius: '20px',
              outline: 'none',
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

        <div style={{ marginBottom: '28px' }}>
          <label style={{
            fontSize: '20px',
            fontWeight: '600',
            marginBottom: '12px',
            display: 'block',
            color: '#3D3D3D'
          }}>年龄（选填）</label>
          <input
            type="number"
            value={age}
            onChange={(e) => setAge(e.target.value)}
            placeholder="请输入您的年龄"
            min="0"
            max="120"
            style={{
              width: '100%',
              padding: '20px 24px',
              fontSize: '20px',
              border: '3px solid #F0EBE3',
              borderRadius: '20px',
              outline: 'none',
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

        <div style={{ marginBottom: '28px' }}>
          <label style={{
            fontSize: '20px',
            fontWeight: '600',
            marginBottom: '12px',
            display: 'block',
            color: '#3D3D3D'
          }}>过敏史（选填）</label>
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
          }}>慢性病史（选填）</label>
          <textarea
            value={chronicDiseases}
            onChange={(e) => setChronicDiseases(e.target.value)}
            placeholder="例如：高血压、糖尿病、冠心病"
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
