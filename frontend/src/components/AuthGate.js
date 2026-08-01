import React from 'react';
import Login from './Login';
import Register from './Register';
import GuardianLogin from './guardian/GuardianLogin';

export default function AuthGate({ mode, showRegister, onLogin, onRegister, onShowRegister,
  onSwitchToGuardian, onSwitchToElder }) {
  if (mode === 'guardian') {
    return <GuardianLogin onLogin={onLogin} onSwitchToElder={onSwitchToElder} />;
  }
  if (showRegister) {
    return <Register onRegister={onRegister} />;
  }
  return <Login onLogin={onLogin} onShowRegister={onShowRegister}
    onSwitchToGuardian={onSwitchToGuardian} />;
}
