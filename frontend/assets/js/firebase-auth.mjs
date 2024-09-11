import { auth } from './firebase-init.mjs';
import { createUserWithEmailAndPassword, signInWithEmailAndPassword, signInWithPopup, GoogleAuthProvider } from "https://www.gstatic.com/firebasejs/10.12.2/firebase-auth.js";

// Helper function to check if the user has a profile
function checkUserProfile(user) {
  return fetch(`https://spring-boot-backend-dot-reference-node-426102-r4.uk.r.appspot.com/api/user/checkUser/${user.uid}`)
    .then(response => {
      if (!response.ok) {
        return response.text().then(text => { throw new Error(text); });
      }
      return response.json();
    })
    .then(data => {
      if (data.exists) {
        window.location.href = 'index-dash.html'; // Redirect to index page if profile exists
      } else {
        window.location.href = 'profile-setup.html'; // Redirect to profile setup page if profile does not exist
      }
    })
    .catch(error => {
      console.error('Error checking user profile:', error);
      alert('Error checking user profile: ' + error.message);
    });
}

// Register with Email and Password
export function registerWithEmail(email, password) {
  return createUserWithEmailAndPassword(auth, email, password)
    .then((userCredential) => {
      const user = userCredential.user;
      console.log('Registered:', user);
      alert('Registration successful');
      window.location.href = 'login.html'; // Redirect to login page
    })
    .catch((error) => {
      const errorCode = error.code;
      const errorMessage = error.message;
      console.error('Error registering:', errorCode, errorMessage);
      alert('Registration failed: ' + errorMessage);
    });
}

// Register with Google
export function registerWithGoogle() {
  const provider = new GoogleAuthProvider();
  return signInWithPopup(auth, provider)
    .then((result) => {
      const user = result.user;
      console.log('Signed in with Google:', user);
      alert('Registration with Google successful');
      window.location.href = 'login.html'; // Redirect to login page
    })
    .catch((error) => {
      const errorCode = error.code;
      const errorMessage = error.message;
      console.error('Error signing in with Google:', errorCode, errorMessage);
      alert('Google registration failed: ' + errorMessage);
    });
}

// Login with Email and Password
export function loginWithEmail(email, password) {
  return signInWithEmailAndPassword(auth, email, password)
    .then((userCredential) => {
      const user = userCredential.user;
      console.log('Logged in:', user);
      alert('Login successful');
      return checkUserProfile(user); // Check if the user has a profile set up
    })
    .catch((error) => {
      const errorCode = error.code;
      const errorMessage = error.message;
      console.error('Error logging in:', errorCode, errorMessage);
      alert('Login failed: ' + errorMessage);
    });
}

// Login with Google
export function loginWithGoogle() {
  const provider = new GoogleAuthProvider();
  return signInWithPopup(auth, provider)
    .then((result) => {
      const user = result.user;
      console.log('Signed in with Google:', user);
      alert('Login with Google successful');
      return checkUserProfile(user); // Check if the user has a profile set up
    })
    .catch((error) => {
      const errorCode = error.code;
      const errorMessage = error.message;
      console.error('Error signing in with Google:', errorCode, errorMessage);
      alert('Google login failed: ' + errorMessage);
    });
}
