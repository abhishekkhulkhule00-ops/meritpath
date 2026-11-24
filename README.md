# MeritPath — Admission Counselling & College Selection Platform

MeritPath is a production-oriented Android (Kotlin + Jetpack Compose) admission counselling platform paired with a Node.js + TypeScript Razorpay verification backend (`/backend`) and a React + TypeScript Admin Web Panel (`/admin-web`).

## Architecture & Components

- **Android App (`/app`)**: Kotlin, Jetpack Compose, Material Design 3, Navigation, DataStore preferences, Credential Manager Google Sign-In, and Cloud Firestore.
  - **Student App**: Onboarding, Google Sign-In, Academic Profile & Completion Meter, Course Selection, College Explorer (search, filters, fee breakdown, historical year/round/category cutoffs with Safe/Moderate/Ambitious guidance & `Data Not Available` guardrails), 3-College Side-by-Side Comparison, Shortlist with Notes, Private Preference List Builder (order & submit lock), Package & Combo Discount Calculator + Promo Codes, Verified Payment Checkout & Receipts, Counselling Appointments, Document Checklist, Notifications Center, FAQ/Support, English/Marathi toggle, Light/Dark themes, and `FLAG_SECURE` screen protection.
  - **In-App Admin & Counsellor Portal**: Adaptive drawer portal for authorized staff (`SUPER_ADMIN`, `ADMIN`, `COUNSELLOR`, `DATA_MANAGER`) covering Analytics Dashboard, Student Management, Course CRUD, College + CSV Bulk Import/Export, Packages & Promos CRUD, Payments & Refunds, Private Preference List Review, Counselling Appointments, Broadcast Alerts, CSV Reports, and RBAC Settings + Audit Logs.
- **Verification Backend (`/backend`)**: Node.js + TypeScript + Express + Firebase Admin SDK + Razorpay SDK. Handles server-side pricing validation, Razorpay order creation, HMAC-SHA256 payment signature verification (`crypto.timingSafeEqual`), raw-body webhook signature verification, and atomic Firestore `payments` + `student_packages` activation.
- **Admin Web Panel (`/admin-web`)**: React + TypeScript + Vite dashboard authenticated via Firebase Auth Google Sign-In against the named Firestore database (`ai-studio-android-meritpat-2bce691d-4c89-4d71-a0e0-e05937575787`).
- **Database Security Rules (`/firestore.rules` & `/firestore.test.js`)**: Zero-trust Firestore rules enforcing strict student PII isolation, private preference-list access (only owning student and authorized Counsellor/Admin), atomic payment-to-package entitlement verification, and staff RBAC.

## Firebase Configuration

1. **Provisioned Project**: `gen-lang-client-0126130709`
2. **Named Firestore Database**: `ai-studio-android-meritpat-2bce691d-4c89-4d71-a0e0-e05937575787` (configured in `app/src/main/res/values/firebase_applet_config.xml`).
3. **Bootstrapped Super Admin**: `abhishekkhulkhule@gmail.com` (`email_verified == true`). Additional staff roles (`ADMIN`, `COUNSELLOR`, `DATA_MANAGER`) can be managed in `admins/{uid}` via the RBAC Settings module.
4. **Optional Additional Firebase Providers**: If you later want Email/Password, Mobile Phone OTP, Firebase Storage document uploads, or Firebase Cloud Messaging push campaigns, enable those providers and SDKs in the Firebase Console and deploy matching `storage.rules` / FCM cloud triggers.

## Razorpay Setup (Sandbox vs Live Mode)

- **Never** embed `RAZORPAY_KEY_SECRET` or `RAZORPAY_WEBHOOK_SECRET` inside the Android APK or `/admin-web` client bundle.
- **Sandbox Mode (Default)**: Out of the box, the Android checkout executes a deterministic **Verified Sandbox Mode** verification transaction and records an atomic `payments` + `student_packages` activation batch.
- **Live Mode**:
  1. Deploy `/backend`:
     ```bash
     cd backend
     cp .env.example .env
     # Set RAZORPAY_KEY_ID, RAZORPAY_KEY_SECRET, RAZORPAY_WEBHOOK_SECRET, GOOGLE_APPLICATION_CREDENTIALS
     npm install
     npm run build
     npm start
     ```
  2. Configure your Razorpay Webhook URL to `https://<your-backend-host>/api/payments/webhook` for `payment.captured`.
  3. In the MeritPath Admin Portal -> **Settings**, set **Backend URL** to `https://<your-backend-host>` and switch **Payment Mode** to `LIVE`.

## Building Android APK & Play Store AAB

> Note: Keep keystore secrets outside version control. Configure `KEYSTORE_PATH`, `STORE_PASSWORD`, and `KEY_PASSWORD` in your CI/environment before release builds.

```bash
# Run unit tests & Robolectric tests
gradle :app:testDebugUnitTest

# Build Debug APK
gradle :app:assembleDebug

# Build Signed Release APK
gradle :app:assembleRelease

# Build Google Play Store Android App Bundle (.aab)
gradle :app:bundleRelease
```

## Admin Web Panel Setup

```bash
cd admin-web
cp .env.example .env
# Populate VITE_FIREBASE_API_KEY and VITE_BACKEND_URL
npm install
npm run build
```

## Verification & Testing Checklist

1. **Firestore Security Rules Emulator Tests**:
   ```bash
   FIRESTORE_EMULATOR_HOST="127.0.0.1:8085" node --test firestore.test.js
   ```
2. **Android Unit & Robolectric Tests**:
   ```bash
   gradle :app:testDebugUnitTest
   ```
3. **End-to-End Flow Verification**:
   - Sign in with Google on a student account -> complete academic profile -> select course -> explore colleges & cutoffs.
   - Sign in as `abhishekkhulkhule@gmail.com` -> open Admin Portal -> **Seed Starter Catalog** -> review colleges, packages, promos, and analytics.
   - Verify private student preference lists are hidden from other students and visible to Counsellors/Admins after `SUBMITTED` lock.
   - Complete sandbox/live checkout and confirm paid features unlock only after verified payment activation.
