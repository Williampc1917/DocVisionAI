import { auth } from './firebase-init.mjs';
import { signOut } from "https://www.gstatic.com/firebasejs/10.12.2/firebase-auth.js";

// Function to sign out the user
export function signOutUser() {
  console.log("Logout button clicked");

  signOut(auth).then(() => {
    console.log("User signed out successfully");
    window.location.href = 'login.html'; // Redirect after sign out
  }).catch((error) => {
    console.error("Error signing out:", error);
    alert("Error signing out. Please try again.");
  });
}

// Check if the button exists and attach event listener
function attachLogoutEventListener() {
  const logoutButton = document.getElementById('logoutButton');
  if (logoutButton) {
    logoutButton.addEventListener('click', signOutUser); // Attach the click event to trigger signOutUser
    console.log('Logout button event listener attached');
  } else {
    console.log('Logout button not found, trying again...');
    setTimeout(attachLogoutEventListener, 500); // Try again after 500ms
  }
}

// Start the process after DOM is fully loaded
document.addEventListener('DOMContentLoaded', attachLogoutEventListener);