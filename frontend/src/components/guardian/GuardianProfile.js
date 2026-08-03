import React, { useState, useEffect } from 'react';
import { useToast } from '../Toast';
import { getToken } from '../../utils/guardianApi';

function GuardianProfile({ user, onLogout }) {
  const { showToast } = useToast();
  const [profile, setProfile] = useState(null);
  const [loading, setLoading] = useState(true);

  // 修改电话
  const [showPhoneEdit, setShowPhoneEdit] = useState(false);
  const [newPhone, setNewPhone] = useState('');
  const [phoneSaving, setPhoneSaving] = useState(false);

  // 修改密码
  const [showPwdEdit, setShowPwdEdit] = useState(false);
  const [oldPwd, setOldPwd] = useState('');
  const [newPwd, setNewPwd] = useState('');
  const [confirmPwd, setConfirmPwd] = useState('');
  const [pwdSaving, setPwdSaving] = useState(false);

  useEffect(() => {
    loadProfile();
  }, [user]);

  const loadProfile = async () => {
    try {
      const res = await fetch(`/api/v1/user/profile`, {
        headers: { 'Authorization': `Bearer ${getToken()}` },
      });
      const data = await res.json();
      if (data.code === 200) {
        setProfile(data.data);
        setNewPhone(data.data.phone || '');
      }
    } catch (e) {
      console.error('加载个人信息失败', e);
    } finally {
      setLoading(false);
    }
  };

  const handleSavePhone = async () => {
    if (!newPhone.trim()) return;
    setPhoneSaving(true);
    try {
      const res = await fetch(`/api/v1/user/profile`, {
        method: 'PUT',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${getToken()}`,
        },
        body: JSON.stringify({ phone: newPhone.trim() }),
      });
      const data = await res.json();
      if (data.code === 200) {
        setProfile(prev => ({ ...prev, phone: newPhone.trim() }));
        setShowPhoneEdit(false);
        showToast('电话号码修改成功', 'success');
      } else {
        showToast(data.message || '修改失败', 'error');
      }
    } catch (e) {
      console.error('修改电话号码失败:', e);
      showToast('网络错误', 'error');
    }
  };

  const handleSavePwd = async () => {
    if (!oldPwd || !newPwd || !confirmPwd) {
      showToast('请填写完整', 'warning');
      return;
    }
    if (newPwd !== confirmPwd) {
      showToast('两次输入的新密码不一致', 'warning');
      return;
    }
    if (newPwd.length < 6) {
      showToast('新密码长度不能少于6位', 'warning');
      return;
    }
    setPwdSaving(true);
    try {
      const res = await fetch(`/api/v1/user/password`, {
        method: 'PUT',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${getToken()}`,
        },
        body: JSON.stringify({ oldPassword: oldPwd, newPassword: newPwd }),
      });
      const data = await res.json();
      if (data.code === 200) {
        showToast('密码修改成功，请重新登录', 'success');
        setOldPwd(''); setNewPwd(''); setConfirmPwd('');
        setShowPwdEdit(false);
        onLogout();
      } else {
        showToast(data.message || '修改失败', 'error');
      }
    } catch (e) {
      console.error('修改密码失败:', e);
      showToast('网络错误', 'error');
    } finally {
      setPwdSaving(false);
    }
  };

  if (loading) {
    return <div className="g-profile-loading">加载中...</div>;
  }

  return (
    <div className="g-profile">
      {/* 用户头像和名称 */}
      <div className="g-profile-header">
        <div className="g-profile-avatar">
          <svg viewBox="0 0 24 24" fill="white" width="32" height="32">
            <path d="M12 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm0 2c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z"/>
          </svg>
        </div>
        <div className="g-profile-name">{profile?.realName || user.realName}</div>
        <div className="g-profile-role">家属</div>
      </div>

      {/* 信息列表 */}
      <div className="g-profile-section">
        <div className="g-profile-item">
          <div className="g-profile-item-left">
            <svg viewBox="0 0 24 24" fill="#3A7BC8" width="18" height="18"><path d="M12 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm0 2c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z"/></svg>
            <span>用户名</span>
          </div>
          <span className="g-profile-value">{profile?.realName || user.username}</span>
        </div>

        {/* 联系电话 */}
        <div className="g-profile-item">
          <div className="g-profile-item-left">
            <svg viewBox="0 0 24 24" fill="#3A7BC8" width="18" height="18"><path d="M6.62 10.79c1.44 2.83 3.76 5.14 6.59 6.59l2.2-2.2c.27-.27.67-.36 1.02-.24 1.12.37 2.33.57 3.57.57.55 0 1 .45 1 1V20c0 .55-.45 1-1 1-9.39 0-17-7.61-17-17 0-.55.45-1 1-1h3.5c.55 0 1 .45 1 1 0 1.25.2 2.45.57 3.57.11.35.03.74-.25 1.02l-2.2 2.2z"/></svg>
            <span>联系电话</span>
          </div>
          {showPhoneEdit ? (
            <div className="g-profile-inline-edit">
              <input type="tel" value={newPhone} onChange={e => setNewPhone(e.target.value)}
                placeholder="输入新电话" />
              <button className="g-profile-btn-sm g-profile-btn-primary" onClick={handleSavePhone}
                disabled={phoneSaving}>{phoneSaving ? '...' : '保存'}</button>
              <button className="g-profile-btn-sm" onClick={() => { setShowPhoneEdit(false); setNewPhone(profile?.phone || ''); }}>取消</button>
            </div>
          ) : (
            <div className="g-profile-item-right">
              <span className="g-profile-value">{profile?.phone || '未设置'}</span>
              <button className="g-profile-edit-btn" onClick={() => setShowPhoneEdit(true)}>修改</button>
            </div>
          )}
        </div>

        {/* 修改密码 */}
        <div className="g-profile-item">
          <div className="g-profile-item-left">
            <svg viewBox="0 0 24 24" fill="#3A7BC8" width="18" height="18"><path d="M18 8h-1V6c0-2.76-2.24-5-5-5S7 3.24 7 6v2H6c-1.1 0-2 .9-2 2v10c0 1.1.9 2 2 2h12c1.1 0 2-.9 2-2V10c0-1.1-.9-2-2-2zm-6 9c-1.1 0-2-.9-2-2s.9-2 2-2 2 .9 2 2-.9 2-2 2zm3.1-9H8.9V6c0-1.71 1.39-3.1 3.1-3.1 1.71 0 3.1 1.39 3.1 3.1v2z"/></svg>
            <span>登录密码</span>
          </div>
          <button className="g-profile-edit-btn" onClick={() => setShowPwdEdit(!showPwdEdit)}>
            {showPwdEdit ? '收起' : '修改'}
          </button>
        </div>
        {showPwdEdit && (
          <div className="g-profile-pwd-form">
            <input type="password" value={oldPwd} onChange={e => setOldPwd(e.target.value)}
              placeholder="旧密码" />
            <input type="password" value={newPwd} onChange={e => setNewPwd(e.target.value)}
              placeholder="新密码（至少6位）" />
            <input type="password" value={confirmPwd} onChange={e => setConfirmPwd(e.target.value)}
              placeholder="确认新密码" />
            <button className="g-profile-btn-primary g-profile-btn-block" onClick={handleSavePwd}
              disabled={pwdSaving}>{pwdSaving ? '保存中...' : '确认修改'}</button>
          </div>
        )}
      </div>

      {/* 退出登录 */}
      <div className="g-profile-section" style={{ marginTop: '24px' }}>
        <button className="g-profile-logout" onClick={onLogout}>退出登录</button>
      </div>
    </div>
  );
}

export default GuardianProfile;
