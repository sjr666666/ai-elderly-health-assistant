import React, { useState } from 'react';

function Register({ onRegister }) {
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [realName, setRealName] = useState('');
  const [age, setAge] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState('');
  const [successData, setSuccessData] = useState(null);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');

    if (!username.trim() || username.trim().length < 4) {
      setError('用户名不能少于4个字符');
      return;
    }

    if (!password || password.length < 6) {
      setError('密码不能少于6个字符');
      return;
    }

    if (password !== confirmPassword) {
      setError('两次输入的密码不一致');
      return;
    }

    if (!realName.trim()) {
      setError('请输入真实姓名');
      return;
    }

    if (!age || parseInt(age) <= 0 || parseInt(age) > 150) {
      setError('请输入有效的年龄（1-150）');
      return;
    }

    setIsLoading(true);

    try {
      const response = await fetch('http://localhost:8080/api/v1/user/register', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          username: username.trim(),
          password: password,
          realName: realName.trim(),
          age: parseInt(age),
        }),
      });

      const data = await response.json();

      if (response.ok && data.code === 200) {
        const userData = {
          ...data.data,
          realName: realName.trim(),
          age: parseInt(age)
        };
        localStorage.setItem('registeredUser', JSON.stringify(userData));
        setSuccessData(userData);
      } else {
        setError(data.message || '注册失败，请重试');
      }
    } catch (err) {
      setError('网络连接失败，请稍后重试');
    } finally {
      setIsLoading(false);
    }
  };

  const handleLogin = () => {
    setSuccessData(null);
    setUsername('');
    setPassword('');
    setConfirmPassword('');
    setRealName('');
    setAge('');
    onRegister(null);
  };

  if (successData) {
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
          maxWidth: '580px',
          textAlign: 'center'
        }}>
          <div style={{ fontSize: '80px', marginBottom: '20px' }}>🎉</div>
          <h1 style={{ fontSize: '36px', fontWeight: '800', color: '#4CAF50', marginBottom: '16px' }}>注册成功！</h1>
          <p style={{ fontSize: '20px', color: '#6B6B6B', marginBottom: '32px' }}>请记住您的账号密码，用于登录</p>

          <div style={{
            background: 'linear-gradient(135deg, #E3F2FD 0%, #F1F8E9 100%)',
            padding: '32px',
            borderRadius: '20px',
            marginBottom: '32px',
            textAlign: 'left'
          }}>
            <div style={{ marginBottom: '20px' }}>
              <p style={{ fontSize: '16px', color: '#6B6B6B', marginBottom: '8px' }}>用户名</p>
              <p style={{ fontSize: '28px', fontWeight: '700', color: '#1976D2', wordBreak: 'break-all' }}>{successData.username}</p>
            </div>
            <div>
              <p style={{ fontSize: '16px', color: '#6B6B6B', marginBottom: '8px' }}>密码</p>
              <p style={{ fontSize: '28px', fontWeight: '700', color: '#388E3C', wordBreak: 'break-all' }}>{successData.password}</p>
            </div>
          </div>

          <div style={{
            background: '#FFF3E0',
            padding: '20px',
            borderRadius: '12px',
            marginBottom: '32px',
            border: '2px solid #FFB74D'
          }}>
            <p style={{ fontSize: '16px', color: '#E65100', fontWeight: '600' }}>
              ⚠️ 请妥善保管您的账号密码！
            </p>
          </div>

          <button
            onClick={handleLogin}
            style={{
              width: '100%',
              padding: '22px',
              fontSize: '22px',
              fontWeight: '700',
              background: 'linear-gradient(135deg, #4CAF50 0%, #81C784 100%)',
              color: 'white',
              border: 'none',
              borderRadius: '20px',
              cursor: 'pointer',
              boxShadow: '0 8px 32px rgba(76, 175, 80, 0.3)',
              transition: 'all 0.3s ease',
            }}
            onMouseEnter={(e) => {
              e.target.style.transform = 'translateY(-4px) scale(1.02)';
              e.target.style.boxShadow = '0 12px 40px rgba(76, 175, 80, 0.4)';
            }}
            onMouseLeave={(e) => {
              e.target.style.transform = 'translateY(0) scale(1)';
              e.target.style.boxShadow = '0 8px 32px rgba(76, 175, 80, 0.3)';
            }}
          >
            🔐 前往登录
          </button>
        </div>
      </div>
    );
  }

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
        maxWidth: '580px'
      }}>
        <div style={{ textAlign: 'center', marginBottom: '40px' }}>
          <div style={{ fontSize: '80px', marginBottom: '20px', filter: 'drop-shadow(2px 4px 8px rgba(0,0,0,0.1))' }}>👴</div>
          <h1 style={{ fontSize: '36px', fontWeight: '800', color: '#4A90E2', marginBottom: '8px' }}>创建老人档案</h1>
          <p style={{ fontSize: '18px', color: '#6B6B6B', marginTop: '8px' }}>请填写基本信息完成注册</p>
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

        <form onSubmit={handleSubmit}>
          <div style={{ marginBottom: '24px' }}>
            <label style={{
              fontSize: '20px',
              fontWeight: '600',
              marginBottom: '12px',
              display: 'block',
              color: '#3D3D3D'
            }}>
              <span style={{ color: '#E74C3C', marginRight: '4px' }}>*</span>
              用户名（用于登录）
            </label>
            <input
              type="text"
              value={username}
              onChange={(e) => setUsername(e.target.value)}
              placeholder="请输入用户名（至少4个字符）"
              style={{
                width: '100%',
                padding: '18px 24px',
                fontSize: '20px',
                border: '3px solid #F0EBE3',
                borderRadius: '16px',
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

          <div style={{ marginBottom: '24px' }}>
            <label style={{
              fontSize: '20px',
              fontWeight: '600',
              marginBottom: '12px',
              display: 'block',
              color: '#3D3D3D'
            }}>
              <span style={{ color: '#E74C3C', marginRight: '4px' }}>*</span>
              密码
            </label>
            <input
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              placeholder="请输入密码（至少6个字符）"
              style={{
                width: '100%',
                padding: '18px 24px',
                fontSize: '20px',
                border: '3px solid #F0EBE3',
                borderRadius: '16px',
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

          <div style={{ marginBottom: '24px' }}>
            <label style={{
              fontSize: '20px',
              fontWeight: '600',
              marginBottom: '12px',
              display: 'block',
              color: '#3D3D3D'
            }}>
              <span style={{ color: '#E74C3C', marginRight: '4px' }}>*</span>
              确认密码
            </label>
            <input
              type="password"
              value={confirmPassword}
              onChange={(e) => setConfirmPassword(e.target.value)}
              placeholder="请再次输入密码"
              style={{
                width: '100%',
                padding: '18px 24px',
                fontSize: '20px',
                border: '3px solid #F0EBE3',
                borderRadius: '16px',
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

          <div style={{ marginBottom: '24px' }}>
            <label style={{
              fontSize: '20px',
              fontWeight: '600',
              marginBottom: '12px',
              display: 'block',
              color: '#3D3D3D'
            }}>
              <span style={{ color: '#E74C3C', marginRight: '4px' }}>*</span>
              真实姓名
            </label>
            <input
              type="text"
              value={realName}
              onChange={(e) => setRealName(e.target.value)}
              placeholder="请输入老人姓名"
              style={{
                width: '100%',
                padding: '18px 24px',
                fontSize: '20px',
                border: '3px solid #F0EBE3',
                borderRadius: '16px',
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

          <div style={{ marginBottom: '32px' }}>
            <label style={{
              fontSize: '20px',
              fontWeight: '600',
              marginBottom: '12px',
              display: 'block',
              color: '#3D3D3D'
            }}>
              <span style={{ color: '#E74C3C', marginRight: '4px' }}>*</span>
              年龄
            </label>
            <input
              type="number"
              value={age}
              onChange={(e) => setAge(e.target.value)}
              placeholder="请输入年龄"
              min="1"
              max="150"
              style={{
                width: '100%',
                padding: '18px 24px',
                fontSize: '20px',
                border: '3px solid #F0EBE3',
                borderRadius: '16px',
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
            type="submit"
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
                注册中...
              </>
            ) : (
              '✅ 完成注册'
            )}
          </button>

          <div style={{
            marginTop: '32px',
            padding: '20px',
            background: 'linear-gradient(135deg, #E8F5F0 0%, #FFE4D1 100%)',
            borderRadius: '16px',
            textAlign: 'center'
          }}>
            <p style={{ fontSize: '16px', color: '#6B6B6B', marginBottom: '8px' }}>已注册过？</p>
            <button
              type="button"
              onClick={handleLogin}
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
              直接登录
            </button>
          </div>
        </form>

        <style>{`
          @keyframes spin {
            to { transform: rotate(360deg); }
          }
        `}</style>
      </div>
    </div>
  );
}

export default Register;