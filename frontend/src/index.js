import React from 'react';
import ReactDOM from 'react-dom/client';
import App from './App';
import { ToastProvider } from './components/Toast';

const root = ReactDOM.createRoot(document.getElementById('root'));
root.render(
  <React.StrictMode>
    <ToastProvider>
      <App />
    </ToastProvider>
  </React.StrictMode>
);
