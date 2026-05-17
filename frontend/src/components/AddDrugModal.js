import React, { useState, useEffect, useRef } from 'react';

/**
 * 添加新药弹窗组件
 * 用户选择药品后填写用量、频率、有效期、备注等信息
 */
function AddDrugModal({ onClose, onAdd, userId }) {
  // 药品列表数据（从后端 API 获取）
  const [drugOptions, setDrugOptions] = useState([]);
  const [loading, setLoading] = useState(false);

  // 创建输入框引用
  const drugSelectRef = useRef(null);
  const dosageRef = useRef(null);
  const frequencyRef = useRef(null);
  const startDateRef = useRef(null);
  const endDateRef = useRef(null);
  const expiryDateRef = useRef(null);
  const totalQuantityRef = useRef(null);

  // 表单状态
  const [selectedDrugId, setSelectedDrugId] = useState('');
  const [dosage, setDosage] = useState('');
  const [frequency, setFrequency] = useState('');
  const [startDate, setStartDate] = useState('');
  const [endDate, setEndDate] = useState('');
  const [expiryDate, setExpiryDate] = useState('');
  const [totalQuantity, setTotalQuantity] = useState(''); // 新增：总数量
  const [note, setNote] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);

  // 弹窗打开时自动加载药品数据
  useEffect(() => {
    const fetchDrugList = async () => {
      try {
        setLoading(true);
        const response = await fetch('/api/v1/drug/list');
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
    
    // 设置默认有效期为一年后
    const oneYearLater = new Date();
    oneYearLater.setFullYear(oneYearLater.getFullYear() + 1);
    setExpiryDate(oneYearLater.toISOString().split('T')[0]);
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

    if (!dosage.trim()) {
      dosageRef.current.setCustomValidity('请填写每次用量');
      dosageRef.current.reportValidity();
      dosageRef.current.focus();
      return;
    } else {
      dosageRef.current.setCustomValidity('');
    }

    if (!frequency.trim()) {
      frequencyRef.current.setCustomValidity('请填写用药频率');
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

    if (!totalQuantity || parseInt(totalQuantity) <= 0) {
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
        dosage: dosage.trim(),
        frequency: frequency.trim(),
        startDate: startDate,
        endDate: endDate,
        expiryDate: expiryDate,
        totalQuantity: parseInt(totalQuantity), // 新增：总数量
        note: note.trim() || null,
        status: 'active'
      };

      console.log('提交药品数据:', drugData);
      console.log('用户 ID:', userId);

      // 调用后端API
      const response = await fetch(`/api/v1/box?userId=${userId}`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json'
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
          totalQuantity: parseInt(totalQuantity) // 新增：总数量
        });
        setIsSubmitting(false);
      } else {
        alert(data.message || '添加失败，请重试');
        setIsSubmitting(false);
      }

    } catch (err) {
      alert('❌ 添加失败，请稍后重试');
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
          onClick={onClose}
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
            <input
              ref={dosageRef}
              type="text"
              value={dosage}
              onChange={(e) => {
                setDosage(e.target.value);
                // 输入内容后清除验证提示
                if (e.target.value.trim()) {
                  dosageRef.current.setCustomValidity('');
                }
              }}
              placeholder="例如：1片、半片、5ml"
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
            <input
              ref={frequencyRef}
              type="text"
              value={frequency}
              onChange={(e) => {
                setFrequency(e.target.value);
                // 输入内容后清除验证提示
                if (e.target.value.trim()) {
                  frequencyRef.current.setCustomValidity('');
                }
              }}
              placeholder="例如：每日两次、每日三次、必要时服用"
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
              🔢 总数量（片/瓶） <span style={{ color: '#E74C3C' }}>*</span>
            </label>
            <input
              ref={totalQuantityRef}
              type="number"
              value={totalQuantity}
              onChange={(e) => {
                setTotalQuantity(e.target.value);
                // 输入内容后清除验证提示
                if (e.target.value && parseInt(e.target.value) > 0) {
                  totalQuantityRef.current.setCustomValidity('');
                }
              }}
              placeholder="例如：30片、60片"
              min="1"
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
