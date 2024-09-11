import { registerWithEmail, registerWithGoogle } from './firebase-auth.mjs';

document.getElementById('signup-btn').addEventListener('click', () => {
  const email = document.getElementById('signup-email').value;
  const password = document.getElementById('signup-password').value;
  registerWithEmail(email, password);
});

document.getElementById('google-signin-btn').addEventListener('click', () => {
  registerWithGoogle();
});

