import React, { useState } from 'react';

function ProfileEdit({ user, onSave, onClose }) {
  const [realName, setRealName] = useState(user?.realName || '');
  const [age, setAge] = useState(user?.age || '');
  const [allergyHistory, setAllergyHistory] = useState(user?.allergyHistory || '');
  const [chronicDiseases, setChronicDiseases] = useState(user?.chronicDiseases || '');
  const [isSubmitting, setIsSubmitting] = useState(false);

  const handleSubmit = async () => {
    if (!realName) {
      alert('请输入您的称呼');
      return;
    }

    if (!user || !user.userId) {
      alert('用户信息缺失，请重新登录');
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
          allergyHistory: allergyHistory || null,
          chronicDiseases: chronicDiseases || null
        }),
      });

      const data = await response.json();

      if (response.ok && data.code === 200) {
        onSave({
          realName,
          age: age ? parseInt(age) : null,
          allergyHistory: allergyHistory || null,
          chronicDiseases: chronicDiseases || null
        });
      } else {
        alert(data.message || '更新失败，请重试');
      }
    } catch (err) {
      alert('网络连接失败，请稍后重试');
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
