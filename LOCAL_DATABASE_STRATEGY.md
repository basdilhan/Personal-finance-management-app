# Local Database Strategy (Room + Firebase)

## Why Local Database
- Fast screen load without network delay
- Offline access for bills, expenses, and goals
- Better UX with immediate updates and later background sync

## Chosen Pattern
- Offline-first repository pattern
- Room is the local persistence layer
- Firestore is remote source of truth
- WorkManager performs retry-safe background sync

## Sync Model
1. User creates or edits data in UI
2. App writes immediately to Room with syncState = PENDING
3. Sync worker pushes pending records to Firestore
4. On success, syncState = SYNCED and updatedAt syncs to server timestamp
5. On failure, syncState = FAILED and WorkManager retries with backoff

## Conflict and Ownership Rules
- Every row and document includes userId
- Every write validates userId against current authenticated user
- Use server timestamps for authoritative last update time
- Conflict policy: latest server update wins, then refresh Room

## Minimum Tables
- bills
- expenses
- goals

Shared fields:
- localId (Room primary key)
- remoteId (Firestore document id)
- userId
- createdAt
- updatedAt
- syncState (PENDING, SYNCED, FAILED)
- deleted (soft delete for sync-safe removal)

## Data Flow Rules
- Read operations: Room first, then remote refresh
- Write operations: Room first, remote sync async
- Delete operations: mark deleted locally, then remote delete, then purge local

## Security Rules Alignment
- Firestore rules require request.auth.uid == userId
- Client never writes another userId
- Cloud Functions recheck ownership for sensitive operations

## Rollout Sequence
1. Add Room schema and DAO interfaces
2. Replace sample data usage in BillsActivity, ExpensesActivity, GoalsActivity
3. Add repository classes and mapper classes
4. Add sync worker for each entity group
5. Add telemetry logs for sync success/failure rates
