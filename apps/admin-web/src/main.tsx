import React from 'react';
import { createRoot } from 'react-dom/client';
import { BrowserRouter } from 'react-router-dom';
import { AuthProvider } from './auth/AuthContext';
import { App } from './app/App';
import { Toasts } from './components/ui';
import './styles/global.css';

createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <AuthProvider>
      <BrowserRouter>
        <App />
        <Toasts />
      </BrowserRouter>
    </AuthProvider>
  </React.StrictMode>,
);
