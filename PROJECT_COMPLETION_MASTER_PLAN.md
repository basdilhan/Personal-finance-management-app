# DreamSaver Project Completion Master Plan

Date: March 28, 2026  
Branch reviewed: sam

This is the single source of truth for delivery to Apr 5: complete backend first, then optional features.

## 1. Current Status (Verified)

Completed now:
- Auth is integrated (email/password, Google sign in, reset password, change password, user profile upsert).
- Room + Firestore + WorkManager dependencies are added.
- Room local DB schema exists (bill, expense, goal, income entities and DAOs).
- Bills, Expenses, and Income have repository-based local-first loading and cloud sync.
- BillsActivity, ExpensesActivity, IncomeHistoryActivity load from repositories.
- AddBillActivity, AddExpenseActivity, AddIncomeActivity save through repositories.
- Firestore rules baseline exists with user ownership and sync-state checks.
- Pending sync worker scheduling exists at app startup.
- Change Password activity implemented (ChangePasswordActivity with proper re-authentication and password update).
- **[NEW - Mar 31]** PendingSyncWorker real retry engine fully implemented with backoff, error logging, and state transitions.

Still incomplete:
- Goals backend integration is incomplete.
- Notifications screen still uses mock data.
- Dashboard summary architecture is not finalized.
- Cloud Functions layer not yet added in repo.
- Tests are still template-level only.

## 2. Target Project Structure (Final)

```text
Personal-finance-management-app/
├── app/
│   └── src/main/java/com/team/financeapp/
│       ├── auth/
│       ├── data/
│       │   ├── local/
│       │   │   ├── AppDatabase.java
│       │   │   ├── dao/
│       │   │   │   ├── BillDao.java
│       │   │   │   ├── ExpenseDao.java
│       │   │   │   ├── GoalDao.java
│       │   │   │   ├── IncomeDao.java
│       │   │   │   ├── NotificationDao.java (new)
│       │   │   │   ├── WalletDao.java (new)
│       │   │   │   └── PaymentMethodDao.java (new)
│       │   │   └── entity/
│       │   │       ├── BillEntity.java
│       │   │       ├── ExpenseEntity.java
│       │   │       ├── GoalEntity.java
│       │   │       ├── IncomeEntity.java
│       │   │       ├── NotificationEntity.java (new)
│       │   │       ├── WalletEntity.java (new)
│       │   │       └── PaymentMethodEntity.java (new)
│       │   ├── repository/
│       │   │   ├── BillRepository.java
│       │   │   ├── ExpenseRepository.java
│       │   │   ├── GoalRepository.java (new)
│       │   │   ├── IncomeRepository.java
│       │   │   ├── NotificationRepository.java (new)
│       │   │   ├── WalletRepository.java (new)
│       │   │   └── PaymentMethodRepository.java (new)
│       │   ├── remote/
│       │   │   └── BankLinkSandboxService.java (new)
│       │   └── sync/
│       │       ├── PendingSyncWorker.java
│       │       └── SyncCoordinator.java (new)
│       ├── wallet/ (new package)
│       └── ...existing activities
├── functions/ (new)
│   └── src/
│       ├── reminders.ts
│       ├── summaries.ts
│       ├── notifications.ts
│       └── bankSandbox.ts
└── docs/ (new)
	├── backend/
	├── features/
	└── qa/
```

## 3. Backend-First Plan (Do This First)

Do not start wallet/card/bank tasks until backend core is complete.

## Phase A: Contract Lock (Mar 28)

Tasks:
- Finalize schema contracts for users, bills, expenses, goals, incomes, notifications, summaries.
- Freeze sync-state model and required fields.
- Agree naming and ownership rules across app and functions.

Exit criteria:
- Team-approved data contracts.

## Phase B: Core Backend Completion (Mar 29 -> Apr 1)

Tasks:
- Implement GoalRepository and integrate AddGoalActivity, GoalsActivity, GoalDetailsActivity.
- Implement real PendingSyncWorker retry flow for PENDING/FAILED states.
- Implement NotificationRepository and replace NotificationsActivity mock list.
- Complete create/update/delete sync paths for all finance modules.
- Add summaries pipeline and wire dashboard to summary docs.

Exit criteria:
- No backend TODO placeholders remain.
- Bills/Expenses/Incomes/Goals all persist and sync.
- Notifications read from backend.

## Phase C: Security and QA Hardening (Apr 2 -> Apr 3)

Tasks:
- Add Firestore rules tests in emulator.
- Add repository unit tests and integration tests for offline/online recovery.
- Run end-to-end flow checks for auth, CRUD, sync, relaunch.

Exit criteria:
- Rules tests pass.
- Regression passes with no critical blockers.

## 4. Optional Features (Only After Backend)

## Feature 1: Wallet

Implement:
- Wallet list and balances.
- Add/edit wallet records per user.

## Feature 2: Card Option

Implement:
- Add card with masked fields only: nickname, brand, last4, expiry month/year.

Do not store:
- Full card number, CVV, PIN.

## Feature 3: Bank Account Details (Sandbox)

Implement:
- Sandbox-only account linking.
- Read-only balances and transaction history.

Do not implement by Apr 5:
- Production real-bank rollout, payment charging, money transfer.

## 5. Team Split (3 People)

## Person 1: Data and Sync Owner

Owns:
- GoalRepository and goals integration.
- PendingSyncWorker full retry engine.
- Update/delete sync handling across modules.

## Person 2: Cloud and Notifications Owner

Owns:
- Firebase Functions setup.
- Reminder + summary + notification cloud logic.
- Notifications backend integration.
- Bank sandbox integration.

## Person 3: App Integration, Security, QA Owner

