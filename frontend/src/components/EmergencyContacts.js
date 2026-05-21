import React, { useState } from 'react';

function EmergencyContacts({ contacts, onAdd, onDelete, onClose, onShowToast }) {
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

  const handleAddContact = () => {
    if (!newContact.name || !newContact.phone) {
      alert('请填写姓名和电话');
      return;
    }

    onAdd({
      ...newContact,
      id: Date.now(),
      isPrimary: contacts.length === 0
    });
    setNewContact({ name: '', phone: '', email: '', relationship: '' });
    setShowAddForm(false);
  };

  const handleDeleteContact = (id) => {
    setDeleteId(id);
    setShowConfirmDialog(true);
  };

  const confirmDelete = () => {
    setIsClosingDialog(true);
    setTimeout(() => {
      onDelete(deleteId);
      setShowConfirmDialog(false);
      setIsClosingDialog(false);
      setDeleteId(null);
    }, 200);
    setTimeout(() => {
      onShowToast && onShowToast('删除成功');
    }, 300);
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
                      {contact.name}
                      {contact.isPrimary && <span className="primary-badge">主要联系人</span>}
                    </div>
                    <div className="contact-details">
                      <span className="contact-item">📱 {contact.phone}</span>
                      {contact.email && <span className="contact-item">✉️ {contact.email}</span>}
                      <span className="contact-item">👨‍👩‍ {contact.relationship}</span>
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
                >
                  添加
                </button>
              </div>
            </div>
          ) : (
            <button
              onClick={() => setShowAddForm(true)}
              className="btn btn-add-contact"
            >
              ➕ 添加紧急联系人
            </button>
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
