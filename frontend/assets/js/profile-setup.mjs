import { auth } from './firebase-init.mjs';

document.getElementById('profile-setup-form').addEventListener('submit', function (e) {
  e.preventDefault();

  const user = auth.currentUser;
  if (user) {
    const userInfo = {
      userId: user.uid,
      fullName: document.getElementById('fullName').value,
      jobTitle: document.getElementById('jobTitle').value,
      specialty: document.getElementById('specialty').value,
      institution: document.getElementById('institution').value,
      licenseNumber: Number(document.getElementById('licenseNumber').value), 
      reportTemplate: document.getElementById('reportTemplate').value
    };

    console.log('Submitting user info:', userInfo); // Debugging information

    fetch('https://spring-boot-backend-dot-reference-node-426102-r4.uk.r.appspot.com/api/user/saveUser', { // Ensure this URL is correct
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify(userInfo)
    })
    .then(response => {
      if (!response.ok) {
        return response.text().then(text => { throw new Error(text); });
      }
      return response.json();
    })
    .then(data => {
      console.log('User info saved successfully:', data);
      window.location.href = 'index-dash.html';
    })
    .catch((error) => {
      console.error('Error saving user info:', error);
      alert('Error saving user info: ' + error.message);
    });
  } else {
    console.error('No user is signed in.');
    alert('No user is signed in.');
  }
});
