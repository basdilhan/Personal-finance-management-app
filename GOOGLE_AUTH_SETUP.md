# Google Authentication Setup

This project now includes Firebase Auth + Google Sign-In + Firestore user profile saving.

## 1) Firebase Console setup

1. Create/select a Firebase project.
2. Add Android app with package name: `com.team.financeapp`.
3. Add SHA-1 and SHA-256 for your debug/release keystores.
4. Enable **Authentication -> Sign-in method -> Google**.
5. Enable **Firestore Database** (start in test mode for development if needed).
6. Download `google-services.json` and place it in:
   - `app/google-services.json`

## 2) Add Web client ID in app string

Open `app/src/main/res/values/strings.xml` and replace:

- `REPLACE_WITH_FIREBASE_WEB_CLIENT_ID`

Use the value from Firebase project settings (OAuth 2.0 Client IDs, Web client).

## 3) What the implementation does

- Email/password login and registration use Firebase Auth.
- Google sign-in is available on Login and Register screens.
- On successful sign-in, user profile data is saved/updated in Firestore:
  - Collection: `users`
  - Document ID: Firebase UID
  - Fields: `uid`, `name`, `email`, `photoUrl`, `updatedAt`
- Home screen auto-skips to Dashboard when a user session exists.
- Logout now signs out Firebase and Google session.

## 4) Quick verification checklist

1. Launch app, open Login screen.
2. Tap `Continue with Google`, choose an account.
3. Confirm navigation to Dashboard.
4. In Firestore, check `users/{uid}` document exists.
5. Logout and confirm app returns to Login.

