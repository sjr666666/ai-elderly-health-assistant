import React, { useState } from 'react';
import { saveToken } from '../utils/elderApi';
import { validateCredentials } from '../utils/authValidation';

function Login({ onLogin, onShowRegister, onSwitchToGuardian, registerSuccess }) {
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState('');

  const handleLogin = async (e) => {
    // 阻止表单默认提交
    if (e) e.preventDefault();

    const credentialError = validateCredentials(username, password);
    if (credentialError) {
      const usernameInput = document.querySelector('input[type="text"]');
      const passwordInput = document.querySelector('input[placeholder="请输入密码"]');
      
      if (!username.trim()) {
        usernameInput.setCustomValidity(credentialError);
        usernameInput.reportValidity();
        usernameInput.focus();
        return;
      }
      
      if (!password) {
        passwordInput.setCustomValidity(credentialError);
        passwordInput.reportValidity();
        passwordInput.focus();
        return;
      }
    }

    setIsLoading(true);
    setError('');

    try {
      const response = await fetch('/api/v1/user/login', {
        method: 'POST',
        credentials: 'include',
        headers: {
          'Content-Type': 'application/json;charset=UTF-8',
          'Accept': 'application/json',
        },
        body: JSON.stringify({
          username: username.trim(),
          password: password,
        }),
      });

      const data = await response.json();

      if (response.ok && data.code === 200) {
        const userData = data.data;
        // 老人端登录界面仅允许老人账号登录，家属账号请使用家属端登录
        if (userData.role === 'family') {
          setError('家属账号请使用家属端登录');
          setIsLoading(false);
          return;
        }
        // 保存JWT token
        if (userData.token) {
          saveToken(userData.token);
        }
        // 传递用户数据（不含token）
        const userInfo = { ...userData };
        delete userInfo.token;
        onLogin(userInfo);
      } else {
        setError(data.message || '用户名或密码错误');
      }
    } catch (err) {
      console.error('登录请求失败:', err);
      setError('网络连接失败，请稍后重试');
    } finally {
      setIsLoading(false);
    }
  };

  const handleKeyPress = (e) => {
    if (e.key === 'Enter') {
      handleLogin();
    }
  };

  return (
    <div style={{
      minHeight: 'calc(var(--vh, 1vh) * 100)',
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'center',
      background: 'linear-gradient(135deg, #F5C6A5 0%, #98D4BB 50%, #FAF7F2 100%)',
      padding: '20px'
    }}>
      <div style={{
        background: 'white',
        padding: '50px 60px',
        borderRadius: '36px',
        boxShadow: '0 16px 64px rgba(0, 0, 0, 0.15)',
        width: '100%',
        maxWidth: '480px'
      }}>
        <div style={{ textAlign: 'center', marginBottom: '40px' }}>
          <div style={{ fontSize: '80px', marginBottom: '20px', filter: 'drop-shadow(2px 4px 8px rgba(0,0,0,0.1))' }}>💊</div>
          <h1 style={{ fontSize: '36px', fontWeight: '800', color: '#4A90E2', marginBottom: '8px' }}>AI 药管家</h1>
          <p style={{ fontSize: '18px', color: '#6B6B6B', marginTop: '8px' }}>您身边贴心的用药安全小助手</p>
        </div>

        {error && (
          <div style={{
            background: '#FFEBEE',
            color: '#E74C3C',
            padding: '16px 20px',
            borderRadius: '12px',
            marginBottom: '24px',
            fontSize: '16px',
            fontWeight: '600'
          }}>
            ⚠️ {error}
          </div>
        )}

        {registerSuccess && (
          <div style={{
            background: '#E8F5E9',
            color: '#2E7D32',
            padding: '16px 20px',
            borderRadius: '12px',
            marginBottom: '24px',
            fontSize: '16px',
            fontWeight: '600'
          }}>
            ✅ {registerSuccess}
          </div>
        )}

        <form onSubmit={handleLogin}>
          <div style={{ marginBottom: '28px' }}>
            <label style={{
              fontSize: '20px',
              fontWeight: '600',
              marginBottom: '12px',
              display: 'block',
              color: '#3D3D3D'
            }}>用户名 <span style={{ color: '#E74C3C' }}>*</span></label>
            <input
              type="text"
              value={username}
              onChange={(e) => {
                setUsername(e.target.value);
                // 清除自定义验证错误
                e.target.setCustomValidity('');
              }}
              onKeyPress={handleKeyPress}
              placeholder="请输入用户名"
              required
              minLength={4}
              style={{
                width: '100%',
                padding: '20px 24px',
                fontSize: '20px',
                border: '3px solid #F0EBE3',
                borderRadius: '20px',
                outline: 'none',
                transition: 'all 0.3s ease',
                background: '#FAF7F2',
                fontFamily: 'inherit'
              }}
              onFocus={(e) => {
                e.target.style.borderColor = '#4A90E2';
                e.target.style.boxShadow = '0 0 0 6px rgba(74, 144, 226, 0.12)';
                e.target.style.background = 'white';
              }}
              onBlur={(e) => {
                e.target.style.borderColor = '#F0EBE3';
                e.target.style.boxShadow = 'none';
                e.target.style.background = '#FAF7F2';
              }}
              onInvalid={(e) => {
                if (!e.target.value) {
                  e.target.setCustomValidity('请输入用户名');
                } else if (e.target.value.length < 4) {
                  e.target.setCustomValidity(`用户名至少需要 4 个字符（当前 ${e.target.value.length} 个）`);
                }
              }}
            />
          </div>

          <div style={{ marginBottom: '36px' }}>
            <label style={{
              fontSize: '20px',
              fontWeight: '600',
              marginBottom: '12px',
              display: 'block',
              color: '#3D3D3D'
            }}>密码 <span style={{ color: '#E74C3C' }}>*</span></label>
            <div style={{ position: 'relative' }}>
              <input
                type={showPassword ? "text" : "password"}
                value={password}
                onChange={(e) => {
                  setPassword(e.target.value);
                  // 清除自定义验证错误
                  e.target.setCustomValidity('');
                }}
                onKeyPress={handleKeyPress}
                placeholder="请输入密码"
                required
                minLength={6}
                style={{
                  width: '100%',
                  padding: '20px 60px 20px 24px',
                  fontSize: '20px',
                  border: '3px solid #F0EBE3',
                  borderRadius: '20px',
                  outline: 'none',
                  transition: 'all 0.3s ease',
                  background: '#FAF7F2',
                  fontFamily: 'inherit',
                  boxSizing: 'border-box'
                }}
                onFocus={(e) => {
                  e.target.style.borderColor = '#4A90E2';
                  e.target.style.boxShadow = '0 0 0 6px rgba(74, 144, 226, 0.12)';
                  e.target.style.background = 'white';
                }}
                onBlur={(e) => {
                  e.target.style.borderColor = '#F0EBE3';
                  e.target.style.boxShadow = 'none';
                  e.target.style.background = '#FAF7F2';
                }}
                onInvalid={(e) => {
                  if (!e.target.value) {
                    e.target.setCustomValidity('请输入密码');
                  } else if (e.target.value.length < 6) {
                    e.target.setCustomValidity(`密码至少需要 6 个字符（当前 ${e.target.value.length} 个）`);
                  }
                }}
              />
              <button
                type="button"
                onClick={() => setShowPassword(!showPassword)}
                style={{
                  position: 'absolute',
                  right: '16px',
                  top: '50%',
                  transform: 'translateY(-50%)',
                  background: 'none',
                  border: 'none',
                  cursor: 'pointer',
                  padding: '8px',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  zIndex: 1
                }}
              >
                {showPassword ? (
                  <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="#6B6B6B" strokeWidth="2">
                    <path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19m-6.72-1.07a3 3 0 1 1-4.24-4.24"/>
                    <line x1="1" y1="1" x2="23" y2="23"/>
                  </svg>
                ) : (
                  <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="#6B6B6B" strokeWidth="2">
                    <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/>
                    <circle cx="12" cy="12" r="3"/>
                  </svg>
                )}
              </button>
            </div>
          </div>

        <button
          onClick={handleLogin}
          disabled={isLoading}
          style={{
            width: '100%',
            padding: '22px',
            fontSize: '22px',
            fontWeight: '700',
            background: isLoading
              ? 'linear-gradient(135deg, #7FB3F5 0%, #98D4BB 100%)'
              : 'linear-gradient(135deg, #4A90E2 0%, #98D4BB 100%)',
            color: 'white',
            border: 'none',
            borderRadius: '20px',
            cursor: isLoading ? 'not-allowed' : 'pointer',
            boxShadow: '0 8px 32px rgba(74, 144, 226, 0.3)',
            transition: 'all 0.3s ease',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            gap: '12px'
          }}
          onMouseEnter={(e) => {
            if (!isLoading) {
              e.target.style.transform = 'translateY(-4px) scale(1.02)';
              e.target.style.boxShadow = '0 12px 40px rgba(74, 144, 226, 0.4)';
            }
          }}
          onMouseLeave={(e) => {
            e.target.style.transform = 'translateY(0) scale(1)';
            e.target.style.boxShadow = '0 8px 32px rgba(74, 144, 226, 0.3)';
          }}
        >
          {isLoading ? (
            <>
              <span style={{ display: 'inline-block', animation: 'spin 1s linear infinite' }}>⏳</span>
              登录中...
            </>
          ) : (
            '登 录'
          )}
        </button>

        <div style={{
          marginTop: '32px',
          padding: '20px',
          background: 'linear-gradient(135deg, #E8F5F0 0%, #FFE4D1 100%)',
          borderRadius: '16px',
          textAlign: 'center'
        }}>
          <p style={{ fontSize: '16px', color: '#6B6B6B', marginBottom: '8px' }}>还没有账号？</p>
          <button
            type="button"
            onClick={() => onShowRegister && onShowRegister()}
            style={{
              background: 'none',
              border: 'none',
              color: '#4A90E2',
              fontSize: '18px',
              fontWeight: '600',
              cursor: 'pointer',
              textDecoration: 'underline'
            }}
          >
            立即注册
          </button>
        </div>

        {/* 家属端登录入口 */}
        <div style={{
          marginTop: '16px',
          textAlign: 'center',
          fontSize: '14px',
          color: '#999'
        }}>
          <span>家属账号？</span>
          <button
            type="button"
            onClick={() => onSwitchToGuardian && onSwitchToGuardian()}
            style={{
              background: 'none',
              border: 'none',
              color: '#3A7BC8',
              fontSize: '14px',
              fontWeight: '500',
              cursor: 'pointer',
              marginLeft: '4px',
              padding: 0,
              transition: 'all 0.2s ease'
            }}
            onMouseEnter={(e) => {
              e.target.style.color = '#2D6AB5';
              e.target.style.transform = 'scale(1.05)';
            }}
            onMouseLeave={(e) => {
              e.target.style.color = '#3A7BC8';
              e.target.style.transform = 'scale(1)';
            }}
          >
            家属端登录 →
          </button>
        </div>
        </form>
      </div>

      <style>{`
        @keyframes spin {
          to { transform: rotate(360deg); }
        }
      `}</style>
    </div>
  );
}

export default Login;
