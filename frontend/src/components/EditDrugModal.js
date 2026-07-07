import React, { useState } from 'react';
import { useToast } from './Toast';
import { getToken } from '../utils/elderApi';

/**
 * 编辑药品弹窗组件
 */
const EditDrugModal = ({ onClose, onSave, drug, userId }) => {
  const { showToast } = useToast();

  // 初始化表单数据
  const [dosage, setDosage] = useState(drug?.dosage || '');
  const [frequency, setFrequency] = useState(drug?.frequency || '');
  const [startDate, setStartDate] = useState(drug?.startDate || '');
  const [endDate, setEndDate] = useState(drug?.endDate || '');
  const [expiryDate, setExpiryDate] = useState(drug?.expiryDate || '');
  const [totalQuantity, setTotalQuantity] = useState(drug?.totalQuantity || '');
  const [remainingQuantity, setRemainingQuantity] = useState(drug?.remaining || drug?.totalQuantity || '');
  const [note, setNote] = useState(drug?.note || '');
  const [status, ] = useState(drug?.status || 'active');
  const [isSubmitting, setIsSubmitting] = useState(false);

  // 获取今天日期
  const today = new Date().toISOString().split('T')[0];

  const handleSubmit = async (e) => {
    e.preventDefault();

    // 验证必填字段
    if (!dosage.trim()) {
      showToast('请输入每次用量', 'warning');
      return;
    }
    if (!frequency) {
      showToast('请选择用药频率', 'warning');
      return;
    }
    if (!expiryDate) {
      showToast('请选择药品有效期', 'warning');
      return;
    }

    setIsSubmitting(true);

    try {
      // 构造请求体（只包含非空字段）
      const requestBody = {};
      if (dosage.trim()) requestBody.dosage = dosage.trim();
      if (frequency) requestBody.frequency = frequency;
      if (startDate) requestBody.startDate = startDate;
      if (endDate) requestBody.endDate = endDate;
      if (expiryDate) requestBody.expiryDate = expiryDate;
      if (totalQuantity) requestBody.totalQuantity = parseInt(totalQuantity);
      if (remainingQuantity) requestBody.remainingQuantity = parseInt(remainingQuantity);
      if (note.trim()) requestBody.note = note.trim();
      if (status) requestBody.status = status;

      console.log('=== 编辑药品请求 ===');
      console.log('boxItemId:', drug.boxItemId);
      console.log('userId:', userId);
      console.log('请求体:', requestBody);
      console.log('==================');

      const response = await fetch(`/api/v1/box/${drug.boxItemId}`, {
        method: 'PUT',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${getToken()}`,
        },
        body: JSON.stringify(requestBody),
      });

      const data = await response.json();

      console.log('=== 编辑药品响应 ===');
      console.log('状态码:', response.status);
      console.log('响应数据:', data);
      console.log('==================');

      if (response.ok && data.code === 200) {
        onSave({
          ...drug,
          dosage: dosage.trim(),
          frequency: frequency,
          startDate: startDate || null,
          endDate: endDate || null,
          expiryDate: expiryDate,
          totalQuantity: totalQuantity ? parseInt(totalQuantity) : drug.totalQuantity,
          remaining: remainingQuantity ? parseInt(remainingQuantity) : drug.remaining,
          note: note.trim() || null,
          status: status
        });
        onClose();
      } else {
        showToast(data.message || '修改失败，请重试', 'error');
      }
    } catch (err) {
      console.error('修改药品异常:', err);
      showToast('网络连接失败，请稍后重试', 'error');
    } finally {
      setIsSubmitting(false);
    }
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
    }}>
      <div style={{
        background: 'white',
        padding: '40px 48px',
        borderRadius: '32px',
        boxShadow: '0 16px 64px rgba(0, 0, 0, 0.2)',
        width: '90%',
        maxWidth: '560px',
        maxHeight: '85vh',
        overflow: 'auto',
      }}>
        <div style={{
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
          marginBottom: '32px'
        }}>
          <h2 style={{
            fontSize: '28px',
            fontWeight: '800',
            color: '#4A90E2',
            margin: 0
          }}>
            ️ 修改药品
          </h2>
          <button
            onClick={onClose}
            style={{
              fontSize: '32px',
              background: 'none',
              border: 'none',
              cursor: 'pointer',
              color: '#6B6B6B',
              lineHeight: 1,
              padding: '0 8px'
            }}
          >
            ×
          </button>
        </div>

        {/* 药品基本信息（只读） */}
        <div style={{
          marginBottom: '28px',
          padding: '20px',
          background: 'linear-gradient(135deg, #E3F2FD 0%, #F1F8E9 100%)',
          borderRadius: '16px',
          border: '2px solid #4A90E2'
        }}>
          <p style={{ fontSize: '18px', color: '#3D3D3D', marginBottom: '8px' }}>
            <strong>药品名称：</strong>{drug?.name}
          </p>
          <p style={{ fontSize: '18px', color: '#3D3D3D', marginBottom: '8px' }}>
            <strong>药品规格：</strong>{drug?.spec}
          </p>
        </div>

        <form onSubmit={handleSubmit}>
          {/* 每次用量 */}
          <div style={{ marginBottom: '24px' }}>
            <label style={{
              fontSize: '20px',
              fontWeight: '600',
              marginBottom: '12px',
              display: 'block',
              color: '#3D3D3D'
            }}>
               每次用量 <span style={{ color: '#E74C3C' }}>*</span>
            </label>
            <input
              type="text"
              value={dosage}
              onChange={(e) => setDosage(e.target.value)}
              placeholder="例如：1片、5ml、半片"
              required
              style={{
                width: '100%',
                padding: '16px 20px',
                fontSize: '18px',
                border: '3px solid #F0EBE3',
                borderRadius: '16px',
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

          {/* 用药频率 */}
          <div style={{ marginBottom: '24px' }}>
            <label style={{
              fontSize: '20px',
              fontWeight: '600',
              marginBottom: '12px',
              display: 'block',
              color: '#3D3D3D'
            }}>
               用药频率 <span style={{ color: '#E74C3C' }}>*</span>
            </label>
            <select
              value={frequency}
              onChange={(e) => setFrequency(e.target.value)}
              required
              style={{
                width: '100%',
                padding: '16px 20px',
                fontSize: '18px',
                border: '3px solid #F0EBE3',
                borderRadius: '16px',
                outline: 'none',
                transition: 'all 0.3s ease',
                background: frequency ? '#FAF7F2' : 'white',
                fontFamily: 'inherit',
                cursor: 'pointer',
                appearance: 'none',
                backgroundImage: `url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='24' height='24' viewBox='0 0 24 24'%3E%3Cpath fill='%236B6B6B' d='M7 10l5 5 5-5z'/%3E%3C/svg%3E")`,
                backgroundRepeat: 'no-repeat',
                backgroundPosition: 'right 20px center'
              }}
              onFocus={(e) => {
                e.target.style.borderColor = '#4A90E2';
                e.target.style.boxShadow = '0 0 0 6px rgba(74, 144, 226, 0.12)';
              }}
              onBlur={(e) => {
                e.target.style.borderColor = '#F0EBE3';
                e.target.style.boxShadow = 'none';
              }}
            >
              <option value="">-- 请选择用药频率 --</option>
              <option value="每日一次">每日一次</option>
              <option value="每日两次">每日两次</option>
              <option value="每日三次">每日三次</option>
              <option value="每日四次">每日四次</option>
              <option value="隔日一次">隔日一次</option>
              <option value="每周一次">每周一次</option>
              <option value="每周两次">每周两次</option>
              <option value="每月一次">每月一次</option>
              <option value="必要时服用">必要时服用</option>
              <option value="睡前服用">睡前服用</option>
              <option value="饭前服用">饭前服用</option>
              <option value="饭后服用">饭后服用</option>
              <option value="空腹服用">空腹服用</option>
            </select>
          </div>

          {/* 有效期 */}
          <div style={{ marginBottom: '24px' }}>
            <label style={{
              fontSize: '20px',
              fontWeight: '600',
              marginBottom: '12px',
              display: 'block',
              color: '#3D3D3D'
            }}>
               有效期 <span style={{ color: '#E74C3C' }}>*</span>
            </label>
            <input
              type="date"
              value={expiryDate}
              onChange={(e) => setExpiryDate(e.target.value)}
              required
              style={{
                width: '100%',
                padding: '16px 20px',
                fontSize: '18px',
                border: '3px solid #F0EBE3',
                borderRadius: '16px',
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

          {/* 开始日期 */}
          <div style={{ marginBottom: '24px' }}>
            <label style={{
              fontSize: '20px',
              fontWeight: '600',
              marginBottom: '12px',
              display: 'block',
              color: '#3D3D3D'
            }}>
               开始日期（选填）
            </label>
            <input
              type="date"
              value={startDate}
              onChange={(e) => setStartDate(e.target.value)}
              style={{
                width: '100%',
                padding: '16px 20px',
                fontSize: '18px',
                border: '3px solid #F0EBE3',
                borderRadius: '16px',
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

          {/* 结束日期 */}
          <div style={{ marginBottom: '24px' }}>
            <label style={{
              fontSize: '20px',
              fontWeight: '600',
              marginBottom: '12px',
              display: 'block',
              color: '#3D3D3D'
            }}>
               结束日期（选填）
            </label>
            <input
              type="date"
              value={endDate}
              onChange={(e) => setEndDate(e.target.value)}
              min={startDate || today}
              style={{
                width: '100%',
                padding: '16px 20px',
                fontSize: '18px',
                border: '3px solid #F0EBE3',
                borderRadius: '16px',
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

          {/* 总数量 */}
          <div style={{ marginBottom: '24px' }}>
            <label style={{
              fontSize: '20px',
              fontWeight: '600',
              marginBottom: '12px',
              display: 'block',
              color: '#3D3D3D'
            }}>
               总数量
            </label>
            <input
              type="number"
              value={totalQuantity}
              onChange={(e) => setTotalQuantity(e.target.value)}
              placeholder="例如：30"
              min="0"
              style={{
                width: '100%',
                padding: '16px 20px',
                fontSize: '18px',
                border: '3px solid #F0EBE3',
                borderRadius: '16px',
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

          {/* 剩余数量 */}
          <div style={{ marginBottom: '24px' }}>
            <label style={{
              fontSize: '20px',
              fontWeight: '600',
              marginBottom: '12px',
              display: 'block',
              color: '#3D3D3D'
            }}>
               剩余数量
            </label>
            <input
              type="number"
              value={remainingQuantity}
              onChange={(e) => setRemainingQuantity(e.target.value)}
              placeholder="例如：25"
              min="0"
              style={{
                width: '100%',
                padding: '16px 20px',
                fontSize: '18px',
                border: '3px solid #F0EBE3',
                borderRadius: '16px',
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

          {/* 备注 */}
          <div style={{ marginBottom: '28px' }}>
            <label style={{
              fontSize: '20px',
              fontWeight: '600',
              marginBottom: '12px',
              display: 'block',
              color: '#3D3D3D'
            }}>
               备注（选填）
            </label>
            <textarea
              value={note}
              onChange={(e) => setNote(e.target.value)}
              placeholder="例如：饭后服用、睡前服用"
              rows="3"
              style={{
                width: '100%',
                padding: '16px 20px',
                fontSize: '18px',
                border: '3px solid #F0EBE3',
                borderRadius: '16px',
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

          {/* 按钮区域 */}
          <div style={{
            display: 'flex',
            gap: '16px',
          }}>
            <button
              type="button"
              onClick={onClose}
              disabled={isSubmitting}
              style={{
                flex: 1,
                padding: '16px',
                fontSize: '18px',
                fontWeight: '700',
                border: 'none',
                borderRadius: '16px',
                cursor: isSubmitting ? 'not-allowed' : 'pointer',
                background: '#F0EBE3',
                color: '#6B6B6B',
                transition: 'all 0.3s ease',
                opacity: isSubmitting ? 0.6 : 1
              }}
            >
              取消
            </button>
            <button
              type="submit"
              disabled={isSubmitting}
              style={{
                flex: 2,
                padding: '16px',
                fontSize: '18px',
                fontWeight: '700',
                border: 'none',
                borderRadius: '16px',
                cursor: isSubmitting ? 'not-allowed' : 'pointer',
                background: isSubmitting ? '#BDC3C7' : 'linear-gradient(135deg, #4A90E2 0%, #357ABD 100%)',
                color: 'white',
                transition: 'all 0.3s ease',
                opacity: isSubmitting ? 0.6 : 1
              }}
            >
              {isSubmitting ? ' 保存中...' : ' 确认修改'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};

export default EditDrugModal;
