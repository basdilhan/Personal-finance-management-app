# Personal Finance App - Flow and Backend Guide

Date: April 1, 2026
Branch context: sam

This document explains how the app works from user flow to backend flow, in simple terms.
It is focused on Room, Firebase, sync, and where each part is in the code.

## 1) What the app does

The app helps a user:
- Sign in / register
- Add and manage Expenses, Bills, Incomes, Goals
- See financial summary on Dashboard
- Work offline first (save locally), then sync to Firebase when internet is available

## 2) Screen flow (how user moves in app)

### Guest user flow
1. App opens to Home screen
2. Tap Get Started
3. Go to Login/Register
4. After successful auth, go to Dashboard

### Logged-in user flow
1. App opens
2. If session exists, Home auto-navigates to Dashboard
3. User can open:
- Expenses
- Bills
- Income History
- Goals
- Profile
- Notifications

### Main navigation files
- app/src/main/AndroidManifest.xml
- app/src/main/java/com/team/financeapp/HomeActivity.java
- app/src/main/java/com/team/financeapp/LoginActivity.java
- app/src/main/java/com/team/financeapp/RegisterActivity.java
- app/src/main/java/com/team/financeapp/DashboardActivity.java
- app/src/main/java/com/team/financeapp/BottomNavigationFragment.java

## 3) High-level backend architecture

The backend design is Local-First + Cloud Sync.

- Local database: Room (fast, offline-safe)
- Cloud database: Firebase Firestore
- Sync engine: WorkManager background worker

Flow idea:
1. Save/read data in Room first
2. Try syncing same data to Firestore
3. If cloud sync fails, keep local data and mark state as FAILED
4. Background worker retries FAILED/PENDING records when network is back

## 4) Room implementation (local database)

### Database root
- app/src/main/java/com/team/financeapp/data/local/AppDatabase.java

### Entities (tables)
- app/src/main/java/com/team/financeapp/data/local/entity/ExpenseEntity.java
- app/src/main/java/com/team/financeapp/data/local/entity/BillEntity.java
- app/src/main/java/com/team/financeapp/data/local/entity/IncomeEntity.java
- app/src/main/java/com/team/financeapp/data/local/entity/GoalEntity.java

### DAO interfaces
- app/src/main/java/com/team/financeapp/data/local/dao/ExpenseDao.java
- app/src/main/java/com/team/financeapp/data/local/dao/BillDao.java
- app/src/main/java/com/team/financeapp/data/local/dao/IncomeDao.java
- app/src/main/java/com/team/financeapp/data/local/dao/GoalDao.java

### Sync state constant
- app/src/main/java/com/team/financeapp/data/local/SyncState.java

Sync states used by records:
- PENDING: local save done, cloud pending
- SYNCED: local and cloud both updated
- FAILED: local save done, cloud failed (retry later)

## 5) Firebase implementation (cloud)

### Core auth + profile
- app/src/main/java/com/team/financeapp/auth/AuthManager.java

AuthManager handles:
- Email login/register
- Google sign-in
- Password reset email
- Change password (with re-authentication)
- User profile upsert to Firestore users collection

### Firestore collections used
- users
- expenses
- bills
- incomes
- goals
- notifications
- summaries (read-only in rules)

### Firebase security rules
- firestore.rules
- firebase.json
- firestore.indexes.json

Rules enforce:
- User can only access own data (userId = auth uid)
- Basic type checks for amounts/dates
- syncState must be one of PENDING/SYNCED/FAILED

## 6) Repository layer (Room + Firestore bridge)

Repository classes combine local DB operations and remote sync calls.

- app/src/main/java/com/team/financeapp/data/repository/ExpenseRepository.java
- app/src/main/java/com/team/financeapp/data/repository/BillRepository.java
- app/src/main/java/com/team/financeapp/data/repository/IncomeRepository.java
- app/src/main/java/com/team/financeapp/data/repository/GoalRepository.java

What repositories do:
- Load local data immediately for fast UI
- Refresh from Firestore and update local DB
- Save/update/delete locally first
- Push changes to Firestore
- Mark sync state by result

## 7) Background sync engine

### Worker scheduling
- app/src/main/java/com/team/financeapp/FinanceApplication.java
- app/src/main/java/com/team/financeapp/data/sync/PendingSyncWorker.java

At app start, FinanceApplication schedules PendingSyncWorker.

### Worker behavior
PendingSyncWorker:
- Runs every 15 minutes (periodic)
- Requires network connected
- Uses exponential backoff on failures
- Fetches PENDING/FAILED records from Room (Bills, Expenses, Incomes)
- Pushes to Firestore
- Updates sync state to SYNCED or FAILED

