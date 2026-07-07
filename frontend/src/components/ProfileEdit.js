import React, { useState } from 'react';
import { useToast } from './Toast';
import { getToken } from '../utils/elderApi';

// 器官功能状态选项
const ORGAN_FUNCTION_OPTIONS = [
  { value: 'normal', label: '正常' },
  { value: 'mild_impairment', label: '轻度不全' },
  { value: 'moderate_impairment', label: '中度不全' },
  { value: 'severe_impairment', label: '重度不全' },
  { value: 'unknown', label: '不详' }
];

// 常见慢性病预设选项
const CHRONIC_DISEASE_OPTIONS = [
  '高血压', '糖尿病', '冠心病', '高血脂', '脑梗死',
  '慢性肾病', '慢性肝病', '哮喘', '慢阻肺', '痛风',
  '骨质疏松', '心律失常', '心力衰竭', '帕金森病', '类风湿关节炎'
];

// 数字转字符串（保留旧值回显）
const numToStr = (v) => (v === null || v === undefined || v === '' ? '' : String(v));

function ProfileEdit({ user, onSave, onClose }) {
  const { showToast } = useToast();

  const [realName, setRealName] = useState(user?.realName || '');
  const [age, setAge] = useState(numToStr(user?.age));
  const [gender, setGender] = useState(user?.gender || '');
  const [height, setHeight] = useState(numToStr(user?.height));
  const [weight, setWeight] = useState(numToStr(user?.weight));
  const [allergyHistory, setAllergyHistory] = useState(user?.allergyHistory || '');
  const [chronicDiseases, setChronicDiseases] = useState(() => {
    const raw = user?.chronicDiseases || '';
    if (!raw.trim()) return [];
    return raw.split(/[、,;，；]/).map(s => s.trim()).filter(Boolean);
  });
  const [kidneyFunction, setKidneyFunction] = useState(user?.kidneyFunction || 'normal');
  const [liverFunction, setLiverFunction] = useState(user?.liverFunction || 'normal');
  const [isPregnant, setIsPregnant] = useState(user?.isPregnant === 1);
  const [isBreastfeeding, setIsBreastfeeding] = useState(user?.isBreastfeeding === 1);
  const [isSmoking, setIsSmoking] = useState(user?.isSmoking === 1);
  const [isDrinking, setIsDrinking] = useState(user?.isDrinking === 1);
  const [isSubmitting, setIsSubmitting] = useState(false);

  // 解析数字字段
  const parseNum = (v) => {
    if (v === '' || v === null || v === undefined) return null;
    const n = Number(v);
    return Number.isFinite(n) ? n : null;
  };

  // BMI（kg / m²）
  const bmi = (() => {
    const h = parseNum(height);
    const w = parseNum(weight);
    if (h === null || w === null) return null;
    const hm = h / 100;
    if (hm <= 0) return null;
    return (w / (hm * hm)).toFixed(1);
  })();

  const bmiAdvice = (() => {
    if (bmi === null) return null;
    const v = Number(bmi);
    if (v < 18.5) return { label: '偏瘦', color: '#4A90E2' };
    if (v < 24) return { label: '正常', color: '#27AE60' };
    if (v < 28) return { label: '超重', color: '#F39C12' };
    return { label: '肥胖', color: '#E74C3C' };
  })();

  // 格式校验
  const validateForm = () => {
    if (!realName || !realName.trim()) {
      showToast('请输入您的称呼', 'warning');
      return false;
    }
    if (realName.trim().length > 50) {
      showToast('称呼不能超过50个字符', 'warning');
      return false;
    }
    if (age !== '') {
      const ageNum = parseInt(age, 10);
      if (isNaN(ageNum) || ageNum < 0 || ageNum > 120) {
        showToast('年龄必须是0-120之间的数字', 'warning');
        return false;
      }
    }
    if (height !== '') {
      const h = Number(height);
      if (isNaN(h) || h < 30 || h > 250) {
        showToast('请输入合理的身高（30-250 cm）', 'warning');
        return false;
      }
    }
    if (weight !== '') {
      const w = Number(weight);
      if (isNaN(w) || w < 2 || w > 300) {
        showToast('请输入合理的体重（2-300 kg）', 'warning');
        return false;
      }
    }
    return true;
  };

  const handleSubmit = async () => {
    if (!validateForm()) {
      return;
    }
    if (!user) {
      showToast('用户信息缺失，请重新登录', 'error');
      return;
    }

    setIsSubmitting(true);
    try {
      const payload = {
        realName: realName.trim() || null,
        age: age !== '' ? parseInt(age, 10) : null,
        gender: gender || null,
        height: parseNum(height),
        weight: parseNum(weight),
        allergyHistory: allergyHistory.trim() || null,
        chronicDiseases: chronicDiseases.length > 0 ? chronicDiseases.join('、') : '',
        kidneyFunction: user?.kidneyFunction || null,
        liverFunction: user?.liverFunction || null,
        isPregnant: isPregnant ? 1 : 0,
        isBreastfeeding: isBreastfeeding ? 1 : 0,
        isSmoking: isSmoking ? 1 : 0,
        isDrinking: isDrinking ? 1 : 0
      };

      const response = await fetch(`/api/v1/user/profile`, {
        method: 'PUT',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${getToken()}`,
        },
        body: JSON.stringify(payload),
      });

      const data = await response.json();
        if (response.ok && data.code === 200) {
          showToast('个人信息已更新！', 'success');
          onSave(payload);
        } else if (data.message && data.message.includes('用户不存在')) {
          // localStorage 中的 userId 已被数据库清理（雪花 ID 失效）
          showToast('登录已失效，请重新登录', 'error');
          localStorage.removeItem('user');
          setTimeout(() => window.location.reload(), 1200);
        } else {
          showToast(data.message || '更新失败，请重试', 'error');
        }
    } catch (err) {
      console.error('更新个人信息失败:', err);
      showToast('网络连接失败，请稍后重试', 'error');
    } finally {
      setIsSubmitting(false);
    }
  };

  // 通用表单组样式
  const labelStyle = { fontSize: '18px', fontWeight: '600', marginBottom: '10px', display: 'block', color: '#3D3D3D' };
  const inputStyle = {
    width: '100%', padding: '14px 18px', fontSize: '18px',
    border: '2px solid #F0EBE3', borderRadius: '14px',
    outline: 'none', background: '#FAF7F2', fontFamily: 'inherit',
    boxSizing: 'border-box'
  };
  const selectStyle = { ...inputStyle, cursor: 'pointer' };

  // 开关按钮（是/否）
  const Switch = ({ checked, onChange, label }) => (
    <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
      <button
        type="button"
        onClick={() => onChange(!checked)}
        style={{
          width: '56px', height: '32px', borderRadius: '16px',
          background: checked ? '#4A90E2' : '#D9D9D9',
          border: 'none', cursor: 'pointer', position: 'relative',
          transition: 'all 0.25s ease', padding: 0
        }}
      >
        <span style={{
          position: 'absolute', top: '3px', left: checked ? '27px' : '3px',
          width: '26px', height: '26px', borderRadius: '50%', background: 'white',
          transition: 'all 0.25s ease', boxShadow: '0 2px 6px rgba(0,0,0,0.2)'
        }} />
      </button>
      <span style={{ fontSize: '18px', color: checked ? '#4A90E2' : '#6B6B6B', fontWeight: '600' }}>
        {label}{checked ? '：是' : '：否'}
      </span>
    </div>
  );

  // 单选按钮
  const Radio = ({ options, value, onChange }) => (
    <div style={{ display: 'flex', gap: '12px', flexWrap: 'wrap' }}>
      {options.map(opt => (
        <button
          key={opt.value}
          type="button"
          onClick={() => onChange(opt.value)}
          style={{
            padding: '10px 20px', borderRadius: '20px', fontSize: '18px',
            border: value === opt.value ? '2px solid #4A90E2' : '2px solid #F0EBE3',
            background: value === opt.value ? 'rgba(74,144,226,0.1)' : 'white',
            color: value === opt.value ? '#4A90E2' : '#3D3D3D',
            cursor: 'pointer', fontWeight: value === opt.value ? '700' : '500',
            transition: 'all 0.2s ease'
          }}
        >
          {opt.label}
        </button>
      ))}
    </div>
  );

  return (
    <div className="profile-edit-modal">
      <div className="profile-edit-content">
        <div className="profile-edit-header">
          <h2>👤 个人档案管理</h2>
          <button className="profile-edit-close" onClick={onClose}>✕</button>
        </div>

        <div className="profile-edit-body">
          {/* 基础信息 */}
          <div className="form-group">
            <label className="form-label">
              <span className="required">*</span> 您的称呼
            </label>
            <input
              type="text"
              value={realName}
              onChange={(e) => setRealName(e.target.value)}
              placeholder="请输入您的姓名或称呼"
              className="form-input"
              maxLength={50}
            />
          </div>

          <div className="form-group">
            <label className="form-label">年龄（选填）</label>
            <input
              type="number"
              value={age}
              onChange={(e) => setAge(e.target.value)}
              placeholder="请输入您的年龄"
              min="0" max="120"
              className="form-input"
            />
          </div>

          {/* 性别 */}
          <div className="form-group">
            <label className="form-label">性别</label>
            <Radio
              options={[{ value: 'male', label: '♂ 男' }, { value: 'female', label: '♀ 女' }]}
              value={gender}
              onChange={setGender}
            />
          </div>

          {/* 身高体重 */}
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '14px' }}>
            <div className="form-group">
              <label className="form-label">📏 身高 (cm)</label>
              <input
                type="number" value={height}
                onChange={(e) => setHeight(e.target.value)}
                placeholder="例如 165" min="30" max="250" step="0.1"
                className="form-input"
              />
            </div>
            <div className="form-group">
              <label className="form-label">⚖️ 体重 (kg)</label>
              <input
                type="number" value={weight}
                onChange={(e) => setWeight(e.target.value)}
                placeholder="例如 60" min="2" max="300" step="0.1"
                className="form-input"
              />
            </div>
          </div>

          {bmi !== null && bmiAdvice && (
            <div style={{
              background: '#F8F5F0', padding: '12px 16px', borderRadius: '12px',
              marginTop: '-8px', marginBottom: '20px', display: 'flex',
              alignItems: 'center', gap: '10px'
            }}>
              <span style={{ fontSize: '16px', color: '#6B6B6B' }}>BMI 指数</span>
              <span style={{ fontSize: '20px', fontWeight: '700', color: bmiAdvice.color }}>{bmi}</span>
              <span style={{
                fontSize: '14px', color: 'white', background: bmiAdvice.color,
                padding: '2px 10px', borderRadius: '10px', fontWeight: '600'
              }}>{bmiAdvice.label}</span>
            </div>
          )}

          {/* 过敏史 / 慢性病史 */}
          <div className="form-group">
            <label className="form-label">过敏史（选填）</label>
            <textarea
              value={allergyHistory}
              onChange={(e) => setAllergyHistory(e.target.value)}
              placeholder="例如：青霉素过敏、海鲜过敏"
              rows="3" className="form-textarea"
            />
          </div>

          <div className="form-group">
            <label className="form-label">慢性病史（选填，可多选）</label>
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
                      padding: '6px 14px',
                      borderRadius: '20px',
                      fontSize: '14px',
                      border: selected ? '1px solid #4A90E2' : '1px solid #ddd',
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
                    padding: '6px 14px',
                    borderRadius: '20px',
                    fontSize: '14px',
                    border: '1px solid #e8a735',
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
                      cursor: 'pointer', fontSize: '14px', padding: '0 2px', lineHeight: 1
                    }}
                  >×</button>
                </span>
              ))}
            </div>
            {chronicDiseases.length > 0 && (
              <div style={{ marginTop: '8px', fontSize: '13px', color: '#888' }}>
                已选：{chronicDiseases.join('、')}
              </div>
            )}
          </div>

          {/* 关键用药因素分组 */}
          <div style={{
            background: 'linear-gradient(135deg, rgba(74,144,226,0.08), rgba(152,212,187,0.08))',
            padding: '18px', borderRadius: '16px', marginBottom: '20px'
          }}>
            <div style={{
              fontSize: '18px', fontWeight: '700', color: '#4A90E2',
              marginBottom: '14px', display: 'flex', alignItems: 'center', gap: '8px'
            }}>💊 关键用药因素</div>

            <div className="form-group">
              <label className="form-label" style={labelStyle}>🫘 肾功能</label>
              <select
                value={kidneyFunction}
                onChange={(e) => setKidneyFunction(e.target.value)}
                style={selectStyle}
              >
                {ORGAN_FUNCTION_OPTIONS.map(o => (
                  <option key={o.value} value={o.value}>{o.label}</option>
                ))}
              </select>
            </div>

            <div className="form-group">
              <label className="form-label" style={labelStyle}>🫀 肝功能</label>
              <select
                value={liverFunction}
                onChange={(e) => setLiverFunction(e.target.value)}
                style={selectStyle}
              >
                {ORGAN_FUNCTION_OPTIONS.map(o => (
                  <option key={o.value} value={o.value}>{o.label}</option>
                ))}
              </select>
            </div>

            <div className="form-group">
              <label className="form-label" style={labelStyle}>🤰 是否孕期</label>
              <Switch checked={isPregnant} onChange={setIsPregnant} label="孕期" />
            </div>

            <div className="form-group">
              <label className="form-label" style={labelStyle}>🍼 是否哺乳期</label>
              <Switch checked={isBreastfeeding} onChange={setIsBreastfeeding} label="哺乳" />
            </div>

            <div className="form-group">
              <label className="form-label" style={labelStyle}>🚬 是否吸烟</label>
              <Switch checked={isSmoking} onChange={setIsSmoking} label="吸烟" />
            </div>

            <div className="form-group">
              <label className="form-label" style={labelStyle}>🍺 是否饮酒</label>
              <Switch checked={isDrinking} onChange={setIsDrinking} label="饮酒" />
            </div>
          </div>
        </div>

        <div className="profile-edit-footer">
          <button onClick={onClose} className="btn btn-secondary" style={{ flex: 1 }}>取消</button>
          <button
            onClick={handleSubmit}
            disabled={isSubmitting}
            className="btn btn-primary"
            style={{ flex: 2 }}
          >
            {isSubmitting ? '保存中...' : '保存修改'}
          </button>
        </div>
      </div>
    </div>
  );
}

export default ProfileEdit;
