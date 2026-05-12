import React, { useState } from 'react';

function Login({ onLogin }) {
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [isLoading, setIsLoading] = useState(false);

  const handleLogin = () => {
    if (!username || !password) {
      alert('请输入用户名和密码');
      return;
    }

    setIsLoading(true);
    setTimeout(() => {
      onLogin({
        username,
        password,
        needProfile: true
      });
      setIsLoading(false);
    }, 1000);
  };

  const handleKeyPress = (e) => {
    if (e.key === 'Enter') {
      handleLogin();
    }
  };

  return (
    <div style={{
      minHeight: '100vh',
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

        <div style={{ marginBottom: '28px' }}>
          <label style={{
            fontSize: '20px',
            fontWeight: '600',
            marginBottom: '12px',
            display: 'block',
            color: '#3D3D3D'
          }}>用户名</label>
          <input
            type="text"
            value={username}
            onChange={(e) => setUsername(e.target.value)}
            onKeyPress={handleKeyPress}
            placeholder="请输入用户名"
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
          />
        </div>

        <div style={{ marginBottom: '36px' }}>
          <label style={{
            fontSize: '20px',
            fontWeight: '600',
            marginBottom: '12px',
            display: 'block',
            color: '#3D3D3D'
          }}>密码</label>
          <input
            type="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            onKeyPress={handleKeyPress}
            placeholder="请输入密码"
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
          />
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
          <p style={{ fontSize: '16px', color: '#6B6B6B', marginBottom: '8px' }}>测试账号</p>
          <p style={{ fontSize: '18px', color: '#4A90E2', fontWeight: '600' }}>admin / 123456</p>
        </div>
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
