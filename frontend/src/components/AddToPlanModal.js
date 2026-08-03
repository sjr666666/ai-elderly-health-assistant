import React, { useState, useEffect, useCallback } from 'react';
import { useToast } from './Toast';

/**
 * 添加到用药日历弹窗组件
 * 允许用户选择要添加到用药日历的时间段
 */
function AddToPlanModal({ drug, onClose, onSubmit }) {
  const { showToast } = useToast();
  const [selectedTimeSlots, setSelectedTimeSlots] = useState([]);

  const timeSlotOptions = [
    { value: 'morning', label: '早上', time: '08:00', icon: '🌅' },
    { value: 'noon', label: '中午', time: '12:00', icon: '☀️' },
    { value: 'evening', label: '晚上', time: '18:00', icon: '🌆' },
    { value: 'before_bed', label: '睡前', time: '21:00', icon: '🌙' }
  ];

  // 根据频率确定应该选择的时间段数量
  const getRequiredSlotCount = () => {
    if (!drug.frequency) return 1;
    
    const freq = drug.frequency.toLowerCase();
    
    if (freq.includes('四次') || freq.includes('4次')) {
      return 4;
    } else if (freq.includes('三次') || freq.includes('3次')) {
      return 3;
    } else if (freq.includes('两次') || freq.includes('2次') || freq.includes('一日二')) {
      return 2;
    } else {
      return 1;
    }
  };

  const requiredSlotCount = getRequiredSlotCount();

  // 切换时间段选择
  const toggleTimeSlot = (value) => {
    setSelectedTimeSlots(prev => {
      if (prev.includes(value)) {
        return prev.filter(slot => slot !== value);
      } else {
        // 最多只能选择 requiredSlotCount 个时段
        if (prev.length >= requiredSlotCount) {
          showToast(`根据您的用药频率，最多只能选择 ${requiredSlotCount} 个时段`, 'warning');
          return prev;
        }
        return [...prev, value];
      }
    });
  };

  // 提交
  const handleSubmit = () => {
    if (selectedTimeSlots.length === 0) {
      showToast('请至少选择一个服药时间段', 'warning');
      return;
    }
    if (selectedTimeSlots.length !== requiredSlotCount) {
      showToast(`根据您的用药频率，请选择恰好 ${requiredSlotCount} 个服药时间段`, 'warning');
      return;
    }
    onSubmit(selectedTimeSlots);
  };

  // 根据频率自动推荐时间段
  const getRecommendedSlots = useCallback(() => {
    if (!drug.frequency) return [];
    
    const freq = drug.frequency.toLowerCase();
    
    if (freq.includes('四次') || freq.includes('4次')) {
      return ['morning', 'noon', 'evening', 'before_bed'];
    } else if (freq.includes('三次') || freq.includes('3次')) {
      return ['morning', 'noon', 'evening'];
    } else if (freq.includes('两次') || freq.includes('2次') || freq.includes('一日二')) {
      return ['morning', 'evening'];
    } else if (freq.includes('一次') || freq.includes('1次') || freq.includes('每日') || freq.includes('qd')) {
      if (freq.includes('睡前') || freq.includes('晚上') || freq.includes('qn')) {
        return ['before_bed'];
      } else if (freq.includes('中午') || freq.includes('下午')) {
        return ['noon'];
      } else {
        return ['morning'];
      }
    }
    
    return [];
  }, [drug.frequency]);

  // 初始化推荐选择
  useEffect(() => {
    const recommendedSlots = getRecommendedSlots();
    if (recommendedSlots.length > 0) {
      setSelectedTimeSlots(recommendedSlots);
    }
  }, [getRecommendedSlots]);

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal-content" onClick={(e) => e.stopPropagation()} style={{ maxWidth: '500px' }}>
        {/* 弹窗头部 */}
        <div className="modal-header">
          <h3 className="modal-title">📅 添加到用药日历</h3>
          <button className="modal-close-btn" onClick={onClose}>✕</button>
        </div>

        {/* 弹窗内容 */}
        <div className="modal-body">
          {/* 药品信息 */}
          <div style={{
            background: 'var(--bg-light)',
            padding: '16px',
            borderRadius: '12px',
            marginBottom: '24px'
          }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
              <span style={{ fontSize: '32px' }}>💊</span>
              <div>
                <h4 style={{ margin: 0, fontSize: '18px', fontWeight: 'bold', color: 'var(--text-dark)' }}>
                  {drug.name}
                </h4>
                <p style={{ margin: '4px 0 0 0', fontSize: '14px', color: 'var(--text-light)' }}>
                  规格：{drug.spec} | 用法：{drug.dosage}
                </p>
              </div>
            </div>
          </div>

          {/* 时间段选择 */}
          <div>
            <label style={{
              display: 'block',
              fontSize: '16px',
              fontWeight: 'bold',
              marginBottom: '12px',
              color: 'var(--text-dark)'
            }}>
              选择服药时间段（请选择 {requiredSlotCount} 个时段）：
            </label>
            
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '12px' }}>
              {timeSlotOptions.map(option => {
                const isSelected = selectedTimeSlots.includes(option.value);
                const isDisabled = !isSelected && selectedTimeSlots.length >= requiredSlotCount;
                
                return (
                  <div
                    key={option.value}
                    onClick={() => !isDisabled && toggleTimeSlot(option.value)}
                    style={{
                      border: `2px solid ${isSelected ? 'var(--tech-blue)' : (isDisabled ? '#f0f0f0' : '#e0e0e0')}`,
                      borderRadius: '12px',
                      padding: '16px',
                      cursor: isDisabled ? 'not-allowed' : 'pointer',
                      transition: 'all 0.2s ease',
                      background: isSelected ? '#e8f4fd' : (isDisabled ? '#fafafa' : 'white'),
                      textAlign: 'center',
                      userSelect: 'none',
                      opacity: isDisabled ? 0.5 : 1,
                      position: 'relative'
                    }}
                  >
                    <div style={{ fontSize: '24px', marginBottom: '4px' }}>{option.icon}</div>
                    <div style={{ 
                      fontSize: '16px', 
                      fontWeight: isSelected ? 'bold' : 'normal',
                      color: isSelected ? 'var(--tech-blue)' : 'var(--text-dark)'
                    }}>
                      {option.label}
                    </div>
                    <div style={{ fontSize: '14px', color: 'var(--text-light)', marginTop: '2px' }}>
                      {option.time}
                    </div>
                    {isSelected && (
                      <div style={{
                        position: 'absolute',
                        top: '6px',
                        right: '6px',
                        width: '20px',
                        height: '20px',
                        background: 'var(--tech-blue)',
                        borderRadius: '50%',
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'center',
                        color: 'white',
                        fontSize: '12px'
                      }}>✓</div>
                    )}
                  </div>
                );
              })}
            </div>
          </div>

          {/* 已选提示 */}
          <div style={{
            marginTop: '16px',
            padding: '12px',
            background: selectedTimeSlots.length === requiredSlotCount ? '#e8f5e9' : '#fff3e0',
            borderRadius: '8px',
            fontSize: '14px',
            color: selectedTimeSlots.length === requiredSlotCount ? '#2e7d32' : '#e65100'
          }}>
            {selectedTimeSlots.length === requiredSlotCount ? (
              <>✓ 已选择 {selectedTimeSlots.length} 个时段：{selectedTimeSlots.map(slot => 
                timeSlotOptions.find(opt => opt.value === slot)?.label
              ).join('、')}</>
            ) : (
              <>⚠ 请选择恰好 {requiredSlotCount} 个时段（已选 {selectedTimeSlots.length}/{requiredSlotCount}）</>
            )}
          </div>
        </div>

        {/* 弹窗底部按钮 */}
        <div className="modal-footer" style={{ display: 'flex', gap: '12px', justifyContent: 'flex-end' }}>
          <button
            className="btn btn-secondary btn-large"
            onClick={onClose}
            style={{ flex: 1 }}
          >
            取消
          </button>
          <button
            className="btn btn-primary btn-large"
            onClick={handleSubmit}
            disabled={selectedTimeSlots.length !== requiredSlotCount}
            style={{ flex: 1 }}
          >
            确认添加 ({selectedTimeSlots.length}/{requiredSlotCount})
          </button>
        </div>
      </div>
    </div>
  );
}

export default AddToPlanModal;
