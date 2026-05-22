import React, { useState } from 'react';

function EmergencyContacts({ contacts, onAdd, onDelete, onClose, onShowToast, userId }) {
  const [showAddForm, setShowAddForm] = useState(false);
  const [newContact, setNewContact] = useState({
    name: '',
    phone: '',
    email: '',
    relationship: ''
  });
  const [showConfirmDialog, setShowConfirmDialog] = useState(false);
  const [isClosingDialog, setIsClosingDialog] = useState(false);
  const [deleteId, setDeleteId] = useState(null);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [isEditMode, setIsEditMode] = useState(false);
  const [selectedPrimaryId, setSelectedPrimaryId] = useState(null);

  const handleAddContact = async () => {
    if (!newContact.name || !newContact.phone) {
      alert('请填写姓名和电话');
      return;
    }

    if (!userId) {
      alert('用户ID不能为空');
      return;
    }

    setIsSubmitting(true);

    try {
      const response = await fetch('/api/emergency/v1/contacts', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          name: newContact.name,
          elderId: userId,  // 直接使用数据库主键ID（Long类型）
          phone: newContact.phone,
          email: newContact.email || '',
          relationship: newContact.relationship || ''
        })
      });

      const result = await response.json();

      if (result.code === 200) {
        // 调用父组件的回调，刷新联系人列表
        onAdd && onAdd();
        setNewContact({ name: '', phone: '', email: '', relationship: '' });
        setShowAddForm(false);
        onShowToast && onShowToast('添加成功');
      } else {
        alert(result.message || '添加失败');
      }
    } catch (error) {
      console.error('添加联系人失败:', error);
      alert('添加失败，请检查网络连接');
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleDeleteContact = (id) => {
    setDeleteId(id);
    setShowConfirmDialog(true);
  };

  const confirmDelete = async () => {
    setIsClosingDialog(true);
    
    try {
      console.log('开始删除联系人，ID:', deleteId);
      const response = await fetch(`/api/emergency/v1/contacts/${deleteId}`, {
        method: 'DELETE'
      });

      console.log('删除响应状态:', response.status);
      const result = await response.json();
      console.log('删除响应数据:', result);

      if (result.code === 200) {
        // 调用父组件的回调，刷新联系人列表
        onDelete(deleteId);
        setShowConfirmDialog(false);
        setIsClosingDialog(false);
        setDeleteId(null);
        setTimeout(() => {
          onShowToast && onShowToast('删除成功');
        }, 300);
      } else {
        console.error('删除失败，响应码:', result.code, '消息:', result.message);
        alert(result.message || '删除失败');
        setShowConfirmDialog(false);
        setIsClosingDialog(false);
        setDeleteId(null);
      }
    } catch (error) {
      console.error('删除联系人失败:', error);
      alert('删除失败，请检查网络连接');
      setShowConfirmDialog(false);
      setIsClosingDialog(false);
      setDeleteId(null);
    }
  };

  // 进入编辑模式
  const handleEnterEditMode = () => {
    // 找出当前的主要联系人
    const primary = contacts.find(c => c.isPrimary === 1);
    setSelectedPrimaryId(primary ? primary.id : null);
    setIsEditMode(true);
  };

  // 取消编辑模式
  const handleCancelEdit = () => {
    setIsEditMode(false);
    setSelectedPrimaryId(null);
  };

  // 保存主要联系人
  const handleSavePrimaryContact = async () => {
    if (!selectedPrimaryId) {
      alert('请选择一个主要联系人');
      return;
    }

    // 找出当前主要联系人
    const currentPrimary = contacts.find(c => c.isPrimary === 1);
    
    // 如果选中的就是当前的主要联系人，不需要更新
    if (currentPrimary && currentPrimary.id === selectedPrimaryId) {
      setIsEditMode(false);
      setSelectedPrimaryId(null);
      onShowToast && onShowToast('未修改主要联系人');
      return;
    }

    setIsSubmitting(true);
    try {
      // 只更新两个联系人：
      // 1. 将新的主要联系人设置为isPrimary=1
      // 2. 将旧的主要联系人设置为isPrimary=0（如果有）
      
      const updates = [];
      
      // 添加新的主要联系人更新
      const newPrimaryContact = contacts.find(c => c.id === selectedPrimaryId);
      if (newPrimaryContact) {
        updates.push({
          ...newPrimaryContact,
          isPrimary: 1
        });
      }
      
      // 添加旧的主要联系人更新（如果存在且不是同一个）
      if (currentPrimary && currentPrimary.id !== selectedPrimaryId) {
        updates.push({
          ...currentPrimary,
          isPrimary: 0
        });
      }
      
      console.log(`需要更新 ${updates.length} 个联系人`);
      
      // 逐个更新
      for (const contact of updates) {
        console.log(`更新联系人 ${contact.name}，isPrimary:`, contact.isPrimary);
        
        const response = await fetch(`http://localhost:8080/api/emergency/emergency-contact`, {
          method: 'PUT',
          headers: {
            'Content-Type': 'application/json',
          },
          body: JSON.stringify({
            id: contact.id,
            elderId: contact.elderId,
            name: contact.name,
            phone: contact.phone,
            email: contact.email || '',
            relationship: contact.relationship || '',
            isPrimary: contact.isPrimary
          })
        });
        
        const result = await response.json();
        console.log(`更新结果:`, result);
        
        if (result.code !== 200) {
          throw new Error(result.message || `更新 ${contact.name} 失败`);
        }
        
        // 等待一小段时间，避免触发限流
        await new Promise(resolve => setTimeout(resolve, 100));
      }
      
      // 所有更新都成功
      console.log('所有更新成功，刷新列表');
      // 刷新联系人列表
      if (onAdd) {
        await onAdd();
      }
      setIsEditMode(false);
      setSelectedPrimaryId(null);
      onShowToast && onShowToast('主要联系人修改成功');
    } catch (error) {
      console.error('修改主要联系人失败:', error);
      alert(error.message || '修改失败，请重试');
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="emergency-contacts-modal">
      <div className="emergency-contacts-content">
        <div className="emergency-contacts-header">
          <h2>📞 紧急联系人管理</h2>
          <button className="emergency-contacts-close" onClick={onClose}>✕</button>
        </div>

        <div className="emergency-contacts-body">
          {contacts.length === 0 ? (
            <div className="empty-state">
              <p className="empty-text">暂无紧急联系人</p>
              <p className="empty-hint">建议至少添加一位紧急联系人，以便在紧急情况下及时联系</p>
            </div>
          ) : (
            <div className="contacts-list">
              {contacts.map((contact, index) => (
                <div key={contact.id} className="contact-card">
                  <div className="contact-info">
                    <div className="contact-name">
                      {isEditMode && (
                        <input
                          type="radio"
                          name="primaryContact"
                          checked={selectedPrimaryId === contact.id}
                          onChange={() => setSelectedPrimaryId(contact.id)}
                          className="radio-primary"
                        />
                      )}
                      {contact.name}
                      {!isEditMode && contact.isPrimary === 1 && <span className="primary-badge">主要联系人</span>}
                    </div>
                    <div className="contact-details">
                      <span className="contact-item"> {contact.phone}</span>
                      {contact.email && <span className="contact-item">✉️ {contact.email}</span>}
                      {contact.relationship && <span className="contact-item">👨‍👩 {contact.relationship}</span>}
                    </div>
                  </div>
                  <button
                    className="btn-delete"
                    onClick={() => handleDeleteContact(contact.id)}
                    title="删除联系人"
                  >
                    🗑️
                  </button>
                </div>
              ))}
            </div>
          )}

          {showAddForm ? (
            <div className="add-contact-form">
              <div className="form-group">
                <label className="form-label">姓名</label>
                <input
                  type="text"
                  value={newContact.name}
                  onChange={(e) => setNewContact({ ...newContact, name: e.target.value })}
                  placeholder="请输入联系人姓名"
                  className="form-input"
                />
              </div>

              <div className="form-group">
                <label className="form-label">电话</label>
                <input
                  type="tel"
                  value={newContact.phone}
                  onChange={(e) => setNewContact({ ...newContact, phone: e.target.value })}
                  placeholder="请输入联系电话"
                  className="form-input"
                />
              </div>

              <div className="form-group">
                <label className="form-label">邮箱</label>
                <input
                  type="email"
                  value={newContact.email}
                  onChange={(e) => setNewContact({ ...newContact, email: e.target.value })}
                  placeholder="请输入邮箱地址（选填）"
                  className="form-input"
                />
              </div>

              <div className="form-group">
                <label className="form-label">关系</label>
                <select
                  value={newContact.relationship}
                  onChange={(e) => setNewContact({ ...newContact, relationship: e.target.value })}
                  className="form-input"
                >
                  <option value="">请选择关系</option>
                  <option value="儿子">儿子</option>
                  <option value="女儿">女儿</option>
                  <option value="配偶">配偶</option>
                  <option value="兄弟">兄弟</option>
                  <option value="姐妹">姐妹</option>
                  <option value="其他">其他</option>
                </select>
              </div>

              <div className="form-actions">
                <button
                  onClick={() => setShowAddForm(false)}
                  className="btn btn-secondary"
                >
                  取消
                </button>
                <button
                  onClick={handleAddContact}
                  className="btn btn-primary"
                  disabled={isSubmitting}
                >
                  {isSubmitting ? '添加中...' : '添加'}
                </button>
              </div>
            </div>
          ) : (
            <div className="button-group">
              <button
                onClick={() => setShowAddForm(true)}
                className="btn btn-add-contact"
              >
                ➕ 添加紧急联系人
              </button>
              {!isEditMode && contacts.length > 0 && (
                <button
                  onClick={handleEnterEditMode}
                  className="btn btn-edit-primary"
                >
                  ✏️ 修改主要联系人
                </button>
              )}
              {isEditMode && (
                <div className="edit-actions">
                  <button
                    onClick={handleCancelEdit}
                    className="btn btn-secondary"
                  >
                    取消
                  </button>
                  <button
                    onClick={handleSavePrimaryContact}
                    className="btn btn-primary"
                    disabled={isSubmitting}
                  >
                    {isSubmitting ? '保存中...' : '保存'}
                  </button>
                </div>
              )}
            </div>
          )}
        </div>
      </div>

      {showConfirmDialog && (
        <div className={`confirm-dialog-overlay ${isClosingDialog ? 'confirm-dialog-overlay-closing' : ''}`}>
          <div className={`confirm-dialog ${isClosingDialog ? 'confirm-dialog-closing' : ''}`}>
            <div className="confirm-icon">⚠️</div>
            <h3 className="confirm-title">确认删除</h3>
            <p className="confirm-message">确定要删除这个联系人吗？此操作无法撤销。</p>
            <div className="confirm-actions">
              <button className="btn btn-cancel" onClick={() => {
                setIsClosingDialog(true);
                setTimeout(() => {
                  setShowConfirmDialog(false);
                  setIsClosingDialog(false);
                }, 200);
              }}>取消</button>
              <button className="btn btn-delete-confirm" onClick={confirmDelete}>删除</button>
            </div>
          </div>
        </div>
      )}

    </div>
  );
}

export default EmergencyContacts;
