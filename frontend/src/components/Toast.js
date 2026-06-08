import { createContext, useContext, useState, useCallback, useRef, useEffect } from 'react';
import { createPortal } from 'react-dom';

const ToastContext = createContext(null);

// Toast types with icons and colors
const TOAST_CONFIG = {
  success: {
    icon: '✓',
    bgColor: '#E8F5E9',
    borderColor: '#4CAF50',
    iconColor: '#4CAF50',
    textColor: '#2E7D32'
  },
  error: {
    icon: '✕',
    bgColor: '#FFEBEE',
    borderColor: '#F44336',
    iconColor: '#F44336',
    textColor: '#C62828'
  },
  warning: {
    icon: '⚠',
    bgColor: '#FFF3E0',
    borderColor: '#FF9800',
    iconColor: '#FF9800',
    textColor: '#E65100'
  },
  info: {
    icon: 'ℹ',
    bgColor: '#E3F2FD',
    borderColor: '#2196F3',
    iconColor: '#2196F3',
    textColor: '#1565C0'
  }
};

const ToastItem = ({ message, type = 'success', onClose }) => {
  const [isExiting, setIsExiting] = useState(false);
  const exitTimerRef = useRef(null);
  const config = TOAST_CONFIG[type] || TOAST_CONFIG.success;

  const handleClose = useCallback(() => {
    setIsExiting(true);
    exitTimerRef.current = setTimeout(() => {
      onClose();
    }, 300);
  }, [onClose]);

  useEffect(() => {
    const timer = setTimeout(handleClose, 2500);
    return () => {
      clearTimeout(timer);
      if (exitTimerRef.current) {
        clearTimeout(exitTimerRef.current);
      }
    };
  }, [handleClose]);

  return (
    <div
      className={`toast-item ${isExiting ? 'toast-exiting' : ''}`}
      style={{
        display: 'flex',
        alignItems: 'center',
        gap: '12px',
        padding: '14px 20px',
        background: config.bgColor,
        border: `2px solid ${config.borderColor}`,
        borderRadius: '12px',
        boxShadow: '0 4px 12px rgba(0, 0, 0, 0.15)',
        minWidth: '200px',
        maxWidth: '320px',
        animation: isExiting ? 'toast-exit 0.3s ease forwards' : 'toast-enter 0.3s ease',
      }}
    >
      <div
        style={{
          width: '28px',
          height: '28px',
          borderRadius: '50%',
          background: config.iconColor,
          color: 'white',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          fontSize: '16px',
          fontWeight: 'bold',
          flexShrink: 0
        }}
      >
        {config.icon}
      </div>
      <div
        style={{
          flex: 1,
          fontSize: '15px',
          color: config.textColor,
          fontWeight: '500',
          lineHeight: 1.4,
          wordBreak: 'break-word'
        }}
      >
        {message}
      </div>
      <button
        onClick={handleClose}
        style={{
          background: 'none',
          border: 'none',
          cursor: 'pointer',
          padding: '4px',
          color: config.textColor,
          opacity: 0.6,
          fontSize: '18px',
          lineHeight: 1,
          flexShrink: 0
        }}
      >
        ✕
      </button>
    </div>
  );
};

const ToastContainer = ({ toasts, removeToast }) => {
  if (toasts.length === 0) return null;

  return createPortal(
    <div
      style={{
        position: 'fixed',
        top: '20px',
        left: '50%',
        transform: 'translateX(-50%)',
        zIndex: 9999,
        display: 'flex',
        flexDirection: 'column',
        gap: '10px',
        alignItems: 'center',
        pointerEvents: 'none'
      }}
    >
      <style>
        {`
          @keyframes toast-enter {
            from {
              opacity: 0;
              transform: translateY(-20px);
            }
            to {
              opacity: 1;
              transform: translateY(0);
            }
          }
          @keyframes toast-exit {
            from {
              opacity: 1;
              transform: translateY(0);
            }
            to {
              opacity: 0;
              transform: translateY(-20px);
            }
          }
        `}
      </style>
      {toasts.map((toast) => (
        <div key={toast.id} style={{ pointerEvents: 'auto' }}>
          <ToastItem
            message={toast.message}
            type={toast.type}
            onClose={() => removeToast(toast.id)}
          />
        </div>
      ))}
    </div>,
    document.body
  );
};

export const ToastProvider = ({ children }) => {
  const [toasts, setToasts] = useState([]);
  const idRef = useRef(0);

  const showToast = useCallback((message, type = 'success') => {
    const id = ++idRef.current;
    setToasts(prev => [...prev, { id, message, type }]);
  }, []);

  const removeToast = useCallback((id) => {
    setToasts(prev => prev.filter(toast => toast.id !== id));
  }, []);

  return (
    <ToastContext.Provider value={{ showToast }}>
      {children}
      <ToastContainer toasts={toasts} removeToast={removeToast} />
    </ToastContext.Provider>
  );
};

export const useToast = () => {
  const context = useContext(ToastContext);
  if (!context) {
    throw new Error('useToast must be used within a ToastProvider');
  }
  return context;
};

export default ToastProvider;
