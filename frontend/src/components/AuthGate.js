import React from 'react';
import Login from './Login';
import Register from './Register';
import GuardianLogin from './guardian/GuardianLogin';
import GuardianRegister from './guardian/GuardianRegister';

export default function AuthGate({ mode, showRegister, onLogin, onRegister, onShowRegister,
  onSwitchToGuardian, onSwitchToElder }) {
  if (mode === 'guardian') {
    if (showRegister) {
      return <GuardianRegister onRegister={onRegister} onBackToLogin={() => onRegister(null)} />;
    }
    return <GuardianLogin onLogin={onLogin} onShowRegister={onShowRegister} onSwitchToElder={onSwitchToElder} />;
  }
  if (showRegister) {
    return <Register onRegister={onRegister} />;
  }
  return <Login onLogin={onLogin} onShowRegister={onShowRegister}
    onSwitchToGuardian={onSwitchToGuardian} />;
}
