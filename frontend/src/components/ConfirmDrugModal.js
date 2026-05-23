import React, { useState, useEffect } from 'react';

/**
 * 确认药品信息弹窗组件
 * 用于在用药说明页面添加药品时，让用户确认或修改药品信息
 */
function ConfirmDrugModal({ onClose, onConfirm, drugInfo, userId }) {
  // 表单状态
  const [dosageAmount, setDosageAmount] = useState('1');
  const [dosageUnit, setDosageUnit] = useState('片');
  const [frequency, setFrequency] = useState('');
  const [startDate, setStartDate] = useState('');
  const [endDate, setEndDate] = useState('');
  const [expiryDate, setExpiryDate] = useState('');
  const [totalQuantity, setTotalQuantity] = useState('30');
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

  // 频率选项
  const frequencyOptions = [
    '每日一次',
    '每日两次',
    '每日三次',
    '每日四次',
    '隔日一次',
    '每周一次',
    '每周两次',
    '必要时服用',
    '睡前服用',
    '饭前服用',
    '饭后服用'
  ];

  // 初始化表单数据
  useEffect(() => {
    // 设置默认日期
    const today = new Date();
    setStartDate(today.toISOString().split('T')[0]);
    
    const threeMonthsLater = new Date();
    threeMonthsLater.setMonth(threeMonthsLater.getMonth() + 3);
    setEndDate(threeMonthsLater.toISOString().split('T')[0]);
    
    const oneYearLater = new Date();
    oneYearLater.setFullYear(oneYearLater.getFullYear() + 1);
    setExpiryDate(oneYearLater.toISOString().split('T')[0]);

    // 如果传入了药品信息，预填充
    if (drugInfo) {
      // 解析频率信息
      if (drugInfo.frequency) {
        setFrequency(drugInfo.frequency);
      } else if (drugInfo.usage) {
        // 尝试从usage中解析频率
        const usageText = drugInfo.usage;
        if (usageText.includes('一日三次') || usageText.includes('每日三次')) {
          setFrequency('每日三次');
        } else if (usageText.includes('一日两次') || usageText.includes('每日两次')) {
          setFrequency('每日两次');
        } else if (usageText.includes('一日一次') || usageText.includes('每日一次')) {
          setFrequency('每日一次');
        } else if (usageText.includes('一日四次') || usageText.includes('每日四次')) {
          setFrequency('每日四次');
        }
      }

      // 解析剂量信息
      if (drugInfo.dosage) {
        // 尝试解析剂量，如"每次1片"、"每次2粒"等
        const dosageMatch = drugInfo.dosage.match(/每次(\d+\.?\d*)(片|粒|丸|颗|胶囊|瓶|支|盒|袋|ml|g|mg)/);
        if (dosageMatch) {
          setDosageAmount(dosageMatch[1]);
          setDosageUnit(dosageMatch[2]);
        }
      }

      // 设置总数量
      if (drugInfo.totalQuantity) {
        setTotalQuantity(drugInfo.totalQuantity.toString());
      }
    }
  }, [drugInfo]);

  // 处理确认
  const handleConfirm = async () => {
    // 验证必填字段
    if (!frequency) {
      alert('请选择服药频率');
      return;
    }
    if (!dosageAmount) {
      alert('请输入每次用量');
      return;
    }
    if (!totalQuantity) {
      alert('请输入药品总数量');
      return;
    }

    setIsSubmitting(true);

    try {
      // 构造药品数据
      const drugData = {
        drugId: drugInfo.drugId,
        dosage: `每次${dosageAmount}${dosageUnit}`,
        frequency: frequency,
        startDate: startDate,
        endDate: endDate,
        expiryDate: expiryDate,
        totalQuantity: parseInt(totalQuantity)
      };

      // 调用确认回调
      await onConfirm(drugData);
      
      // 关闭弹窗
      onClose();
    } catch (error) {
      console.error('添加药品失败:', error);
      alert('添加失败，请稍后重试');
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
      backgroundColor: 'rgba(0, 0, 0, 0.5)',
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'center',
      zIndex: 1000
    }}>
      <div style={{
        backgroundColor: 'white',
        borderRadius: '24px',
        padding: '32px',
        width: '90%',
        maxWidth: '600px',
        maxHeight: '90vh',
        overflowY: 'auto',
        boxShadow: '0 20px 60px rgba(0, 0, 0, 0.3)'
      }}>
        {/* 标题 */}
        <div style={{
          textAlign: 'center',
          marginBottom: '24px'
        }}>
          <h2 style={{
            fontSize: '28px',
            fontWeight: 'bold',
            color: '#2C3E50',
            margin: '0 0 8px 0'
          }}>
            📦 确认添加到药箱
          </h2>
          <p style={{
            fontSize: '18px',
            color: '#7F8C8D',
            margin: 0
          }}>
            请确认或修改以下药品信息
          </p>
        </div>

        {/* 药品名称 */}
        {drugInfo && (
          <div style={{
            backgroundColor: '#F8F9FA',
            padding: '16px',
            borderRadius: '12px',
            marginBottom: '24px'
          }}>
            <div style={{ fontSize: '16px', color: '#7F8C8D', marginBottom: '8px' }}>
              药品名称
            </div>
            <div style={{ fontSize: '24px', fontWeight: 'bold', color: '#2C3E50' }}>
              {drugInfo.name || drugInfo.genericName}
            </div>
            {drugInfo.specification && (
              <div style={{ fontSize: '16px', color: '#7F8C8D', marginTop: '4px' }}>
                规格：{drugInfo.specification}
              </div>
            )}
          </div>
        )}

        {/* 表单内容 */}
        <div style={{ marginBottom: '24px' }}>
          {/* 每次用量 */}
          <label style={{
            display: 'block',
            fontSize: '18px',
            fontWeight: 'bold',
            color: '#2C3E50',
            marginBottom: '12px'
          }}>
            💊 每次用量
          </label>
          <div style={{ display: 'flex', gap: '16px', alignItems: 'center', marginBottom: '24px' }}>
            {/* 剂量数值列 - 可输入 */}
            <div style={{ flex: 1 }}>
              <input
                type="text"
                value={dosageAmount}
                onChange={(e) => {
                  const value = e.target.value;
                  if (/^[0-9]*\.?[0-9]*$/.test(value) || value === '') {
                    setDosageAmount(value);
                  }
                }}
                placeholder="输入或选择剂量"
                style={{
                  width: '100%',
                  padding: '16px 20px',
                  fontSize: '18px',
                  border: '2px solid #E0E0E0',
                  borderRadius: '12px',
                  outline: 'none',
                  background: '#FAFAFA',
                  fontFamily: 'inherit'
                }}
                list="dosage-amount-suggestions"
              />
              <datalist id="dosage-amount-suggestions">
                {dosageAmountOptions.map(amount => (
                  <option key={amount} value={amount} />
                ))}
              </datalist>
            </div>
            
            {/* 剂量单位列 */}
            <div style={{ flex: 1 }}>
              <select
                value={dosageUnit}
                onChange={(e) => setDosageUnit(e.target.value)}
                style={{
                  width: '100%',
                  padding: '16px 20px',
                  fontSize: '18px',
                  border: '2px solid #E0E0E0',
                  borderRadius: '12px',
                  outline: 'none',
                  background: '#FAFAFA',
                  fontFamily: 'inherit',
                  cursor: 'pointer'
                }}
              >
                {dosageUnitOptions.map(unit => (
                  <option key={unit} value={unit}>{unit}</option>
                ))}
              </select>
            </div>
          </div>

          {/* 用量预览 */}
          <div style={{
            textAlign: 'center',
            padding: '12px',
            backgroundColor: '#E8F5E9',
            borderRadius: '8px',
            marginBottom: '24px',
            fontSize: '16px',
            color: '#2E7D32'
          }}>
            每次用量：{dosageAmount || '0'} {dosageUnit}
          </div>

          {/* 服药频率 */}
          <label style={{
            display: 'block',
            fontSize: '18px',
            fontWeight: 'bold',
            color: '#2C3E50',
            marginBottom: '12px'
          }}>
            🕐 服药频率
          </label>
          <select
            value={frequency}
            onChange={(e) => setFrequency(e.target.value)}
            style={{
              width: '100%',
              padding: '16px 20px',
              fontSize: '18px',
              border: '2px solid #E0E0E0',
              borderRadius: '12px',
              outline: 'none',
              background: '#FAFAFA',
              fontFamily: 'inherit',
              cursor: 'pointer',
              marginBottom: '24px'
            }}
          >
            <option value="">请选择服药频率</option>
            {frequencyOptions.map(freq => (
              <option key={freq} value={freq}>{freq}</option>
            ))}
          </select>

          {/* 药品总数量 */}
          <label style={{
            display: 'block',
            fontSize: '18px',
            fontWeight: 'bold',
            color: '#2C3E50',
            marginBottom: '12px'
          }}>
            📦 药品总数量
          </label>
          <input
            type="text"
            value={totalQuantity}
            onChange={(e) => {
              const value = e.target.value;
              if (/^\d*\.?\d{0,1}$/.test(value) || value === '') {
                setTotalQuantity(value);
              }
            }}
            placeholder="请输入药品总数量"
            style={{
              width: '100%',
              padding: '16px 20px',
              fontSize: '18px',
              border: '2px solid #E0E0E0',
              borderRadius: '12px',
              outline: 'none',
              background: '#FAFAFA',
              fontFamily: 'inherit',
              marginBottom: '24px'
            }}
          />

          {/* 用药时间 */}
          <label style={{
            display: 'block',
            fontSize: '18px',
            fontWeight: 'bold',
            color: '#2C3E50',
            marginBottom: '12px'
          }}>
            📅 用药时间
          </label>
          <div style={{ display: 'flex', gap: '16px', marginBottom: '24px' }}>
            <div style={{ flex: 1 }}>
              <label style={{ fontSize: '14px', color: '#7F8C8D', marginBottom: '8px', display: 'block' }}>
                开始日期
              </label>
              <input
                type="date"
                value={startDate}
                onChange={(e) => setStartDate(e.target.value)}
                style={{
                  width: '100%',
                  padding: '12px 16px',
                  fontSize: '16px',
                  border: '2px solid #E0E0E0',
                  borderRadius: '8px',
                  outline: 'none'
                }}
              />
            </div>
            <div style={{ flex: 1 }}>
              <label style={{ fontSize: '14px', color: '#7F8C8D', marginBottom: '8px', display: 'block' }}>
                结束日期
              </label>
              <input
                type="date"
                value={endDate}
                onChange={(e) => setEndDate(e.target.value)}
                style={{
                  width: '100%',
                  padding: '12px 16px',
                  fontSize: '16px',
                  border: '2px solid #E0E0E0',
                  borderRadius: '8px',
                  outline: 'none'
                }}
              />
            </div>
          </div>

          {/* 有效期 */}
          <label style={{
            display: 'block',
            fontSize: '18px',
            fontWeight: 'bold',
            color: '#2C3E50',
            marginBottom: '12px'
          }}>
            ⏰ 药品有效期
          </label>
          <input
            type="date"
            value={expiryDate}
            onChange={(e) => setExpiryDate(e.target.value)}
            style={{
              width: '100%',
              padding: '16px 20px',
              fontSize: '18px',
              border: '2px solid #E0E0E0',
              borderRadius: '12px',
              outline: 'none',
              background: '#FAFAFA',
              fontFamily: 'inherit'
            }}
          />
        </div>

        {/* 按钮组 */}
        <div style={{ display: 'flex', gap: '16px', marginTop: '32px' }}>
          <button
            onClick={onClose}
            disabled={isSubmitting}
            style={{
              flex: 1,
              padding: '16px 24px',
              fontSize: '18px',
              fontWeight: 'bold',
              border: '2px solid #E0E0E0',
              borderRadius: '12px',
              backgroundColor: 'white',
              color: '#7F8C8D',
              cursor: isSubmitting ? 'not-allowed' : 'pointer',
              opacity: isSubmitting ? 0.6 : 1
            }}
          >
            取消
          </button>
          <button
            onClick={handleConfirm}
            disabled={isSubmitting}
            style={{
              flex: 1,
              padding: '16px 24px',
              fontSize: '18px',
              fontWeight: 'bold',
              border: 'none',
              borderRadius: '12px',
              backgroundColor: isSubmitting ? '#BDC3C7' : '#27AE60',
              color: 'white',
              cursor: isSubmitting ? 'not-allowed' : 'pointer'
            }}
          >
            {isSubmitting ? '添加中...' : '确认添加'}
          </button>
        </div>
      </div>
    </div>
  );
}

export default ConfirmDrugModal;
