# DreamSaver Backend Execution Checklist (Mar 25 -> Apr 5)

## Technology Stack (Chosen)
- Backend platform: Firebase (Auth, Firestore, Cloud Functions, Cloud Messaging)
- Local fast-access database: Room (Android SQLite abstraction)
- Background sync and reminders: WorkManager
- Validation and server-side workflows: Cloud Functions (TypeScript)
- Local and CI testing: Firebase Emulator Suite + Android unit tests

## Architecture Decision
- Source of truth: Firestore and Cloud Functions
- Read path in app: Room first (fast), then refresh from Firestore
- Write path in app: local write to Room with sync status, then sync to Firestore
- Conflict strategy: last-write-wins with server timestamp and owner checks

## Phase 1: Foundation (Mar 25)
- [ ] Create Firebase dev and prod projects
- [ ] Configure app for dev project and verify sign-in + Firestore access
- [x] Add Firestore security rules baseline (users only read/write own data)
- [x] Add Firestore indexes file
- [x] Add Room dependencies and create initial local database schema
- [x] Add WorkManager dependency and base sync worker shell

## Phase 2: Data Contracts and Security (Mar 26)
- [ ] Finalize schema for users, bills, expenses, goals, notifications, summaries
- [ ] Define required fields and validation rules for each collection
- [ ] Enforce userId ownership on all records
- [ ] Add security rules tests for unauthorized reads/writes

## Phase 3: Bills Backend (Mar 27-28)
- [ ] Implement bill model mapping (Room <-> Firestore)
- [ ] Build bill repository with offline-first behavior
- [ ] Replace sample bill loading in BillsActivity with repository reads
- [ ] Implement create/update/delete bill sync jobs
- [ ] Add due-date status calculation and server timestamp fields

## Phase 4: Expenses Backend (Mar 29)
- [ ] Implement expense model mapping (Room <-> Firestore)
- [ ] Build expense repository with offline-first behavior
- [ ] Replace sample expense loading in ExpensesActivity
- [ ] Add monthly aggregation update logic

## Phase 5: Goals Backend (Mar 30)
- [ ] Implement goal model mapping (Room <-> Firestore)
- [ ] Build goal repository with offline-first behavior
- [ ] Replace sample goal loading in GoalsActivity
- [ ] Add progress update and completion status logic

## Phase 6: Notifications and Reminders (Mar 31)
- [ ] Add FCM token registration and storage per user
- [ ] Implement Cloud Function scheduled reminder job for due bills
- [ ] Write notification records to Firestore and sync to local Room cache

## Phase 7: Dashboard Summaries (Apr 1)
- [ ] Define summary documents (monthly spending, due totals, goal progress)
- [ ] Create scheduled/cloud-triggered summary updates
- [ ] Use summary docs in dashboard for fast loading

## Phase 8: Hardening and QA (Apr 2-4)
- [ ] Add validation and error handling in repositories and workers
- [ ] Add idempotency keys for scheduled jobs (avoid duplicate reminders)
- [ ] Run emulator integration tests
- [ ] Run manual end-to-end tests for sign-in -> CRUD -> sync
- [ ] Deploy to production Firebase project
- [ ] Configure monitoring and alerts

## Phase 9: Final Buffer and Submission (Apr 5)
- [ ] Fix remaining blockers
- [ ] Verify all critical flows
- [ ] Freeze release candidate and prepare demo checklist

## Done Criteria
- [ ] All bills/expenses/goals are persisted to Firestore and cached in Room
- [ ] App works with fast local reads and recovers after offline periods
- [ ] Security rules block cross-user access
- [ ] Reminder jobs run automatically and do not duplicate
- [ ] Build and core tests pass
