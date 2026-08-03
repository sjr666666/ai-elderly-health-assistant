import { useEffect, useRef, useState } from 'react';
import { createPortal } from 'react-dom';
import Toast from './Toast';
import ConfirmDialog from './ConfirmDialog';

/**
 * 检测是否为移动端设备
 * 移动端：直接 tel: 协议拨号
 * 桌面端：弹出友好提示，告知用户该功能仅在手机端可用
 */
const isMobileDevice = () => {
  if (typeof navigator === 'undefined') return false;
  return /Android|iPhone|iPad|iPod|Windows Phone|Mobile/i.test(navigator.userAgent)
    || (typeof window !== 'undefined' && 'ontouchstart' in window && window.innerWidth < 1024);
};

const ContactModal = ({ isOpen, onClose, contacts }) => {
  const overlayRef = useRef(null);
  const [showToast, setShowToast] = useState(false);
  const [confirmState, setConfirmState] = useState({ isOpen: false, phone: '', title: '', message: '' });
  const [showDesktopTip, setShowDesktopTip] = useState(false); // 桌面端不支持拨号提示

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
  }, [isOpen, onClose]);

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

  const handleCallPhone = (phone) => {
    // 桌面端：弹出友好提示，告知用户该功能仅在手机端可用
    if (!isMobileDevice()) {
      setShowDesktopTip(true);
      return;
    }
    // 移动端：原有的确认拨号逻辑
    const isEmergency = phone === '120' || phone === '110';
    setConfirmState({
      isOpen: true,
      phone,
      title: isEmergency ? `呼叫${phone}` : '拨打电话',
      message: isEmergency
        ? `确定要拨打${phone}吗？`
        : `确定要拨打 ${phone} 吗？`
    });
  };

  const handleConfirm = () => {
    window.location.href = `tel:${confirmState.phone}`;
    setConfirmState({ ...confirmState, isOpen: false });
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
              {/* 紧急快捷呼叫 */}
              <div className="emergency-quick-call">
                <button
                  className="emergency-quick-btn emergency-120"
                  onClick={() => handleCallPhone('120')}
                >
                  <span className="quick-call-icon">🚑</span>
                  <span className="quick-call-text">呼叫120</span>
                </button>
                <button
                  className="emergency-quick-btn emergency-110"
                  onClick={() => handleCallPhone('110')}
                >
                  <span className="quick-call-icon">🚔</span>
                  <span className="quick-call-text">呼叫110</span>
                </button>
              </div>

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
                      className="global-call-btn"
                      onClick={() => handleCallPhone(contact.phone)}
                    >
                      📞 拨打电话
                    </button>
                    <button
                      className="global-copy-btn"
                      onClick={() => copyToClipboard(contact.phone)}
                    >
                       复制号码
                    </button>
                  </div>
                </div>
              ))}
            </div>
          </div>
        </div>,
        document.body
      )}
      <ConfirmDialog
        isOpen={confirmState.isOpen}
        title={confirmState.title}
        message={confirmState.message}
        confirmText="拨打"
        cancelText="取消"
        onConfirm={handleConfirm}
        onCancel={() => setConfirmState({ ...confirmState, isOpen: false })}
        confirmStyle={confirmState.phone === '120' || confirmState.phone === '110' ? 'danger' : ''}
      />

      {/* 桌面端不支持拨号提示弹窗 */}
      <ConfirmDialog
        isOpen={showDesktopTip}
        title="功能仅在手机端可用"
        message="拨号功能需要手机硬件支持，电脑端暂不可用。请您用手机登录老人端拨打家属电话或 120/110 急救电话，给您带来不便敬请谅解。"
        confirmText="我知道了"
        hideCancel
        onConfirm={() => setShowDesktopTip(false)}
        onCancel={() => setShowDesktopTip(false)}
      />
    </>
  );
};

export default ContactModal;
