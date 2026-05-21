import { useEffect, useRef, useState } from 'react';
import { createPortal } from 'react-dom';
import Toast from './Toast';

const ContactModal = ({ isOpen, onClose, contacts }) => {
  const overlayRef = useRef(null);
  const [showToast, setShowToast] = useState(false);

  useEffect(() => {
    const handleEscape = (e) => {
      if (e.key === 'Escape') {
        onClose();
      }
    };

    if (isOpen) {
      document.addEventListener('keydown', handleEscape);
      document.body.style.overflow = 'hidden';
    }

    return () => {
      document.removeEventListener('keydown', handleEscape);
      if (!isOpen) {
        document.body.style.overflow = '';
      }
    };
  }, [isOpen]);

  useEffect(() => {
    if (!isOpen) {
      document.body.style.overflow = '';
    }
  }, [isOpen]);

  const handleOverlayClick = (e) => {
    if (e.target === overlayRef.current) {
      onClose();
    }
  };

  const copyToClipboard = async (text) => {
    try {
      await navigator.clipboard.writeText(text);
      setShowToast(true);
    } catch (err) {
      console.error('复制失败:', err);
    }
  };

  if (!isOpen) return null;

  return (
    <>
      <Toast
        message="复制成功！"
        isVisible={showToast}
        onClose={() => setShowToast(false)}
      />
      {createPortal(
        <div
          ref={overlayRef}
          className="global-modal-overlay"
          onClick={handleOverlayClick}
        >
          <div className="global-modal-content">
            <div className="global-modal-header">
              <h3>📞 选择联系方式</h3>
              <button className="global-modal-close" onClick={onClose}>✕</button>
            </div>
            <div className="global-modal-body">
              <p className="global-modal-tip">请选择要联系的人：</p>
              {contacts.map((contact) => (
                <div key={contact.id} className="global-contact-card">
                  <div className="global-contact-info">
                    <div className="global-contact-name">
                      {contact.name}
                      {contact.isPrimary === 1 && <span className="global-primary-tag">主要</span>}
                    </div>
                    <div className="global-contact-phone"> {contact.phone}</div>
                    {contact.relationship && <div className="global-contact-relation">👨‍👩 {contact.relationship}</div>}
                  </div>
                  <div className="global-contact-actions">
                    <button
                      className="global-copy-btn"
                      onClick={() => copyToClipboard(contact.phone)}
                    >
                       复制电话号码
                    </button>
                  </div>
                </div>
              ))}
            </div>
          </div>
        </div>,
        document.body
      )}
    </>
  );
};

export default ContactModal;