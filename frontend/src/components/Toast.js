import { useEffect, useState, useRef, useCallback } from 'react';
import { createPortal } from 'react-dom';

const Toast = ({ message, isVisible, onClose }) => {
  const [show, setShow] = useState(false);
  const [isExiting, setIsExiting] = useState(false);
  const timerRef = useRef(null);
  const exitTimerRef = useRef(null);

  const handleClose = useCallback(() => {
    setIsExiting(true);
    exitTimerRef.current = setTimeout(() => {
      setShow(false);
      onClose();
    }, 300);
  }, [onClose]);

  useEffect(() => {
    if (isVisible) {
      setIsExiting(false);
      setShow(true);
      
      timerRef.current = setTimeout(handleClose, 2000);
    }

    return () => {
      if (timerRef.current) {
        clearTimeout(timerRef.current);
      }
      if (exitTimerRef.current) {
        clearTimeout(exitTimerRef.current);
      }
    };
  }, [isVisible, handleClose]);

  useEffect(() => {
    if (!isVisible && show) {
      handleClose();
    }
  }, [isVisible, show, handleClose]);

  if (!show) return null;

  return createPortal(
    <div className="toast-overlay">
      <div className={`toast-content ${isExiting ? 'exiting' : ''}`}>
        <div className="toast-icon">✓</div>
        <div className="toast-message">{message}</div>
      </div>
    </div>,
    document.body
  );
};

export default Toast;