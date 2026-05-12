import React, { useState } from 'react';

function ProfileEdit({ user, onSave, onClose }) {
  const [realName, setRealName] = useState(user?.realName || '');
  const [age, setAge] = useState(user?.age || '');
  const [allergyHistory, setAllergyHistory] = useState(user?.allergyHistory || '');
  const [chronicDiseases, setChronicDiseases] = useState(user?.chronicDiseases || '');
  const [isSubmitting, setIsSubmitting] = useState(false);

  const handleSubmit = () => {
    if (!realName) {
      alert('请输入您的称呼');
      return;
    }

    setIsSubmitting(true);
    setTimeout(() => {
      onSave({
        realName,
        age: age ? parseInt(age) : null,
        allergyHistory: allergyHistory || null,
        chronicDiseases: chronicDiseases || null
      });
      setIsSubmitting(false);
    }, 800);
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