## 8) Feature-by-feature file map

### Expenses
UI + flow:
- app/src/main/java/com/team/financeapp/ExpensesActivity.java
- app/src/main/java/com/team/financeapp/AddExpenseActivity.java
- app/src/main/java/com/team/financeapp/ExpenseAdapter.java
- app/src/main/java/com/team/financeapp/Expense.java
Backend:
- app/src/main/java/com/team/financeapp/data/repository/ExpenseRepository.java
- app/src/main/java/com/team/financeapp/data/local/entity/ExpenseEntity.java
- app/src/main/java/com/team/financeapp/data/local/dao/ExpenseDao.java

### Bills
UI + flow:
- app/src/main/java/com/team/financeapp/BillsActivity.java
- app/src/main/java/com/team/financeapp/AddBillActivity.java
- app/src/main/java/com/team/financeapp/BillAdapter.java
- app/src/main/java/com/team/financeapp/Bill.java
Backend:
- app/src/main/java/com/team/financeapp/data/repository/BillRepository.java
- app/src/main/java/com/team/financeapp/data/local/entity/BillEntity.java
- app/src/main/java/com/team/financeapp/data/local/dao/BillDao.java

### Incomes
UI + flow:
- app/src/main/java/com/team/financeapp/IncomeHistoryActivity.java
- app/src/main/java/com/team/financeapp/AddIncomeActivity.java
- app/src/main/java/com/team/financeapp/IncomeAdapter.java
- app/src/main/java/com/team/financeapp/IncomeEntry.java
Backend:
- app/src/main/java/com/team/financeapp/data/repository/IncomeRepository.java
- app/src/main/java/com/team/financeapp/data/local/entity/IncomeEntity.java
- app/src/main/java/com/team/financeapp/data/local/dao/IncomeDao.java

### Goals
UI + flow:
- app/src/main/java/com/team/financeapp/GoalsActivity.java
- app/src/main/java/com/team/financeapp/AddGoalActivity.java
- app/src/main/java/com/team/financeapp/GoalDetailsActivity.java
- app/src/main/java/com/team/financeapp/GoalAdapter.java
- app/src/main/java/com/team/financeapp/Goal.java
Backend:
- app/src/main/java/com/team/financeapp/data/repository/GoalRepository.java
- app/src/main/java/com/team/financeapp/data/local/entity/GoalEntity.java
- app/src/main/java/com/team/financeapp/data/local/dao/GoalDao.java

### Dashboard
- app/src/main/java/com/team/financeapp/DashboardActivity.java

Dashboard currently reads and combines data from repositories/Firestore and computes totals/charts in app code.

### Notifications
- app/src/main/java/com/team/financeapp/NotificationsActivity.java
- app/src/main/java/com/team/financeapp/NotificationAdapter.java
- app/src/main/java/com/team/financeapp/NotificationItem.java

Current state: NotificationsActivity still uses mock list data.

## 9) Simple CRUD + sync example (Expense)

When user adds expense:
1. AddExpenseActivity builds Expense model and calls ExpenseRepository.saveExpense
2. Repository inserts ExpenseEntity into Room with syncState=PENDING
3. UI success can be shown immediately (local write done)
4. Repository pushes same record to Firestore
5. If cloud success: syncState -> SYNCED
6. If cloud error: syncState -> FAILED
7. PendingSyncWorker retries FAILED/PENDING later

The same pattern is used for Bills and Incomes. Goals use the same repository style too.

## 10) What is complete vs pending (backend-related)

Completed:
- Room database setup with entities + DAOs
- Repositories for Expenses/Bills/Incomes/Goals
- Local-first save/load
- Firestore sync in repositories
- Pending sync retry worker (WorkManager)
- Auth + Google sign-in + reset + change password
- Edit/Delete flows for Expenses/Bills/Incomes

Pending / partial:
- Notifications backend integration (still mock in UI)
- Cloud Functions layer not present in repo
- Automated tests are minimal (template-level only)
- Dashboard summary pipeline can be improved to full backend-driven summaries

## 11) Extra notes for team members

- App uses fallbackToDestructiveMigration in Room; schema changes can wipe local DB during version changes.
- local.properties is machine-specific (SDK path may show warnings on other machines).
- WorkManager periodic sync minimum interval is 15 minutes by Android rules.

---

If you want, I can also create a second short document with diagrams only (flowchart style) for quick revision before demo/presentation.
