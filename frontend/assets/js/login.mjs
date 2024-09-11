import { loginWithEmail, loginWithGoogle } from './firebase-auth.mjs';

document.getElementById('login-btn').addEventListener('click', () => {
  const email = document.getElementById('login-email').value;
  const password = document.getElementById('login-password').value;
  loginWithEmail(email, password);
});

document.getElementById('google-login-btn').addEventListener('click', () => {
  loginWithGoogle();
});

