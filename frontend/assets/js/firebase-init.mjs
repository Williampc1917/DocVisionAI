// Import the functions you need from the SDKs you need
import { initializeApp } from "https://www.gstatic.com/firebasejs/10.12.2/firebase-app.js";
import { getAnalytics } from "https://www.gstatic.com/firebasejs/10.12.2/firebase-analytics.js";
import { getAuth } from "https://www.gstatic.com/firebasejs/10.12.2/firebase-auth.js";
import { getFirestore } from "https://www.gstatic.com/firebasejs/10.12.2/firebase-firestore.js";

// Your web app's Firebase configuration
const firebaseConfig = {
  apiKey: "AIzaSyDqurV4wJWvSg1n6ShJXFD5NPohXaRQlnw",
  authDomain: "reference-node-426102-r4.firebaseapp.com",
  projectId: "reference-node-426102-r4",
  storageBucket: "reference-node-426102-r4.appspot.com",
  messagingSenderId: "695943809656",
  appId: "1:695943809656:web:8ffd79aa1840057de5fb8d",
  measurementId: "G-7XN9XLG7CD"
};

// Initialize Firebase
const app = initializeApp(firebaseConfig);
const analytics = getAnalytics(app);
const auth = getAuth(app);
const db = getFirestore(app);

// Export the initialized Firebase services
export { app, analytics, auth, db };


