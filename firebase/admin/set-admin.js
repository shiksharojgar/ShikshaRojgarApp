// Run once with a Firebase service-account JSON:
// GOOGLE_APPLICATION_CREDENTIALS=./service-account.json ADMIN_EMAIL=you@example.com node set-admin.js
const admin = require('firebase-admin');
admin.initializeApp();
const email = process.env.ADMIN_EMAIL;
const phone = process.env.ADMIN_PHONE;
if (!email && !phone) throw new Error('Set ADMIN_EMAIL or ADMIN_PHONE');
(async()=>{
  const user = email ? await admin.auth().getUserByEmail(email) : await admin.auth().getUserByPhoneNumber(phone);
  await admin.auth().setCustomUserClaims(user.uid, { admin: true });
  await admin.firestore().collection('users').doc(user.uid).set({
    admin: true,
    isAdmin: true,
    role: 'admin'
  }, { merge: true });
  console.log(`Admin enabled for UID ${user.uid}. Auth claim + users/${user.uid} Firestore document updated.`);
  console.log('Sign out/in in the app after changing the Auth claim.');
  process.exit(0);
})().catch(e=>{console.error(e);process.exit(1);});
