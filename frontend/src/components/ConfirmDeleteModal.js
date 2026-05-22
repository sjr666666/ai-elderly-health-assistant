import React from 'react';

/**
 * 确认删除弹窗组件
 */
const ConfirmDeleteModal = ({ drugName, onConfirm, onCancel }) => {
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
      zIndex: 2000,
    }}>
      <div style={{
        background: 'white',
        padding: '40px 48px',
        borderRadius: '32px',
        boxShadow: '0 16px 64px rgba(0, 0, 0, 0.2)',
        width: '90%',
        maxWidth: '500px',
        textAlign: 'center',
      }}>
        {/* 警告图标 */}
        <div style={{
          fontSize: '64px',
          marginBottom: '24px',
        }}>
          ⚠️
        </div>

        {/* 提示文字 */}
        <h3 style={{
          fontSize: '24px',
          fontWeight: '700',
          color: '#3D3D3D',
          marginBottom: '16px',
        }}>
          确认删除药品
        </h3>

        <p style={{
          fontSize: '18px',
          color: '#6B6B6B',
          marginBottom: '12px',
          lineHeight: '1.6',
        }}>
          确定要删除「<strong style={{ color: '#E74C3C' }}>{drugName}</strong>」吗？
        </p>

        <p style={{
          fontSize: '16px',
          color: '#999',
          marginBottom: '36px',
        }}>
          删除后将不再显示该药品
        </p>

        {/* 按钮区域 */}
        <div style={{
          display: 'flex',
          gap: '16px',
        }}>
          <button
            onClick={onCancel}
            style={{
              flex: 1,
              padding: '16px',
              fontSize: '18px',
              fontWeight: '700',
              border: '3px solid #F0EBE3',
              borderRadius: '16px',
              cursor: 'pointer',
              background: 'white',
              color: '#6B6B6B',
              transition: 'all 0.3s ease',
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
            取消
          </button>
          <button
            onClick={onConfirm}
            style={{
              flex: 1,
              padding: '16px',
              fontSize: '18px',
              fontWeight: '700',
              border: 'none',
              borderRadius: '16px',
              cursor: 'pointer',
              background: 'linear-gradient(135deg, #E74C3C 0%, #C0392B 100%)',
              color: 'white',
              transition: 'all 0.3s ease',
              boxShadow: '0 4px 16px rgba(231, 76, 60, 0.3)',
            }}
            onMouseEnter={(e) => {
              e.target.style.transform = 'translateY(-2px)';
              e.target.style.boxShadow = '0 6px 20px rgba(231, 76, 60, 0.4)';
            }}
            onMouseLeave={(e) => {
              e.target.style.transform = 'translateY(0)';
              e.target.style.boxShadow = '0 4px 16px rgba(231, 76, 60, 0.3)';
            }}
          >
            确认删除
          </button>
        </div>
      </div>
    </div>
  );
};

export default ConfirmDeleteModal;
