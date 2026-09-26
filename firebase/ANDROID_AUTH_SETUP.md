# Shiksha Rojgar Firebase Authentication setup

For Channel Follow/Comments:
1. Firebase Console -> Authentication -> Sign-in method -> enable **Anonymous**.
2. For Admin mobile login -> enable **Phone**.
3. Add the release APK SHA-256 certificate fingerprint to the Android app in Firebase:
   `FC:42:F6:58:DD:C1:2E:73:F9:EF:CC:68:32:29:10:DB:78:BA:84:B6:A2:23:6E:D9:38:60:F7:AF:2B:0F:E8:0F`
4. Create the Admin user with Email/Password or Phone authentication, then set the Firebase users/{uid} document `admin=true` using the admin script.

The app does not impose a follower-count limit. Firebase service quotas/billing still apply.

## Build compatibility note
The Android Phone Authentication callback uses `FirebaseException` in
`onVerificationFailed(...)`, matching the current Firebase Android SDK API.
Do not change this callback parameter to `FirebaseAuthException`.
