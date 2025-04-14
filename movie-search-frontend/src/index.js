import React from 'react';
import { createRoot } from 'react-dom/client';
import './index.css';
import App from './App';
import { Auth0Provider } from '@auth0/auth0-react';

const root = createRoot(document.getElementById('root'));

root.render(
  <Auth0Provider
    domain="dev-opqvt1nsdwq040ox.us.auth0.com"
    clientId="XkQrjGwmkJYjgBXhXf49ZL2FIHMc7vkk"
    redirectUri={window.location.origin + '/callback'}
    audience="https://dev-opqvt1nsdwq040ox.us.auth0.com/api/v2/"
    scope="openid profile email read:history"
  >
    <App />
  </Auth0Provider>
);
