import React from 'react';
import ReactDOM from 'react-dom/client';
import App from './App';
import { ToastProvider } from './components/Toast';

/**
 * 移动端 100vh 修复
 * 移动浏览器地址栏/工具栏占据空间，导致 100vh > 可视区域。
 * 现代浏览器用 100dvh（Dynamic Viewport Height）原生解决；
 * 对不支持 dvh 的旧浏览器，通过 JS 计算 --vh 变量作为降级兜底。
 */
function setViewportHeight() {
  const vh = window.innerHeight * 0.01;
  document.documentElement.style.setProperty('--vh', `${vh}px`);
}
setViewportHeight();
window.addEventListener('resize', setViewportHeight);
window.addEventListener('orientationchange', setViewportHeight);

const root = ReactDOM.createRoot(document.getElementById('root'));
root.render(
  <React.StrictMode>
    <ToastProvider>
      <App />
    </ToastProvider>
  </React.StrictMode>
);