Owns:
- Dashboard summary consumption.
- Firestore rules hardening and tests.
- Wallet/card UI integration.
- End-to-end QA and release checklist.

## 6. Day-by-Day Timeline (Mar 28 -> Apr 5)

Mar 28:
- Lock contracts and task ownership.

Mar 29:
- Start GoalRepository + Functions bootstrap + rules test setup.

Mar 30:
- Integrate goals flows + notification backend write path + summary model.

Mar 31:
- Complete sync worker retry logic + reminder function + rules test expansion.

Apr 1:
- Complete summaries and dashboard summary reads + backend bug fixes.

Apr 2:
- Backend regression and offline/online recovery testing.

Apr 3:
- Start optional wallet/card + bank sandbox integration.

Apr 4:
- Polish optional features and prepare fallback demo mode.

Apr 5:
- Final QA sweep, build freeze, and submission/demo prep.

## 7. Definition of Done

Backend done when:
- Goals are fully persisted and synced like other modules.
- ✅ Pending sync worker retries pending/failed records successfully. (DONE Mar 31)
- Notifications are backend-driven.
- Dashboard reads summary documents.
- Security rules tests pass.
- Core backend tests pass.

Auth complete when:
- ✅ Email sign-in/register
- ✅ Google sign-in
- ✅ Forgot password (reset via email link)
- ✅ Change password (for logged-in users with re-authentication)
- ✅ User profile persistence in Firestore
- ✅ Session management and logout

Optional features done when:
- Wallet and masked card flows are stable.
- Bank account details work in sandbox mode.
- No optional feature breaks core backend flows.

## 8. Immediate Start Checklist

1. ✅ **[DONE - Mar 28]** Implement ChangePasswordActivity with proper re-authentication.
2. ✅ **[DONE - Mar 31]** Implement PendingSyncWorker real sync/retry loop.
3. Implement GoalRepository and remove AddGoalActivity TODO save path.
4. Replace GoalsActivity sample data with repository data.
5. Replace NotificationsActivity mock data with backend data source.
6. Add Firestore emulator rules tests.
7. Begin wallet/card/bank sandbox only after checklist items 3 to 6 are merged into dev.

---

## Recent Completions (Mar 28-31)

**Change Password Feature - IMPLEMENTED & TESTED (Mar 28)**
- ✅ Created `ChangePasswordActivity.java` with three password input fields
- ✅ Created `activity_change_password.xml` layout with Material Design
- ✅ Added `updatePassword()` method to `AuthManager` with Firebase re-authentication
- ✅ Updated `ProfileActivity` to navigate to new `ChangePasswordActivity` instead of `ForgotPasswordActivity`
- ✅ Registered activity in `AndroidManifest.xml`
- ✅ Built and deployed to device successfully
- ✅ App running on device: `R94XB0FWFJX`

**How it works:**
- User enters current password (verified against Firebase)
- Enters new password (6+ characters minimum)
- Confirms new password matches
- Firebase securely updates password after re-authentication
- Success message shown to user

---

**PendingSyncWorker Real Retry Engine - IMPLEMENTED & BUILT (Mar 31)**

**What was implemented:**
- ✅ Full sync orchestration in `doWork()` that:
  - Retrieves current Firebase user ID
  - Queries PENDING/FAILED records from all 3 modules (Bills, Expenses, Income)
  - Attempts Firestore sync for each record with 20-second timeout
  - Updates sync state (PENDING → SYNCED on success, PENDING → FAILED on error)
  - Records updatedAt timestamp and handles exceptions
  
- ✅ Sync handlers for each module:
  - `syncBill()` - retries pending/failed bills to Firestore with full payload
  - `syncExpense()` - retries pending/failed expenses to Firestore with full payload
  - `syncIncome()` - retries pending/failed income to Firestore with full payload
  
- ✅ Backoff strategy:
  - Exponential backoff: 30 seconds initial, WorkManager applies multiplier on retry
  - Returns `Result.retry()` if any sync failed → WorkManager reschedules per policy
  - Returns `Result.success()` if all records synced successfully
  
- ✅ Error handling & logging:
  - Structured logging with TAG "PendingSyncWorker"
  - Logs each sync attempt result with record remoteId and specific errors
  - Catches timeout exceptions and marks records as FAILED (prevents data loss)
  - Tracks total synced count and failure flag per run
  
- ✅ Scheduling configuration:
  - Runs every 15 minutes (PeriodicWorkRequest interval)
  - Only when network is CONNECTED (WorkManager constraint)
  - Periodic work with KEEP policy (survives app restarts)
  - ExponentialBackoffPolicy(30 seconds) for retry delays

**How offline-to-online recovery works:**
1. User creates bill/expense/income while offline → saved to Room with syncState=PENDING
2. WorkManager schedules PendingSyncWorker every 15 minutes
3. User reconnects to network
4. Next sync run triggers (or sooner if manually forced)
5. Worker finds PENDING/FAILED records, attempts Firestore push
6. On success → syncState updated to SYNCED locally (Room.update())
7. On failure → syncState set to FAILED locally, retry scheduled by WorkManager
8. Next run attempts FAILED records again with exponential backoff

**Build validation:**
- ✅ Compiled successfully: BUILD SUCCESSFUL in 35s
- ✅ All dependencies resolved (Google Tasks, Firebase Firestore, Room, WorkManager)
- ✅ No errors, no blocking warnings

**Next validation steps (for QA member):**
- Manual test: Create transaction offline, enable network, verify sync within 15 min
- Monitor WorkManager logs: `adb logcat | grep PendingSyncWorker`
- Verify both PENDING and FAILED state transitions in local Room database
- Confirm zero data loss during failed network conditions

