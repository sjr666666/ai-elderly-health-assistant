import React, { useRef, useState } from 'react';
import { saveToken } from '../../utils/guardianApi';
import { AUTH_RULES } from '../../utils/authValidation';
import './guardian.css';

const initialForm = {
  username: '',
  password: '',
  confirmPassword: '',
  realName: '',
  age: '',
  phone: '',
};

function GuardianRegister({ onRegister, onBackToLogin }) {
  const [form, setForm] = useState(initialForm);
  const [showPassword, setShowPassword] = useState(false);
  const [showConfirmPassword, setShowConfirmPassword] = useState(false);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState('');
  const [fieldErrors, setFieldErrors] = useState({});
  const refs = {
    username: useRef(null),
    password: useRef(null),
    confirmPassword: useRef(null),
    realName: useRef(null),
    age: useRef(null),
    phone: useRef(null),
  };

  const update = (field) => (event) => {
    setForm((current) => ({ ...current, [field]: event.target.value }));
    setError('');
    setFieldErrors((current) => ({ ...current, [field]: '' }));
  };

  const validate = () => {
    const errors = {};
    if (!AUTH_RULES.username.test(form.username.trim())) errors.username = '用户名需为4-20位字母、数字或下划线';
    if (!AUTH_RULES.password.test(form.password)) errors.password = '密码长度需为6-20位';
    if (form.password !== form.confirmPassword) errors.confirmPassword = '两次输入的密码不一致';
    if (!form.realName.trim() || form.realName.trim().length > 50) errors.realName = '请输入真实姓名，且不能超过50个字符';
    if (form.age === '' || Number(form.age) < 0 || Number(form.age) > 150) errors.age = '年龄必须在0-150之间';
    if (!AUTH_RULES.phone.test(form.phone.trim())) errors.phone = '请输入有效的手机号';
    setFieldErrors(errors);
    const firstInvalid = Object.keys(errors)[0];
    if (firstInvalid) refs[firstInvalid].current.focus();
    return Object.keys(errors).length === 0;
  };

  const handleSubmit = async (event) => {
    event.preventDefault();
    setError('');
    if (!validate()) return;
    setIsLoading(true);
    try {
      const registerResponse = await fetch('/api/v1/user/register', {
        method: 'POST',
        credentials: 'include',
        headers: { 'Content-Type': 'application/json', Accept: 'application/json' },
        body: JSON.stringify({
          username: form.username.trim(),
          password: form.password,
          realName: form.realName.trim(),
          age: Number(form.age),
          phone: form.phone.trim(),
          role: 'family',
        }),
      });
      const registerData = await registerResponse.json();
      if (!registerResponse.ok || registerData.code !== 200) {
        setError(registerData.message || '注册失败，请重试');
        return;
      }

      const loginResponse = await fetch('/api/v1/user/login', {
        method: 'POST',
        credentials: 'include',
        headers: { 'Content-Type': 'application/json', Accept: 'application/json' },
        body: JSON.stringify({ username: form.username.trim(), password: form.password }),
      });
      const loginData = await loginResponse.json();
      if (!loginResponse.ok || loginData.code !== 200 || loginData.data?.role !== 'family') {
        setError('注册成功，请返回登录');
        return;
      }

      const userData = { ...loginData.data };
      if (userData.token) saveToken(userData.token);
      delete userData.token;
      onRegister(userData);
    } catch (requestError) {
      console.error('家属注册失败:', requestError);
      setError('网络连接失败，请稍后重试');
    } finally {
      setIsLoading(false);
    }
  };

  const inputStyle = (field) => ({
    width: '100%',
    padding: '14px 16px',
    border: `2px solid ${fieldErrors[field] ? '#E74C3C' : '#e6e9ef'}`,
    borderRadius: '10px',
    fontSize: '16px',
    boxSizing: 'border-box',
  });

  const fieldError = (field) => fieldErrors[field] && <div className="g-field-error">{fieldErrors[field]}</div>;

  return (
    <div className="g-login">
      <div className="g-login-card g-register-card">
        <div className="g-login-header">
          <div className="g-login-logo">👨‍👩‍👧</div>
          <h1>家属注册</h1>
          <p>创建家属账号，开始守护家人</p>
        </div>
        <form onSubmit={handleSubmit} noValidate>
          <div className="g-form-group">
            <label htmlFor="guardian-register-username">用户名</label>
            <input ref={refs.username} id="guardian-register-username" value={form.username} onChange={update('username')} autoComplete="username" style={inputStyle('username')} />
            {fieldError('username')}
          </div>
          <div className="g-form-group">
            <label htmlFor="guardian-register-password">密码</label>
            <div className="g-register-input-wrap">
              <input ref={refs.password} id="guardian-register-password" type={showPassword ? 'text' : 'password'} value={form.password} onChange={update('password')} autoComplete="new-password" style={inputStyle('password')} />
              <button type="button" className="g-pwd-toggle" onClick={() => setShowPassword((value) => !value)}>{showPassword ? '隐藏' : '显示'}</button>
            </div>
            {fieldError('password')}
          </div>
          <div className="g-form-group">
            <label htmlFor="guardian-register-confirm-password">确认密码</label>
            <div className="g-register-input-wrap">
              <input ref={refs.confirmPassword} id="guardian-register-confirm-password" type={showConfirmPassword ? 'text' : 'password'} value={form.confirmPassword} onChange={update('confirmPassword')} autoComplete="new-password" style={inputStyle('confirmPassword')} />
              <button type="button" className="g-pwd-toggle" onClick={() => setShowConfirmPassword((value) => !value)}>{showConfirmPassword ? '隐藏' : '显示'}</button>
            </div>
            {fieldError('confirmPassword')}
          </div>
          <div className="g-form-group">
            <label htmlFor="guardian-register-name">真实姓名</label>
            <input ref={refs.realName} id="guardian-register-name" value={form.realName} onChange={update('realName')} style={inputStyle('realName')} />
            {fieldError('realName')}
          </div>
          <div className="g-form-group">
            <label htmlFor="guardian-register-age">年龄</label>
            <input ref={refs.age} id="guardian-register-age" type="number" value={form.age} onChange={update('age')} style={inputStyle('age')} />
            {fieldError('age')}
          </div>
          <div className="g-form-group">
            <label htmlFor="guardian-register-phone">手机号</label>
            <input ref={refs.phone} id="guardian-register-phone" type="tel" value={form.phone} onChange={update('phone')} autoComplete="tel" style={inputStyle('phone')} />
            {fieldError('phone')}
          </div>
          {error && <div className="g-error-tip" role="alert">{error}</div>}
          <button type="submit" className="g-btn g-btn-primary g-login-submit" disabled={isLoading}>{isLoading ? '注册中...' : '完成注册'}</button>
        </form>
        <div className="g-login-switch"><button type="button" onClick={onBackToLogin}>返回家属端登录</button></div>
      </div>
    </div>
  );
}

export default GuardianRegister;
