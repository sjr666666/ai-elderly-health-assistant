import React, { useState } from 'react';
import { useToast } from './Toast';

function ProfileEdit({ user, onSave, onClose }) {
  const { showToast } = useToast();

  const [realName, setRealName] = useState(user?.realName || '');
  const [age, setAge] = useState(user?.age || '');
  const [allergyHistory, setAllergyHistory] = useState(user?.allergyHistory || '');
  const [chronicDiseases, setChronicDiseases] = useState(user?.chronicDiseases || '');
  const [isSubmitting, setIsSubmitting] = useState(false);

  // 格式校验
  const validateForm = () => {
    // 称呼校验
    if (!realName || !realName.trim()) {
      showToast('请输入您的称呼', 'warning');
      return false;
    }
    if (realName.trim().length > 50) {
      showToast('称呼不能超过50个字符', 'warning');
      return false;
    }

    // 年龄校验（选填，但如果有值必须有效）
    if (age !== '') {
      const ageNum = parseInt(age, 10);
      if (isNaN(ageNum) || ageNum < 0 || ageNum > 120) {
        showToast('年龄必须是0-120之间的数字', 'warning');
        return false;
      }
    }

    return true;
  };

  const handleSubmit = async () => {
    if (!validateForm()) {
      return;
    }

    if (!user || !user.userId) {
      showToast('用户信息缺失，请重新登录', 'error');
      return;
    }

    setIsSubmitting(true);
    try {
      // 调用更新接口 - 使用 Query 参数
      const response = await fetch(`/api/v1/user/profile?userId=${user.userId}`, {
        method: 'PUT',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          realName: realName.trim() || null,
          age: age ? parseInt(age, 10) : null,
          allergyHistory: allergyHistory.trim() || null,
          chronicDiseases: chronicDiseases.trim() || null
        }),
      });

      const data = await response.json();

      if (response.ok && data.code === 200) {
        showToast('个人信息已更新！', 'success');
        onSave({
          realName: realName.trim(),
          age: age ? parseInt(age, 10) : null,
          allergyHistory: allergyHistory.trim() || null,
          chronicDiseases: chronicDiseases.trim() || null
        });
      } else {
        showToast(data.message || '更新失败，请重试', 'error');
      }
    } catch (err) {
      showToast('网络连接失败，请稍后重试', 'error');
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="profile-edit-modal">
      <div className="profile-edit-content">
        <div className="profile-edit-header">
          <h2>👤 个人档案管理</h2>
          <button className="profile-edit-close" onClick={onClose}>✕</button>
        </div>

        <div className="profile-edit-body">
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
              min="0"
              max="120"
              className="form-input"
            />
          </div>

          <div className="form-group">
            <label className="form-label">过敏史（选填）</label>
            <textarea
              value={allergyHistory}
              onChange={(e) => setAllergyHistory(e.target.value)}
              placeholder="例如：青霉素过敏、海鲜过敏"
              rows="3"
              className="form-textarea"
            />
          </div>

          <div className="form-group">
            <label className="form-label">慢性病史（选填）</label>
            <textarea
              value={chronicDiseases}
              onChange={(e) => setChronicDiseases(e.target.value)}
              placeholder="例如：高血压、糖尿病、冠心病"
              rows="3"
              className="form-textarea"
            />
          </div>
        </div>

        <div className="profile-edit-footer">
          <button
            onClick={onClose}
            className="btn btn-secondary"
            style={{ flex: 1 }}
          >
            取消
          </button>
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
