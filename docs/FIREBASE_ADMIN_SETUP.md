# Firebase Admin + Phone Login setup

The app supports both Email/Password and Mobile OTP. Firebase must be configured once; code alone cannot turn Phone Authentication on.

## A. Anonymous login for Channel Follow/Comments
1. Firebase Console → Authentication → Sign-in method.
2. Enable **Anonymous**.
3. Save.

## B. Phone OTP for Admin
1. Authentication → Sign-in method → enable **Phone**.
2. Authentication → Settings → SMS region policy → allow **India**. New Firebase projects can have SMS regions disabled until a policy is configured.
3. Project Settings → Your apps → Android app `com.shiksharojgar.app`. Add SHA-1 and SHA-256 fingerprints.

### Debug APK
The GitHub `Build Shiksha Rojgar APK` workflow now runs `gradle signingReport` before building. Open the workflow log and copy the **debug** SHA-1 and SHA-256 into Firebase.

### Permanent Release APK
Use the permanent release SHA-1/SHA-256 documented in `docs/RELEASE_SIGNING.md`.

## C. Create the Admin account
Use Firebase Authentication to create an Email/Password account, or create a Phone account by signing in once with OTP. Then set the Firestore users/{uid} admin fields `admin=true` for that exact account.

The project includes `firebase/admin/set-admin.js`. It requires a Firebase service-account JSON and the account's email or phone. Never put the service-account JSON, private key, or passwords in GitHub.

After setting the claim, sign out and sign in again in the app so the refreshed ID token contains `admin=true`.

## D. If OTP shows “internal error”
The app now displays the Firebase error code and a setup hint. Common causes are:
- Phone provider is disabled.
- India is not allowed by SMS region policy.
- The APK's SHA-1/SHA-256 is missing.
- Too many SMS requests.

Do not use `setAppVerificationDisabledForTesting()` for real phone numbers.

## E. Deploy the counters/rules once
From the `firebase` folder, after installing Firebase CLI and selecting project `shiksha-rojgar`:

```bash
firebase use shiksha-rojgar
firebase deploy --only firestore:rules,functions
```

The Functions maintain follower, like, comment, post-view and post-share counters and periodically repair them from the underlying unique documents.

## F. Analytics meaning
Firebase Analytics automatically provides `first_open`, sessions and active-user metrics after the APK is opened. The app also records a unique first-use user and daily usage document for the Admin Analytics panel. A Blogger/website APK file download is not the same as an installed/opened app; the APK itself cannot retroactively count file downloads made before installation.
