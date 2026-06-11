import React, { useState } from 'react';
import './guardian.css';

function GuardianLogin({ onLogin }) {
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState('');

  const handleLogin = async (e) => {
    if (e) e.preventDefault();
    if (!username.trim() || !password) { setError('请输入用户名和密码'); return; }
    setIsLoading(true); setError('');
    try {
      const response = await fetch('/api/v1/user/login', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json;charset=UTF-8', 'Accept': 'application/json' },
        body: JSON.stringify({ username: username.trim(), password }),
      });
      const data = await response.json();
      if (response.ok && data.code === 200) {
        const userData = data.data;
        if (userData.role !== 'family') { setError('该账号不是家属账号'); setIsLoading(false); return; }
        localStorage.setItem('guardianUser', JSON.stringify(userData));
        onLogin(userData);
      } else { setError(data.message || '用户名或密码错误'); }
    } catch { setError('网络连接失败'); }
    finally { setIsLoading(false); }
  };

  return (
    <div className="g-login">
      <div className="g-login-card">
        <div className="g-login-header">
          <svg width="56" height="56" viewBox="0 0 24 24" fill="var(--primary)"><path d="M16 11c1.66 0 2.99-1.34 2.99-3S17.66 5 16 5c-1.66 0-3 1.34-3 3s1.34 3 3 3zm-8 0c1.66 0 2.99-1.34 2.99-3S9.66 5 8 5C6.34 5 5 6.34 5 8s1.34 3 3 3zm0 2c-2.33 0-7 1.17-7 3.5V19h14v-2.5c0-2.33-4.67-3.5-7-3.5zm8 0c-.29 0-.62.02-.97.05 1.16.84 1.97 1.97 1.97 3.45V19h6v-2.5c0-2.33-4.67-3.5-7-3.5z"/></svg>
          <h1>家属守护</h1>
          <p>关爱家人，随时守护</p>
        </div>

        {error && <div className="g-error-tip">{error}</div>}

        <form onSubmit={handleLogin}>
          <div className="g-form-group">
            <label>用户名 <span className="g-required">*</span></label>
            <input type="text" value={username} onChange={(e) => setUsername(e.target.value)} placeholder="请输入用户名" required />
          </div>
          <div className="g-form-group">
            <label>密码 <span className="g-required">*</span></label>
            <div className="g-pwd-wrapper">
              <input type={showPassword ? "text" : "password"} value={password} onChange={(e) => setPassword(e.target.value)} placeholder="请输入密码" required />
              <button type="button" className="g-pwd-toggle" onClick={() => setShowPassword(!showPassword)}>
                {showPassword ? '隐藏' : '显示'}
              </button>
            </div>
          </div>
          <button type="submit" className="g-btn g-btn-primary" disabled={isLoading}>
            {isLoading ? '登录中...' : '登 录'}
          </button>
        </form>
      </div>
    </div>
  );
}

export default GuardianLogin;
