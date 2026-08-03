import React, { useState, useRef } from 'react';
import { saveToken } from '../utils/elderApi';

function Register({ onRegister }) {
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [confirmPassword, setConfirmPassword] = useState('');
  const [showConfirmPassword, setShowConfirmPassword] = useState(false);
  const [realName, setRealName] = useState('');
  const [age, setAge] = useState('');
  const [role, setRole] = useState('elder');
  const [phone, setPhone] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState('');
  const [fieldErrors, setFieldErrors] = useState({});

  // 创建输入框引用
  const usernameRef = useRef(null);
  const passwordRef = useRef(null);
  const confirmPasswordRef = useRef(null);
  const realNameRef = useRef(null);
  const ageRef = useRef(null);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');

    const errors = [];
    if (!/^[A-Za-z0-9_]{4,20}$/.test(username.trim())) errors.push('用户名需为4-20位字母、数字或下划线');
    if (!/^.{6,20}$/.test(password)) errors.push('密码长度需为6-20位');
    if (password !== confirmPassword) errors.push('两次输入的密码不一致');
    if (!realName.trim() || realName.trim().length > 50) errors.push('请输入真实姓名，且不能超过50个字符');
    if (age === '' || Number(age) < 0 || Number(age) > 150) errors.push('年龄必须在0-150之间');
    const nextFieldErrors = {};
    if (!/^[A-Za-z0-9_]{4,20}$/.test(username.trim())) nextFieldErrors.username = '用户名需为4-20位字母、数字或下划线';
    if (!/^.{6,20}$/.test(password)) nextFieldErrors.password = '密码长度需为6-20位';
    if (password !== confirmPassword) nextFieldErrors.confirmPassword = '两次输入的密码不一致';
    if (!realName.trim() || realName.trim().length > 50) nextFieldErrors.realName = '请输入真实姓名，且不能超过50个字符';
    if (age === '' || Number(age) < 0 || Number(age) > 150) nextFieldErrors.age = '年龄必须在0-150之间';
    setFieldErrors(nextFieldErrors);
    if (errors.length > 0) {
      return;
    }

    // 验证用户名
    if (!username.trim() || username.trim().length < 4) {
      usernameRef.current.setCustomValidity('用户名不能少于4个字符');
      usernameRef.current.reportValidity();
      usernameRef.current.focus();
      return;
    } else {
      usernameRef.current.setCustomValidity('');
    }

    // 验证密码
    if (!password || password.length < 6) {
      passwordRef.current.setCustomValidity('密码不能少于6个字符');
      passwordRef.current.reportValidity();
      passwordRef.current.focus();
      return;
    } else {
      passwordRef.current.setCustomValidity('');
    }

    // 验证确认密码
    if (password !== confirmPassword) {
      confirmPasswordRef.current.setCustomValidity('两次输入的密码不一致');
      confirmPasswordRef.current.reportValidity();
      confirmPasswordRef.current.focus();
      return;
    } else {
      confirmPasswordRef.current.setCustomValidity('');
    }

    // 验证真实姓名
    if (!realName.trim()) {
      realNameRef.current.setCustomValidity('请输入真实姓名');
      realNameRef.current.reportValidity();
      realNameRef.current.focus();
      return;
    } else {
      realNameRef.current.setCustomValidity('');
    }

    // 验证年龄
    if (!age || parseInt(age) < 0 || parseInt(age) > 150) {
      ageRef.current.setCustomValidity('请输入有效的年龄（1-150）');
      ageRef.current.reportValidity();
      ageRef.current.focus();
      return;
    } else {
      ageRef.current.setCustomValidity('');
    }

    setIsLoading(true);

    try {
      const response = await fetch('/api/v1/user/register', {
        method: 'POST',
        credentials: 'include',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          username: username.trim(),
          password: password,
          realName: realName.trim(),
          age: parseInt(age),
          role: 'elder',
        }),
      });

      const data = await response.json();

      if (response.ok && data.code === 200) {
        // 注册成功后自动调用登录接口
        const loginResponse = await fetch('/api/v1/user/login', {
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

        const loginData = await loginResponse.json();

        if (loginResponse.ok && loginData.code === 200) {
          // 登录成功，保存JWT token（用户信息不存localStorage）
          const loginResult = loginData.data;
          if (loginResult.token) {
            saveToken(loginResult.token);
          }
          const userData = {
            ...loginResult,
            realName: realName.trim(),
            age: parseInt(age)
          };
          delete userData.token;
          onRegister(userData);
        } else {
          // 登录失败，但注册成功，提示用户手动登录
          setError('注册成功，请手动登录');
        }
      } else {
        setError(data.message || '注册失败，请重试');
      }
    } catch (err) {
      console.error('注册请求失败:', err);
      setError('网络连接失败，请稍后重试');
    } finally {
      setIsLoading(false);
    }
  };

  const handleLogin = () => {
    setUsername('');
    setPassword('');
    setConfirmPassword('');
    setRealName('');
    setAge('');
    onRegister(null);
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
        maxWidth: '580px'
      }}>
        <div style={{ textAlign: 'center', marginBottom: '40px' }}>
          <div style={{ fontSize: '80px', marginBottom: '20px', filter: 'drop-shadow(2px 4px 8px rgba(0,0,0,0.1))' }}>👴</div>
          <h1 style={{ fontSize: '36px', fontWeight: '800', color: '#4A90E2', marginBottom: '8px' }}>创建账号</h1>
          <p style={{ fontSize: '18px', color: '#6B6B6B', marginTop: '8px' }}>请选择身份并填写基本信息</p>
        </div>

        <form onSubmit={handleSubmit} noValidate>
          {/* 角色选择 */}
          <div style={{ display: 'none' }} aria-hidden="true">
            <label style={{
              fontSize: '20px',
              fontWeight: '600',
              marginBottom: '12px',
              display: 'block',
              color: '#3D3D3D'
            }}>
              <span style={{ color: '#E74C3C', marginRight: '4px' }}>*</span>
              我是
            </label>
            <div style={{ display: 'none' }} aria-hidden="true">
              <div
                onClick={() => setRole('elder')}
                style={{
                  flex: 1,
                  padding: '20px',
                  borderRadius: '16px',
                  border: role === 'elder' ? '3px solid #4A90E2' : '3px solid #F0EBE3',
                  background: role === 'elder' ? '#EBF2FC' : '#FAF7F2',
                  cursor: 'pointer',
                  textAlign: 'center',
                  transition: 'all 0.3s ease'
                }}
              >
                <svg width="40" height="40" viewBox="0 0 24 24" fill="none" stroke={role === 'elder' ? '#4A90E2' : '#999'} strokeWidth="1.5" style={{ marginBottom: '8px' }}>
                  <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/>
                  <circle cx="12" cy="7" r="4"/>
                </svg>
                <div style={{ fontSize: '18px', fontWeight: '600', color: role === 'elder' ? '#4A90E2' : '#666' }}>老人</div>
                <div style={{ fontSize: '13px', color: '#999', marginTop: '4px' }}>管理我的健康与用药</div>
              </div>
              <div
                onClick={() => setRole('family')}
                style={{
                  flex: 1,
                  padding: '20px',
                  borderRadius: '16px',
                  border: role === 'family' ? '3px solid #4A90E2' : '3px solid #F0EBE3',
                  background: role === 'family' ? '#EBF2FC' : '#FAF7F2',
                  cursor: 'pointer',
                  textAlign: 'center',
                  transition: 'all 0.3s ease'
                }}
              >
                <svg width="40" height="40" viewBox="0 0 24 24" fill="none" stroke={role === 'family' ? '#4A90E2' : '#999'} strokeWidth="1.5" style={{ marginBottom: '8px' }}>
                  <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/>
                  <circle cx="9" cy="7" r="4"/>
                  <path d="M23 21v-2a4 4 0 0 0-3-3.87"/>
                  <path d="M16 3.13a4 4 0 0 1 0 7.75"/>
                </svg>
                <div style={{ fontSize: '18px', fontWeight: '600', color: role === 'family' ? '#4A90E2' : '#666' }}>家属</div>
                <div style={{ fontSize: '13px', color: '#999', marginTop: '4px' }}>关注老人健康状态</div>
              </div>
            </div>
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
              用户名（用于登录）
            </label>
            <input
              ref={usernameRef}
              type="text"
              value={username}
              onChange={(e) => {
                setUsername(e.target.value);
                // 清除错误状态
                if (e.target.value.trim().length >= 4) {
                  usernameRef.current.setCustomValidity('');
                }
              }}
              placeholder="请输入用户名（至少4个字符）"
              required
              minLength={4}
              maxLength={20}
              pattern="[A-Za-z0-9_]+"
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
            {fieldErrors.username && <div style={{ color: '#E74C3C', marginTop: '8px', fontSize: '15px' }}>{fieldErrors.username}</div>}
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
            <div style={{ position: 'relative' }}>
              <input
                ref={passwordRef}
                type={showPassword ? "text" : "password"}
                value={password}
                onChange={(e) => {
                  setPassword(e.target.value);
                  // 清除错误状态
                  if (e.target.value.length >= 6) {
                    passwordRef.current.setCustomValidity('');
                  }
                }}
                placeholder="请输入密码（至少6个字符）"
                required
                minLength={6}
                maxLength={20}
                style={{
                  width: '100%',
                  padding: '18px 60px 18px 24px',
                  fontSize: '20px',
                  border: '3px solid #F0EBE3',
                  borderRadius: '16px',
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
            {fieldErrors.password && <div style={{ color: '#E74C3C', marginTop: '8px', fontSize: '15px' }}>{fieldErrors.password}</div>}
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
            <div style={{ position: 'relative' }}>
              <input
                ref={confirmPasswordRef}
                type={showConfirmPassword ? "text" : "password"}
                value={confirmPassword}
                onChange={(e) => {
                  setConfirmPassword(e.target.value);
                  // 清除错误状态
                  if (e.target.value === password && e.target.value.length > 0) {
                    confirmPasswordRef.current.setCustomValidity('');
                  }
                }}
                placeholder="请再次输入密码"
                required
                style={{
                  width: '100%',
                  padding: '18px 60px 18px 24px',
                  fontSize: '20px',
                  border: '3px solid #F0EBE3',
                  borderRadius: '16px',
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
              />
              <button
                type="button"
                onClick={() => setShowConfirmPassword(!showConfirmPassword)}
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
                {showConfirmPassword ? (
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
            {fieldErrors.confirmPassword && <div style={{ color: '#E74C3C', marginTop: '8px', fontSize: '15px' }}>{fieldErrors.confirmPassword}</div>}
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
              ref={realNameRef}
              type="text"
              value={realName}
              onChange={(e) => {
                setRealName(e.target.value);
                // 清除错误状态
                if (e.target.value.trim().length > 0) {
                  realNameRef.current.setCustomValidity('');
                }
              }}
              placeholder="请输入老人姓名"
              required
              maxLength={50}
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
            {fieldErrors.realName && <div style={{ color: '#E74C3C', marginTop: '8px', fontSize: '15px' }}>{fieldErrors.realName}</div>}
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
              ref={ageRef}
              type="number"
              value={age}
              onChange={(e) => {
                setAge(e.target.value);
                // 清除错误状态
                const val = parseInt(e.target.value);
                if (val > 0 && val <= 150) {
                  ageRef.current.setCustomValidity('');
                }
              }}
              placeholder="请输入年龄"
              required
              min="1"
              max="150"
              inputMode="numeric"
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
            {fieldErrors.age && <div style={{ color: '#E74C3C', marginTop: '8px', fontSize: '15px' }}>{fieldErrors.age}</div>}
          </div>

          {/* 家属角色时显示联系电话 */}
          {role === 'family' && (
            <div style={{ marginBottom: '24px' }}>
              <label style={{
                fontSize: '20px',
                fontWeight: '600',
                marginBottom: '12px',
                display: 'block',
                color: '#3D3D3D'
              }}>
                <span style={{ color: '#E74C3C', marginRight: '4px' }}>*</span>
                联系电话
              </label>
              <input
                type="tel"
                value={phone}
                onChange={(e) => setPhone(e.target.value)}
                placeholder="请输入联系电话"
                required
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
          )}

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

          {error && (
            <div style={{
              marginTop: '16px',
              marginBottom: '16px',
              background: '#FFEBEE',
              color: '#E74C3C',
              padding: '16px 20px',
              borderRadius: '12px',
              fontSize: '16px',
              fontWeight: '600',
              textAlign: 'center'
            }}>
              ⚠️ {error}
            </div>
          )}

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
