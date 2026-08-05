import React, { useState } from 'react';
import { saveToken } from '../../utils/guardianApi';
import { validateCredentials } from '../../utils/authValidation';
import './guardian.css';

function GuardianLogin({ onLogin, onSwitchToElder, onShowRegister, registerSuccess }) {
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState('');

  const handleLogin = async (e) => {
    if (e) e.preventDefault();

    const credentialError = validateCredentials(username, password);
    if (credentialError) {
      setError(credentialError);
      return;
    }

    setIsLoading(true);
    setError('');

    try {
      const response = await fetch('/api/v1/user/login', {
        method: 'POST',
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
        // 校验角色：家属端仅允许 family 账号登录
        if (userData.role !== 'family') {
          setError('该账号不是家属账号，请使用老人端登录');
          setIsLoading(false);
          return;
        }
        // 保存JWT token（用户信息不存localStorage，通过API获取）
        if (userData.token) {
          saveToken(userData.token);
        }
        const userInfo = { ...userData };
        delete userInfo.token;
        onLogin(userInfo);
      } else {
        setError(data.message || '用户名或密码错误');
      }
    } catch (err) {
      console.error('家属端登录失败:', err);
      setError('网络连接失败，请检查网络后重试');
    } finally {
      setIsLoading(false);
    }
  };

  const handleKeyPress = (e) => {
    if (e.key === 'Enter') {
      handleLogin();
    }
  };

  const handleForgotPassword = () => {
    setError('请联系管理员重置密码');
  };

  return (
    <div className="g-login">
      <div className="g-login-card">
        {/* 头部 Logo */}
        <div className="g-login-header">
          <div className="g-login-logo">
            <svg width="48" height="48" viewBox="0 0 24 24" fill="white">
              <path d="M16 11c1.66 0 2.99-1.34 2.99-3S17.66 5 16 5c-1.66 0-3 1.34-3 3s1.34 3 3 3zm-8 0c1.66 0 2.99-1.34 2.99-3S9.66 5 8 5C6.34 5 5 6.34 5 8s1.34 3 3 3zm0 2c-2.33 0-7 1.17-7 3.5V19h14v-2.5c0-2.33-4.67-3.5-7-3.5zm8 0c-.29 0-.62.02-.97.05 1.16.84 1.97 1.97 1.97 3.45V19h6v-2.5c0-2.33-4.67-3.5-7-3.5z"/>
            </svg>
          </div>
          <h1>家属守护</h1>
          <p>关爱家人，随时守护</p>
        </div>

        {/* 错误提示 */}
        {error && (
          <div className="g-error-tip" role="alert">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="currentColor" style={{ marginRight: 6, verticalAlign: 'middle' }}>
              <path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm1 15h-2v-2h2v2zm0-4h-2V7h2v6z"/>
            </svg>
            {error}
          </div>
        )}

        {/* 注册成功提示 */}
        {registerSuccess && (
          <div className="g-success-tip" role="status">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="currentColor" style={{ marginRight: 6, verticalAlign: 'middle' }}>
              <path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm-2 15l-5-5 1.41-1.41L10 14.17l7.59-7.59L19 8l-9 9z"/>
            </svg>
            {registerSuccess}
          </div>
        )}

        <form onSubmit={handleLogin}>
          {/* 用户名输入 */}
          <div className="g-form-group">
            <label htmlFor="guardian-username">用户名</label>
            <div className="g-input-wrapper">
              <svg className="g-input-icon" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/>
                <circle cx="12" cy="7" r="4"/>
              </svg>
              <input
                id="guardian-username"
                type="text"
                value={username}
                onChange={(e) => {
                  setUsername(e.target.value);
                  setError('');
                }}
                onKeyPress={handleKeyPress}
                placeholder="请输入用户名"
                autoComplete="username"
                disabled={isLoading}
              />
            </div>
          </div>

          {/* 密码输入 */}
          <div className="g-form-group">
            <label htmlFor="guardian-password">密码</label>
            <div className="g-input-wrapper">
              <svg className="g-input-icon" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                <rect x="3" y="11" width="18" height="11" rx="2" ry="2"/>
                <path d="M7 11V7a5 5 0 0 1 10 0v4"/>
              </svg>
              <input
                id="guardian-password"
                type={showPassword ? "text" : "password"}
                value={password}
                onChange={(e) => {
                  setPassword(e.target.value);
                  setError('');
                }}
                onKeyPress={handleKeyPress}
                placeholder="请输入密码"
                autoComplete="current-password"
                disabled={isLoading}
              />
              <button
                type="button"
                className="g-pwd-toggle"
                onClick={() => setShowPassword(!showPassword)}
                aria-label={showPassword ? '隐藏密码' : '显示密码'}
                tabIndex={-1}
              >
                {showPassword ? (
                  <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                    <path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19m-6.72-1.07a3 3 0 1 1-4.24-4.24"/>
                    <line x1="1" y1="1" x2="23" y2="23"/>
                  </svg>
                ) : (
                  <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                    <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/>
                    <circle cx="12" cy="12" r="3"/>
                  </svg>
                )}
              </button>
            </div>
          </div>

          {/* 忘记密码链接 */}
          <div className="g-login-forgot">
            <button type="button" onClick={handleForgotPassword}>忘记密码？</button>
          </div>

          {/* 登录按钮 */}
          <button
            type="submit"
            className="g-btn g-btn-primary g-login-submit"
            disabled={isLoading}
          >
            {isLoading ? (
              <>
                <span className="g-login-spinner"></span>
                登录中...
              </>
            ) : (
              '登 录'
            )}
          </button>
        </form>

        {/* 切换到老人端登录 */}
        <div className="g-login-switch">
          <button type="button" onClick={onShowRegister}>
            没有家属账号？立即注册
          </button>
          <button type="button" onClick={onSwitchToElder}>
            老人端登录
          </button>
        </div>
      </div>
    </div>
  );
}

export default GuardianLogin;
