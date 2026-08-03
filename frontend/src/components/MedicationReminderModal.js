import React, { useEffect } from 'react';

/**
 * 渐进式提醒弹窗
 * 阶段：pre_remind(提前15min/蓝色) → due_now(到时/橙色) → overdue(超时/红色) → notify_family(已通知家属/深红)
 */
const MedicationReminderModal = ({ reminders, onClose, onMarkAsTaken }) => {

  useEffect(() => {
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

  // 根据提醒阶段获取样式配置
  const getStageConfig = (stage) => {
    switch (stage) {
      case 'pre_remind':
        return {
          headerBg: 'linear-gradient(135deg, #42A5F5 0%, #1E88E5 100%)',
          icon: '🔔',
          title: '即将到服药时间',
          subtitle: '以下药物将在15分钟后需要服用',
          cardBg: '#E3F2FD',
          cardBorder: '#42A5F5',
          tagBg: '#E3F2FD',
          tagColor: '#1565C0',
          tagText: '提前提醒',
          footerText: '💡 提前做好准备，按时服药效果更好'
        };
      case 'due_now':
        return {
          headerBg: 'linear-gradient(135deg, #FF9800 0%, #F57C00 100%)',
          icon: '⏰',
          title: '该服药了',
          subtitle: '以下药物已到服药时间，请及时服用',
          cardBg: '#FFF3E0',
          cardBorder: '#FF9800',
          tagBg: '#FFF3E0',
          tagColor: '#E65100',
          tagText: '到时提醒',
          footerText: '💡 点击"已服用"按钮标记为已服用'
        };
      case 'overdue':
        return {
          headerBg: 'linear-gradient(135deg, #EF5350 0%, #C62828 100%)',
          icon: '⚠️',
          title: '服药超时提醒',
          subtitle: '以下药物已超时未服用，请尽快服药！',
          cardBg: '#FFEBEE',
          cardBorder: '#EF5350',
          tagBg: '#FFEBEE',
          tagColor: '#C62828',
          tagText: '超时提醒',
          footerText: '⚠️ 超时未服用将通知您的家属'
        };
      case 'notify_family':
        return {
          headerBg: 'linear-gradient(135deg, #B71C1C 0%, #7f0000 100%)',
          icon: '🚨',
          title: '已通知家属',
          subtitle: '以下药物超时较久，已通知您的家属关注',
          cardBg: '#FFEBEE',
          cardBorder: '#B71C1C',
          tagBg: '#FFCDD2',
          tagColor: '#B71C1C',
          tagText: '已通知家属',
          footerText: '🚨 您的家属已收到漏服通知，请尽快服药'
        };
      default:
        return {
          headerBg: 'linear-gradient(135deg, #FF9800 0%, #F57C00 100%)',
          icon: '⏰',
          title: '用药提醒',
          subtitle: '您有以下用药计划未完成',
          cardBg: '#FFF3E0',
          cardBorder: '#FF9800',
          tagBg: '#FFF3E0',
          tagColor: '#E65100',
          tagText: '提醒',
          footerText: '💡 点击"已服用"按钮标记为已服用'
        };
    }
  };

  // 获取所有提醒中最高级别的阶段
  const getHighestStage = () => {
    const stageOrder = ['pre_remind', 'due_now', 'overdue', 'notify_family'];
    let highestIndex = -1;
    reminders.forEach(r => {
      const idx = stageOrder.indexOf(r.reminderStage);
      if (idx > highestIndex) highestIndex = idx;
    });
    return highestIndex >= 0 ? stageOrder[highestIndex] : 'due_now';
  };

  const stageConfig = getStageConfig(getHighestStage());

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
        {/* 头部 - 根据阶段变色 */}
        <div style={{
          background: stageConfig.headerBg,
          color: 'white',
          padding: '20px',
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center'
        }}>
          <div>
            <h3 style={{ margin: 0, fontSize: '20px', fontWeight: '700' }}>
              {stageConfig.icon} {stageConfig.title}
            </h3>
            <p style={{ margin: '4px 0 0 0', fontSize: '14px', opacity: 0.9 }}>
              {stageConfig.subtitle}
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
            const scheduledTime = reminder.time || reminder.scheduledTime;
            const drugName = reminder.drug || reminder.drugName || reminder.name || '未知药品';
            const dosage = reminder.dosage;
            const overdueMinutes = calculateOverdueMinutes(scheduledTime);
            const overdueHours = (overdueMinutes / 60).toFixed(1);
            const itemStage = reminder.reminderStage || 'due_now';
            const itemConfig = getStageConfig(itemStage);

            return (
              <div
                key={reminder.id || index}
                style={{
                  background: itemConfig.cardBg,
                  border: `2px solid ${itemConfig.cardBorder}`,
                  borderRadius: '12px',
                  padding: '16px',
                  marginBottom: '12px',
                  transition: 'all 0.2s ease',
                  boxShadow: '0 2px 8px rgba(0,0,0,0.1)'
                }}
                onMouseEnter={(e) => {
                  e.currentTarget.style.transform = 'translateY(-2px)';
                  e.currentTarget.style.boxShadow = '0 4px 12px rgba(0,0,0,0.15)';
                }}
                onMouseLeave={(e) => {
                  e.currentTarget.style.transform = 'translateY(0)';
                  e.currentTarget.style.boxShadow = '0 2px 8px rgba(0,0,0,0.1)';
                }}
              >
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
                  <div style={{ flex: 1 }}>
                    <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '8px' }}>
                      <span style={{ fontSize: '16px', fontWeight: '600', color: '#3D3D3D' }}>
                        💊 {drugName}
                      </span>
                      <span style={{
                        background: itemConfig.tagBg,
                        color: itemConfig.tagColor,
                        fontSize: '11px',
                        fontWeight: '600',
                        padding: '2px 8px',
                        borderRadius: '10px',
                        border: `1px solid ${itemConfig.cardBorder}`
                      }}>
                        {itemConfig.tagText}
                      </span>
                    </div>
                    <p style={{
                      fontSize: '14px',
                      color: '#6B6B6B',
                      margin: '0 0 4px 0'
                    }}>
                      应在 {formatTime(scheduledTime)} 服用
                      {reminder.specification && ` - ${reminder.specification}`}
                    </p>
                    {overdueMinutes > 0 && (
                      <p style={{
                        fontSize: '13px',
                        color: itemConfig.tagColor,
                        margin: 0,
                        fontWeight: '600'
                      }}>
                        ⚠️ 已超时 {overdueMinutes >= 60 ? `${overdueHours} 小时` : `${overdueMinutes} 分钟`}
                      </p>
                    )}
                    {dosage && (
                      <p style={{
                        fontSize: '13px',
                        color: '#9E9E9E',
                        margin: '4px 0 0 0'
                      }}>
                        剂量：{dosage}
                      </p>
                    )}
                  </div>
                  <button
                    onClick={(e) => {
                      e.stopPropagation();
                      onMarkAsTaken(reminder);
                    }}
                    style={{
                      background: '#4CAF50',
                      color: 'white',
                      padding: '8px 16px',
                      borderRadius: '20px',
                      fontSize: '14px',
                      fontWeight: '600',
                      marginLeft: '12px',
                      flexShrink: 0,
                      border: 'none',
                      cursor: 'pointer',
                      transition: 'all 0.2s ease',
                      boxShadow: '0 2px 8px rgba(76, 175, 80, 0.3)'
                    }}
                    onMouseEnter={(e) => {
                      e.target.style.background = '#45a049';
                      e.target.style.transform = 'scale(1.05)';
                    }}
                    onMouseLeave={(e) => {
                      e.target.style.background = '#4CAF50';
                      e.target.style.transform = 'scale(1)';
                    }}
                  >
                    ✓ 已服用
                  </button>
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
            {stageConfig.footerText}
          </p>
        </div>
      </div>

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
