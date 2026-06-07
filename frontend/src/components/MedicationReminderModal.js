import React, { useEffect } from 'react';

const MedicationReminderModal = ({ reminders, onClose, onMarkAsTaken }) => {
  useEffect(() => {
    // 阻止背景滚动
    document.body.style.overflow = 'hidden';
    return () => {
      document.body.style.overflow = 'auto';
    };
  }, []);

  const formatTime = (timeStr) => {
    if (!timeStr) return '';
    const [hours, minutes] = timeStr.split(':');
    return `${hours}:${minutes}`;
  };

  const calculateOverdueMinutes = (scheduledTime) => {
    if (!scheduledTime) return 0;
    const now = new Date();
    const [hours, minutes] = scheduledTime.split(':').map(Number);
    const scheduled = new Date();
    scheduled.setHours(hours, minutes, 0, 0);
    const diff = now - scheduled;
    return Math.floor(diff / (1000 * 60));
  };

  return (
    <div style={{
      position: 'fixed',
      top: 0,
      left: 0,
      right: 0,
      bottom: 0,
      background: 'rgba(0, 0, 0, 0.5)',
      zIndex: 9999,
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'center',
      padding: '20px'
    }} onClick={onClose}>
      <div 
        style={{
          background: 'white',
          borderRadius: '16px',
          width: '100%',
          maxWidth: '480px',
          maxHeight: '80vh',
          overflow: 'hidden',
          boxShadow: '0 8px 32px rgba(0, 0, 0, 0.2)',
          animation: 'fadeInUp 0.3s ease'
        }}
        onClick={(e) => e.stopPropagation()}
      >
        {/* 头部 */}
        <div style={{
          background: 'linear-gradient(135deg, #ff9800 0%, #f57c00 100%)',
          color: 'white',
          padding: '20px',
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center'
        }}>
          <div>
            <h3 style={{ margin: 0, fontSize: '20px', fontWeight: '700' }}>
              ⚠️ 用药提醒
            </h3>
            <p style={{ margin: '4px 0 0 0', fontSize: '14px', opacity: 0.9 }}>
              您有以下用药计划未完成
            </p>
          </div>
          <button
            onClick={onClose}
            style={{
              background: 'rgba(255,255,255,0.2)',
              border: 'none',
              color: 'white',
              fontSize: '24px',
              cursor: 'pointer',
              padding: '8px',
              borderRadius: '8px',
              width: '40px',
              height: '40px',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center'
            }}
          >
            ×
          </button>
        </div>

        {/* 提醒列表 */}
        <div style={{
          padding: '16px',
          maxHeight: 'calc(80vh - 140px)',
          overflowY: 'auto'
        }}>
          {reminders && reminders.map((reminder, index) => {
            const overdueMinutes = calculateOverdueMinutes(reminder.scheduledTime);
            return (
              <div
                key={index}
                style={{
                  background: overdueMinutes > 60 ? '#ffebee' : '#fff3e0',
                  border: `2px solid ${overdueMinutes > 60 ? '#ef5350' : '#ff9800'}`,
                  borderRadius: '12px',
                  padding: '16px',
                  marginBottom: '12px',
                  cursor: 'pointer',
                  transition: 'all 0.2s ease',
                  boxShadow: '0 2px 8px rgba(0,0,0,0.1)'
                }}
                onClick={() => onMarkAsTaken(reminder)}
                onMouseEnter={(e) => {
                  e.target.style.transform = 'translateY(-2px)';
                  e.target.style.boxShadow = '0 4px 12px rgba(0,0,0,0.15)';
                }}
                onMouseLeave={(e) => {
                  e.target.style.transform = 'translateY(0)';
                  e.target.style.boxShadow = '0 2px 8px rgba(0,0,0,0.1)';
                }}
              >
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
                  <div style={{ flex: 1 }}>
                    <p style={{
                      fontSize: '16px',
                      fontWeight: '600',
                      color: '#3D3D3D',
                      margin: '0 0 8px 0'
                    }}>
                      💊 {reminder.drugName || reminder.name || '未知药品'}
                    </p>
                    <p style={{
                      fontSize: '14px',
                      color: '#6B6B6B',
                      margin: '0 0 4px 0'
                    }}>
                      ⏰ 应在 {formatTime(reminder.scheduledTime)} 服用
                      {reminder.specification && ` - ${reminder.specification}`}
                    </p>
                    {overdueMinutes > 0 && (
                      <p style={{
                        fontSize: '13px',
                        color: overdueMinutes > 60 ? '#c62828' : '#e65100',
                        margin: 0,
                        fontWeight: '600'
                      }}>
                        ⚠️ 已超时 {overdueMinutes} 分钟
                      </p>
                    )}
                    {reminder.dosage && (
                      <p style={{
                        fontSize: '13px',
                        color: '#9E9E9E',
                        margin: '4px 0 0 0'
                      }}>
                        剂量：{reminder.dosage}
                      </p>
                    )}
                  </div>
                  <div style={{
                    background: '#4CAF50',
                    color: 'white',
                    padding: '8px 16px',
                    borderRadius: '20px',
                    fontSize: '14px',
                    fontWeight: '600',
                    marginLeft: '12px',
                    flexShrink: 0
                  }}>
                    ✓ 已服用
                  </div>
                </div>
              </div>
            );
          })}
        </div>

        {/* 底部说明 */}
        <div style={{
          padding: '16px 20px',
          background: '#f5f5f5',
          borderTop: '1px solid #e0e0e0',
          textAlign: 'center'
        }}>
          <p style={{
            fontSize: '13px',
            color: '#9E9E9E',
            margin: 0
          }}>
            💡 点击卡片标记为已服用
          </p>
        </div>
      </div>

      {/* CSS动画 */}
      <style>{`
        @keyframes fadeInUp {
          0% {
            opacity: 0;
            transform: translateY(20px);
          }
          100% {
            opacity: 1;
            transform: translateY(0);
          }
        }
      `}</style>
    </div>
  );
};

export default MedicationReminderModal;
