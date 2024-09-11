import { auth } from './firebase-init.mjs';
import { getFirestore, doc, getDoc } from "https://www.gstatic.com/firebasejs/10.12.2/firebase-firestore.js";

const db = getFirestore();

// Function to fetch and display user info
async function fetchAndDisplayUserInfo() {
  const user = auth.currentUser;

  if (user) {
    try {
      const userDocRef = doc(db, "Users", user.uid);
      const userDocSnap = await getDoc(userDocRef);

      if (userDocSnap.exists()) {
        const userData = userDocSnap.data();
        const userNameElement = document.getElementById('user-name');

        if (userNameElement) {
          userNameElement.textContent = userData.fullName;
        } else {
          console.error('User name element not found.');
        }
      } else {
        console.error('No such document!');
      }
    } catch (error) {
      console.error('Error fetching user info:', error);
    }
  } else {
    console.error('No user is signed in.');
  }
}

// Wait for the auth state to change before attempting to fetch user info
auth.onAuthStateChanged((user) => {
  if (user) {
    fetchAndDisplayUserInfo();
  } else {
    console.error('User is not authenticated.');
  }
});

// No need to call fetchAndDisplayUserInfo() on load anymore
// fetchAndDisplayUserInfo(); <-- Remove this line
