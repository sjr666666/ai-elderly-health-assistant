import { useEffect, useRef } from 'react';
import { createPortal } from 'react-dom';

/**
 * 确认弹窗
 * @param {boolean} hideCancel 是否隐藏取消按钮（用于纯提示场景，单按钮模式）
 */
const ConfirmDialog = ({ isOpen, title, message, confirmText, cancelText, onConfirm, onCancel, confirmStyle, hideCancel }) => {
  const overlayRef = useRef(null);

  useEffect(() => {
    if (isOpen) {
      document.body.style.overflow = 'hidden';
    }
    return () => {
      document.body.style.overflow = '';
    };
  }, [isOpen]);

  const handleOverlayClick = (e) => {
    if (e.target === overlayRef.current) {
      onCancel();
    }
  };

  if (!isOpen) return null;

  return createPortal(
    <div
      ref={overlayRef}
      className="confirm-overlay"
      onClick={handleOverlayClick}
    >
      <div className="confirm-content">
        <div className="confirm-header">
          <h3>{title}</h3>
        </div>
        <div className="confirm-body">
          <p>{message}</p>
        </div>
        <div className="confirm-actions">
          {!hideCancel && (
            <button className="confirm-cancel-btn" onClick={onCancel}>
              {cancelText || '取消'}
            </button>
          )}
          <button
            className={`confirm-ok-btn ${confirmStyle || ''}`}
            onClick={onConfirm}
          >
            {confirmText || '确定'}
          </button>
        </div>
      </div>
    </div>,
    document.body
  );
};

export default ConfirmDialog;
