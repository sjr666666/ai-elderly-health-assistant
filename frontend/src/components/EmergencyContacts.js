import React, { useState } from 'react';
import { useToast } from './Toast';

function EmergencyContacts({ contacts, onAdd, onDelete, onClose, userId }) {
  const { showToast } = useToast();
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
  const [editingContact, setEditingContact] = useState(null);

  const closeContactForm = () => {
    setShowAddForm(false);
    setEditingContact(null);
    setNewContact({ name: '', phone: '', email: '', relationship: '' });
  };

  const handleEditContact = (contact) => {
    setEditingContact(contact);
    setNewContact({
      name: contact.name || '',
      phone: contact.phone || '',
      email: contact.email || '',
      relationship: contact.relationship || ''
    });
    setShowAddForm(true);
  };

  const handleSubmitContact = async () => {
    if (!newContact.name || !newContact.phone) {
      showToast('请填写姓名和电话', 'warning');
      return;
    }

    if (!userId) {
      showToast('用户ID不能为空，请重新登录', 'error');
      return;
    }

    setIsSubmitting(true);

    try {
      if (editingContact) {
        // 编辑模式：调用 PUT 更新联系人
        const response = await fetch('/api/emergency/emergency-contact', {
          method: 'PUT',
          headers: {
            'Content-Type': 'application/json',
          },
          body: JSON.stringify({
            id: editingContact.id,
            elderId: editingContact.elderId || userId,
            name: newContact.name,
            phone: newContact.phone,
            email: newContact.email || '',
            relationship: newContact.relationship || '',
            isPrimary: editingContact.isPrimary
          })
        });

        const result = await response.json();
        if (result.code === 200) {
          onAdd && onAdd();
          closeContactForm();
          showToast('修改成功', 'success');
        } else {
          showToast(result.message || '修改失败', 'error');
        }
        return;
      }

      // 新增模式：调用 POST 添加联系人
      const response = await fetch('/api/emergency/v1/contacts', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          name: newContact.name,
          elderId: userId,
          phone: newContact.phone,
          email: newContact.email || '',
          relationship: newContact.relationship || ''
        })
      });

      const result = await response.json();

      if (result.code === 200) {
        onAdd && onAdd();
        closeContactForm();
        showToast('添加成功', 'success');
      } else {
        showToast(result.message || '添加失败', 'error');
      }
    } catch (error) {
      console.error('保存联系人失败:', error);
      showToast(editingContact ? '修改失败，请检查网络连接' : '添加失败，请检查网络连接', 'error');
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
      const response = await fetch(`/api/emergency/v1/contacts/${deleteId}`, {
        method: 'DELETE',
      });
      
      const result = await response.json();
      
      if (result.code === 200) {
        onDelete(deleteId);
        showToast('删除成功', 'success');
      } else {
        console.error('删除联系人失败，响应码:', result.code, '消息:', result.message);
        showToast(result.message || '删除失败', 'error');
      }
      
      setShowConfirmDialog(false);
      setIsClosingDialog(false);
      setDeleteId(null);
    } catch (error) {
      console.error('删除联系人失败:', error);
      showToast('删除失败，请检查网络连接', 'error');
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
      showToast('请选择一个主要联系人', 'warning');
      return;
    }

    // 找出当前主要联系人
    const currentPrimary = contacts.find(c => c.isPrimary === 1);
    
    // 如果选中的就是当前的主要联系人，不需要更新
    if (currentPrimary && currentPrimary.id === selectedPrimaryId) {
      setIsEditMode(false);
      setSelectedPrimaryId(null);
      showToast('未修改主要联系人', 'info');
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
        
        const response = await fetch(`/api/emergency/emergency-contact`, {
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
      showToast('主要联系人修改成功', 'success');
    } catch (error) {
      console.error('修改主要联系人失败:', error);
      showToast(error.message || '修改失败，请重试', 'error');
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
                  {!isEditMode && (
                    <div className="contact-actions">
                      <button
                        className="btn-edit"
                        onClick={() => handleEditContact(contact)}
                        title="编辑联系人"
                      >
                        ✏️
                      </button>
                      <button
                        className="btn-delete"
                        onClick={() => handleDeleteContact(contact.id)}
                        title="删除联系人"
                      >
                        🗑️
                      </button>
                    </div>
                  )}
                  {isEditMode && (
                    <button
                      className="btn-delete"
                      onClick={() => handleDeleteContact(contact.id)}
                      title="删除联系人"
                    >
                      🗑️
                    </button>
                  )}
                </div>
              ))}
            </div>
          )}

          {showAddForm ? (
            <div className="add-contact-form">
              <h3 className="form-title">
                {editingContact ? '✏️ 编辑联系人' : '➕ 添加紧急联系人'}
              </h3>
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
                  onClick={closeContactForm}
                  className="btn btn-secondary"
                >
                  取消
                </button>
                <button
                  onClick={handleSubmitContact}
                  className="btn btn-primary"
                  disabled={isSubmitting}
                >
                  {isSubmitting
                    ? (editingContact ? '保存中...' : '添加中...')
                    : (editingContact ? '保存' : '添加')}
                </button>
              </div>
            </div>
          ) : (
            <div className="button-group">
              <button
                onClick={() => {
                  setEditingContact(null);
                  setNewContact({ name: '', phone: '', email: '', relationship: '' });
                  setShowAddForm(true);
                }}
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
