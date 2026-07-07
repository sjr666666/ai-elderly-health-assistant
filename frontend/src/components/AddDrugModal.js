import React, { useState, useEffect, useRef } from 'react';
import { useToast } from './Toast';
import { getToken } from '../utils/elderApi';

/**
 * 添加新药弹窗组件
 * 用户选择药品后填写用量、频率、有效期、备注等信息
 */
function AddDrugModal({ onClose, onAdd, userId }) {
  const { showToast } = useToast();

  // 药品列表数据（从后端 API 获取）
  const [drugOptions, setDrugOptions] = useState([]);
  const [loading, setLoading] = useState(false);

  // 创建输入框引用
  const drugSelectRef = useRef(null);
  const frequencyRef = useRef(null);
  const startDateRef = useRef(null);
  const endDateRef = useRef(null);
  const expiryDateRef = useRef(null);
  const totalQuantityRef = useRef(null);

  // 表单状态
  const [selectedDrugId, setSelectedDrugId] = useState('');
  const [dosageAmount, setDosageAmount] = useState('1'); // 剂量数值
  const [dosageUnit, setDosageUnit] = useState('片'); // 剂量单位
  const [frequency, setFrequency] = useState('');
  const [startDate, setStartDate] = useState('');
  const [endDate, setEndDate] = useState('');
  const [expiryDate, setExpiryDate] = useState('');
  const [totalQuantity, setTotalQuantity] = useState(''); // 新增：总数量
  const [note, setNote] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);

  // 常见剂量选项
  const dosageAmountOptions = [
    '0.25', '0.5', '1', '1.5', '2', '2.5', '3', '4', '5',
    '6', '7', '8', '9', '10', '12', '15', '20', '25', '30',
    '50', '60', '100', '150', '200', '250', '500', '1000'
  ];

  // 剂量单位选项
  const dosageUnitOptions = [
    '片', '粒', '丸', '颗', '胶囊', '瓶', '支', '盒', '袋',
    'ml', 'g', 'mg', 'μg', 'IU', '单位', '喷', '滴', '贴',
    '膏', '栓', '锭', '糖浆', '口服液', '针'
  ];

  // 弹窗打开时自动加载药品数据
  useEffect(() => {
    const fetchDrugList = async () => {
      try {
        setLoading(true);
        const response = await fetch('/api/v1/drug/list', {
          headers: { 'Authorization': `Bearer ${getToken()}` },
        });
        const data = await response.json();
        
        console.log('=== 药品列表响应 ===');
        console.log('状态码:', response.status);
        console.log('响应数据:', data);
        console.log('==================');
        
        if (response.ok && data.code === 200) {
          // 转换后端数据格式为前端需要的格式
          const drugs = data.data.map(drug => ({
            id: drug.id,
            genericName: drug.drugName,
            specification: drug.specification,
            manufacturer: drug.manufacturer,
            displayText: drug.displayText
          }));
          setDrugOptions(drugs);
        } else {
          console.error('获取药品列表失败:', data.message);
        }
      } catch (err) {
        console.error('获取药品列表异常:', err);
      } finally {
        setLoading(false);
      }
    };

    fetchDrugList();
  }, []);

  // 获取选中的药品信息

  const selectedDrug = drugOptions.find(d => d.id === parseInt(selectedDrugId));

  // 设置默认开始时间为今天，结束时间为3个月后
  useEffect(() => {
    const today = new Date();
    setStartDate(today.toISOString().split('T')[0]);
    
    const threeMonthsLater = new Date();
    threeMonthsLater.setMonth(threeMonthsLater.getMonth() + 3);
    setEndDate(threeMonthsLater.toISOString().split('T')[0]);
    
    // 设置默认有效期为开始日期后12个月
    const twelveMonthsLater = new Date();
    twelveMonthsLater.setMonth(twelveMonthsLater.getMonth() + 12);
    setExpiryDate(twelveMonthsLater.toISOString().split('T')[0]);
  }, []);

  // 提交表单
  const handleSubmit = async (e) => {
    e.preventDefault();

    // 表单验证 - 使用浏览器原生验证提示
    if (!selectedDrugId) {
      drugSelectRef.current.setCustomValidity('请选择药品');
      drugSelectRef.current.reportValidity();
      drugSelectRef.current.focus();
      return;
    } else {
      drugSelectRef.current.setCustomValidity('');
    }

    if (!frequency) {
      frequencyRef.current.setCustomValidity('请选择用药频率');
      frequencyRef.current.reportValidity();
      frequencyRef.current.focus();
      return;
    } else {
      frequencyRef.current.setCustomValidity('');
    }

    if (!startDate) {
      startDateRef.current.setCustomValidity('请选择开始服药日期');
      startDateRef.current.reportValidity();
      startDateRef.current.focus();
      return;
    } else {
      startDateRef.current.setCustomValidity('');
    }

    if (!endDate) {
      endDateRef.current.setCustomValidity('请选择结束服药日期');
      endDateRef.current.reportValidity();
      endDateRef.current.focus();
      return;
    } else {
      endDateRef.current.setCustomValidity('');
    }

    if (!expiryDate) {
      expiryDateRef.current.setCustomValidity('请选择有效期');
      expiryDateRef.current.reportValidity();
      expiryDateRef.current.focus();
      return;
    } else {
      expiryDateRef.current.setCustomValidity('');
    }

    if (!totalQuantity || parseFloat(totalQuantity) <= 0) {
      totalQuantityRef.current.setCustomValidity('请填写有效的总数量');
      totalQuantityRef.current.reportValidity();
      totalQuantityRef.current.focus();
      return;
    } else {
      totalQuantityRef.current.setCustomValidity('');
    }

    setIsSubmitting(true);

    try {
      // 构造提交数据
      const drugData = {
        drugId: parseInt(selectedDrugId),
        dosage: `${dosageAmount}${dosageUnit}`, // 组合剂量和单位
        frequency: frequency,
        startDate: startDate,
        endDate: endDate,
        expiryDate: expiryDate,
        totalQuantity: parseFloat(totalQuantity), // 支持一位小数
        note: note.trim() || null,
        status: 'active'
      };

      console.log('提交药品数据:', drugData);
      console.log('用户 ID:', userId);

      // 调用后端API
      const response = await fetch(`/api/v1/box`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${getToken()}`,
        },
        body: JSON.stringify(drugData)
      });

      const data = await response.json();
      
      if (response.ok && data.code === 200) {
        // 成功后通知父组件
        onAdd({
          ...drugData,
          drugName: selectedDrug.genericName,
          spec: selectedDrug.specification,
          manufacturer: selectedDrug.manufacturer,
          totalQuantity: parseFloat(totalQuantity)
        });
        setIsSubmitting(false);
        // 关闭弹窗
        onClose();
      } else {
        // 检查是否是药品过期
        if (data.message && data.message.includes('已过期')) {
          // 通知父组件显示过期弹窗
          onAdd({
            ...drugData,
            drugName: selectedDrug.genericName,
            spec: selectedDrug.specification,
            manufacturer: selectedDrug.manufacturer,
            totalQuantity: parseFloat(totalQuantity),
            expired: true,
            errorMessage: data.message
          });
          setIsSubmitting(false);
          // 关闭弹窗
          onClose();
        } else {
          showToast(data.message || '添加失败，请重试', 'error');
          setIsSubmitting(false);
        }
      }

    } catch (err) {
      console.error('添加药品失败:', err);
      showToast('添加失败，请稍后重试', 'error');
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
      background: 'rgba(0, 0, 0, 0.6)',
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'center',
      zIndex: 1000,
      padding: '20px',
      backdropFilter: 'blur(4px)'
    }}>
      <div style={{
        background: 'white',
        borderRadius: '32px',
        padding: '48px',
        width: '100%',
        maxWidth: '600px',
        maxHeight: '90vh',
        overflowY: 'auto',
        boxShadow: '0 20px 60px rgba(0, 0, 0, 0.3)',
        position: 'relative'
      }}>
        {/* 关闭按钮 */}
        <button
          style={{
            position: 'absolute',
            top: '20px',
            right: '20px',
            width: '48px',
            height: '48px',
            borderRadius: '50%',
            border: 'none',
            background: '#F5F5F5',
            fontSize: '24px',
            cursor: 'pointer',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            transition: 'all 0.3s ease'
          }}
          onClick={onClose}
          onMouseEnter={(e) => e.target.style.background = '#E0E0E0'}
          onMouseLeave={(e) => e.target.style.background = '#F5F5F5'}
        >
          ✕
        </button>

        {/* 标题 */}
        <div style={{ textAlign: 'center', marginBottom: '40px' }}>
          <div style={{ fontSize: '64px', marginBottom: '16px' }}>💊</div>
          <h2 style={{
            fontSize: '32px',
            fontWeight: '800',
            color: '#4A90E2',
            marginBottom: '8px'
          }}>
            添加新药
          </h2>
          <p style={{ fontSize: '18px', color: '#6B6B6B' }}>
            选择药品并填写用药信息
          </p>
        </div>

        {/* 表单 */}
        <form onSubmit={handleSubmit}>
          {/* 药品选择 */}
          <div style={{ marginBottom: '28px' }}>
            <label style={{
              fontSize: '20px',
              fontWeight: '600',
              marginBottom: '12px',
              display: 'block',
              color: '#3D3D3D'
            }}>
              💊 选择药品 <span style={{ color: '#E74C3C' }}>*</span>
            </label>
            <select
              ref={drugSelectRef}
              value={selectedDrugId}
              onChange={(e) => {
                setSelectedDrugId(e.target.value);
                // 选择药品后清除验证提示
                if (e.target.value) {
                  drugSelectRef.current.setCustomValidity('');
                }
              }}
              disabled={loading}
              style={{
                width: '100%',
                padding: '20px 24px',
                fontSize: '20px',
                border: '3px solid #F0EBE3',
                borderRadius: '20px',
                outline: 'none',
                transition: 'all 0.3s ease',
                background: selectedDrugId ? '#FAF7F2' : 'white',
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
              <option value="">{loading ? '⏳ 加载中...' : '-- 请选择药品 --'}</option>
              {drugOptions.map(drug => (
                <option key={drug.id} value={drug.id}>
                  {drug.displayText || `${drug.genericName} (${drug.specification}) - ${drug.manufacturer}`}
                </option>
              ))}
            </select>

            {/* 选中药品信息显示 */}
            {selectedDrug && (
              <div style={{
                marginTop: '16px',
                padding: '20px',
                background: 'linear-gradient(135deg, #E3F2FD 0%, #F1F8E9 100%)',
                borderRadius: '16px',
                border: '2px solid #4A90E2'
              }}>
                <p style={{ fontSize: '18px', color: '#3D3D3D', marginBottom: '8px' }}>
                  <strong>通用名：</strong>{selectedDrug.genericName}
                </p>
                <p style={{ fontSize: '18px', color: '#3D3D3D', marginBottom: '8px' }}>
                  <strong>商品名：</strong>{selectedDrug.tradeName}
                </p>
                <p style={{ fontSize: '18px', color: '#3D3D3D', marginBottom: '8px' }}>
                  <strong>规格：</strong>{selectedDrug.specification}
                </p>
                <p style={{ fontSize: '18px', color: '#3D3D3D' }}>
                  <strong>厂家：</strong>{selectedDrug.manufacturer}
                </p>
              </div>
            )}
          </div>

          {/* 每次用量 */}
          <div style={{ marginBottom: '28px' }}>
            <label style={{
              fontSize: '20px',
              fontWeight: '600',
              marginBottom: '12px',
              display: 'block',
              color: '#3D3D3D'
            }}>
              💉 每次用量 <span style={{ color: '#E74C3C' }}>*</span>
            </label>
            <div style={{ display: 'flex', gap: '16px', alignItems: 'center' }}>
              {/* 剂量数值列 - 可输入 */}
              <div style={{ flex: 1, position: 'relative' }}>
                <input
                  type="text"
                  value={dosageAmount}
                  onChange={(e) => {
                    const value = e.target.value;
                    // 只允许输入数字和小数点
                    if (/^[0-9]*\.?[0-9]*$/.test(value) || value === '') {
                      setDosageAmount(value);
                    }
                  }}
                  placeholder="输入或选择剂量"
                  style={{
                    width: '100%',
                    padding: '20px 24px',
                    fontSize: '20px',
                    border: '3px solid #F0EBE3',
                    borderRadius: '20px',
                    outline: 'none',
                    background: '#FAF7F2',
                    fontFamily: 'inherit',
                    appearance: 'none'
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
                  list="dosage-amount-suggestions"
                />
                <datalist id="dosage-amount-suggestions">
                  {dosageAmountOptions.map(amount => (
                    <option key={amount} value={amount} />
                  ))}
                </datalist>
              </div>

              {/* 连接符号 */}
              <span style={{ fontSize: '24px', color: '#6B6B6B', fontWeight: 'bold' }}>×</span>

              {/* 剂量单位列 */}
              <div style={{ flex: 1, position: 'relative' }}>
                <select
                  value={dosageUnit}
                  onChange={(e) => {
                    setDosageUnit(e.target.value);
                  }}
                  style={{
                    width: '100%',
                    padding: '20px 24px',
                    fontSize: '20px',
                    border: '3px solid #F0EBE3',
                    borderRadius: '20px',
                    outline: 'none',
                    background: '#FAF7F2',
                    fontFamily: 'inherit',
                    cursor: 'pointer',
                    appearance: 'none',
                    backgroundImage: `url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='24' height='24' viewBox='0 0 24 24'%3E%3Cpath fill='%236B6B6B' d='M7 10l5 5 5-5z'/%3E%3C/svg%3E")`,
                    backgroundRepeat: 'no-repeat',
                    backgroundPosition: 'right 16px center'
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
                  {dosageUnitOptions.map(unit => (
                    <option key={unit} value={unit}>{unit}</option>
                  ))}
                </select>
              </div>
            </div>

            {/* 预览 */}
            <div style={{
              marginTop: '12px',
              padding: '12px 16px',
              background: 'linear-gradient(135deg, #E3F2FD 0%, #F1F8E9 100%)',
              borderRadius: '12px',
              fontSize: '18px',
              color: '#4A90E2',
              fontWeight: '600',
              textAlign: 'center'
            }}>
              每次用量：{dosageAmount} {dosageUnit}
            </div>
          </div>

          {/* 用药频率 */}
          <div style={{ marginBottom: '28px' }}>
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
              ref={frequencyRef}
              value={frequency}
              onChange={(e) => {
                setFrequency(e.target.value);
                if (e.target.value) {
                  frequencyRef.current.setCustomValidity('');
                }
              }}
              style={{
                width: '100%',
                padding: '20px 24px',
                fontSize: '20px',
                border: '3px solid #F0EBE3',
                borderRadius: '20px',
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

          {/* 开始服药日期 */}
          <div style={{ marginBottom: '28px' }}>
            <label style={{
              fontSize: '20px',
              fontWeight: '600',
              marginBottom: '12px',
              display: 'block',
              color: '#3D3D3D'
            }}>
               开始服药日期 <span style={{ color: '#E74C3C' }}>*</span>
            </label>
            <input
              ref={startDateRef}
              type="date"
              value={startDate}
              onChange={(e) => {
                setStartDate(e.target.value);
                // 选择日期后清除验证提示
                if (e.target.value) {
                  startDateRef.current.setCustomValidity('');
                }
              }}
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

          {/* 结束服药日期 */}
          <div style={{ marginBottom: '28px' }}>
            <label style={{
              fontSize: '20px',
              fontWeight: '600',
              marginBottom: '12px',
              display: 'block',
              color: '#3D3D3D'
            }}>
              📅 结束服药日期 <span style={{ color: '#E74C3C' }}>*</span>
            </label>
            <input
              ref={endDateRef}
              type="date"
              value={endDate}
              onChange={(e) => {
                setEndDate(e.target.value);
                // 选择日期后清除验证提示
                if (e.target.value) {
                  endDateRef.current.setCustomValidity('');
                }
              }}
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

          {/* 有效期 */}
          <div style={{ marginBottom: '28px' }}>
            <label style={{
              fontSize: '20px',
              fontWeight: '600',
              marginBottom: '12px',
              display: 'block',
              color: '#3D3D3D'
            }}>
              📅 有效期 <span style={{ color: '#E74C3C' }}>*</span>
            </label>
            <input
              ref={expiryDateRef}
              type="date"
              value={expiryDate}
              onChange={(e) => {
                setExpiryDate(e.target.value);
                // 选择日期后清除验证提示
                if (e.target.value) {
                  expiryDateRef.current.setCustomValidity('');
                }
              }}
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

          {/* 总数量 */}
          <div style={{ marginBottom: '28px' }}>
            <label style={{
              fontSize: '20px',
              fontWeight: '600',
              marginBottom: '12px',
              display: 'block',
              color: '#3D3D3D'
            }}>
              🔢 总数量 <span style={{ color: '#E74C3C' }}>*</span>
            </label>
            <input
              ref={totalQuantityRef}
              type="text"
              value={totalQuantity}
              onChange={(e) => {
                const value = e.target.value;
                // 允许数字和最多一位小数
                if (/^\d*\.?\d{0,1}$/.test(value)) {
                  setTotalQuantity(value);
                  // 输入内容后清除验证提示
                  if (value && parseFloat(value) > 0) {
                    totalQuantityRef.current.setCustomValidity('');
                  }
                }
              }}
              placeholder="例如：30、60.5、100.0"
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
            <p style={{
              fontSize: '14px',
              color: '#6B6B6B',
              marginTop: '8px'
            }}>
              支持整数或一位小数，如：30、60.5、100.0
            </p>
          </div>

          {/* 备注 */}
          <div style={{ marginBottom: '36px' }}>
            <label style={{
              fontSize: '20px',
              fontWeight: '600',
              marginBottom: '12px',
              display: 'block',
              color: '#3D3D3D'
            }}>
              📝 备注（选填）
            </label>
            <textarea
              value={note}
              onChange={(e) => setNote(e.target.value)}
              placeholder="例如：饭前服用、需要冷藏等"
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

          {/* 按钮组 */}
          <div style={{
            display: 'flex',
            gap: '20px',
            justifyContent: 'center'
          }}>
            <button
              type="button"
              onClick={onClose}
              disabled={isSubmitting}
              style={{
                flex: 1,
                padding: '24px 40px',
                fontSize: '22px',
                fontWeight: '700',
                border: '3px solid #F0EBE3',
                borderRadius: '20px',
                background: 'white',
                color: '#6B6B6B',
                cursor: isSubmitting ? 'not-allowed' : 'pointer',
                transition: 'all 0.3s ease',
                opacity: isSubmitting ? 0.5 : 1
              }}
              onMouseEnter={(e) => {
                if (!isSubmitting) {
                  e.target.style.borderColor = '#4A90E2';
                  e.target.style.color = '#4A90E2';
                }
              }}
              onMouseLeave={(e) => {
                e.target.style.borderColor = '#F0EBE3';
                e.target.style.color = '#6B6B6B';
              }}
            >
              取消
            </button>
            <button
              type="submit"
              disabled={isSubmitting}
              style={{
                flex: 1,
                padding: '24px 40px',
                fontSize: '22px',
                fontWeight: '700',
                border: 'none',
                borderRadius: '20px',
                background: isSubmitting
                  ? '#B0BEC5'
                  : 'linear-gradient(135deg, #4A90E2 0%, #357ABD 100%)',
                color: 'white',
                cursor: isSubmitting ? 'not-allowed' : 'pointer',
                transition: 'all 0.3s ease',
                boxShadow: isSubmitting ? 'none' : '0 8px 24px rgba(74, 144, 226, 0.3)',
                position: 'relative',
                overflow: 'hidden'
              }}
              onMouseEnter={(e) => {
                if (!isSubmitting) {
                  e.target.style.transform = 'translateY(-2px)';
                  e.target.style.boxShadow = '0 12px 32px rgba(74, 144, 226, 0.4)';
                }
              }}
              onMouseLeave={(e) => {
                e.target.style.transform = 'translateY(0)';
                e.target.style.boxShadow = '0 8px 24px rgba(74, 144, 226, 0.3)';
              }}
            >
              {isSubmitting ? '⏳ 添加中...' : '✅ 确认添加'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}

export default AddDrugModal;
