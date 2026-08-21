**Java Source Documentation — Personal Finance Management App**

This document contains a detailed explanation of the mobile Java classes scanned so far. Each section includes: class purpose, key fields, public methods and short usage notes. (Progress: scanned initial set of mobile sources — more files will be appended.)

**ChangePasswordActivity.java**: `com.team.financeapp.ChangePasswordActivity`
- Purpose: UI screen to allow a logged-in user to change their password.
- Key fields: `AuthManager authManager`, `TextInputEditText inputCurrentPassword, inputNewPassword, inputConfirmPassword`, `MaterialButton btnChangePassword, btnCancel`.
- Important methods:
  - `onCreate(Bundle)` — sets up UI, initializes views and AuthManager.
  - `initializeViews()` — binds view references.
  - `setupClickListeners()` — wire up change and cancel buttons.
  - `handleChangePassword()` — validates input then calls `AuthManager.updatePassword(...)` with a callback handling success/error.
  - `validateInputs(...)` — input checks: emptiness, length >=6, matching, new != current.
  - `getTextFromEditText(...)` — helper to safely extract trimmed text.
- Notes: Uses toast messages for feedback; closes activity on success.

**BudgetProgress.java**: `com.team.financeapp.BudgetProgress`
- Purpose: Lightweight model holding a `BudgetLimitEntity` and the spent amount for that category.
- Fields: `BudgetLimitEntity limit`, `double spentAmount`.
- Methods: constructor + getters `getLimit()`, `getSpentAmount()`.
- Notes: Used by UI to render budget progress bars.

**BudgetAdapter.java**: `com.team.financeapp.BudgetAdapter`
- Purpose: RecyclerView adapter rendering budget progress rows.
- Key fields: list of `BudgetProgress` items, `OnDeleteClickListener` callback.
- Important methods:
  - `setBudgets(List)` — replace dataset and notify.
  - `onCreateViewHolder/onBindViewHolder/getItemCount()` — standard adapter lifecycle; `onBindViewHolder` calculates percent and changes tint colors.
  - `BudgetViewHolder` — binds category name, status text, `ProgressBar` and delete icon.
- Notes: Progress color thresholds: >=100 red, >=80 orange, else blue. Delete uses provided listener.

**BudgetActivity.java**: `com.team.financeapp.BudgetActivity`
- Purpose: Screen for creating and viewing monthly budget limits and their progress.
- Key fields: spinner, `etLimitAmount`, save button, `RecyclerView` + `BudgetAdapter`, `BudgetRepository`, `userId`, `currentMonthYear`.
- Important methods:
  - `onCreate(Bundle)` — verifies auth, prepares repository, initializes UI and loads budgets.
  - `setupViews()` — binds views, configures adapter with delete callback to `BudgetRepository.deleteBudgetLimit(...)`.
  - `loadBudgets()` — calls repository to load and set the adapter data.
  - `saveLimit()` — validates input, calls `BudgetRepository.saveBudgetLimit(...)`, shows toast and reloads.
  - `onSupportNavigateUp()` — finish activity.
- Notes: Uses a fixed set of categories and stores limits per `currentMonthYear`.

**BottomNavigationFragment.java**: `com.team.financeapp.BottomNavigationFragment`
- Purpose: Reusable fragment that hosts the bottom navigation across top-level activities.
- Key features: statically attachable via `attach(AppCompatActivity, containerId, selectedItemId)`.
- Important methods:
  - `newInstance(int)` — create fragment with selected item id.
  - `onViewCreated(...)` — sets up `BottomNavigationView` listener to call `handleNavigation(itemId)`.
  - `handleNavigation(int)` — resolves to Activity classes (DashboardActivity, ExpensesActivity, BillsActivity, GoalsActivity, IncomeHistoryActivity) and starts the destination with `FLAG_ACTIVITY_REORDER_TO_FRONT`.
- Notes: Enables consistent navigation and animations between top-level screens.

**BillsActivity.java**: `com.team.financeapp.BillsActivity`
- Purpose: Shows user's bills list, filter chips for status, total due amount, and actions (edit/mark paid/delete).
- Key fields: `BillRepository`, `BillAdapter`, lists `billsList`, `allBillsList`, UI fields (total due TextView, RecyclerView, chips).
- Important methods:
  - `onCreate/onResume()` — init, attach BottomNavigationFragment, load bills.
  - `setupRecyclerView()` — create adapter and wire click listeners to `openEditBill`.
  - `loadBills()` — loads bills via repository and sorts by due date.
  - `filterBills(int)` — filter list by chip selection (all, urgent, paid, pending).
  - `calculateTotalDue()` — aggregates unpaid bills into displayed total.
  - `showBillActions(Bill)` — dialog to edit/mark/delete; delegates to `updateBillStatus` and `deleteBill`.
  - `updateBillStatus(...)`, `deleteBill(...)` — call `billRepository` to persist changes, then reload.
- Notes: Robust UX flows for paid vs pending bills; uses status-based indicators and drawable resources.

**BillAdapter.java**: `com.team.financeapp.BillAdapter`
- Purpose: Adapter for displaying `Bill` items in a RecyclerView.
- Key functionality: binds category icon, status indicator background, formatted due date and amount; handles click and long-click via `OnBillItemClickListener`.
- ViewHolder: `BillViewHolder` – uses `DrawableUtils.safeSetImageResource` and `safeSetBackgroundResource`.

**Bill.java**: `com.team.financeapp.Bill`
- Purpose: Data model representing a bill.
- Fields: `id, name, description, amount, dueDate (millis), category, categoryIcon (res id), status, indicatorColor`.
- Methods: constructors (with and without id), getters and setters.

**AddIncomeActivity.java**: `com.team.financeapp.AddIncomeActivity`
- Purpose: Screen to add or edit an income entry.
- Key fields: extras constants, input fields (`etAmount`, `etDescription`, `etDate`), `spinnerSource` (AutoCompleteTextView), `calendar`, date/time formatters, `IncomeRepository`, `AuthManager`, `isEditMode`.
- Important methods:
  - `onCreate(...)` — reads intent extras to set edit mode, initializes UI and repository.
  - `setupSourceDropdown()` — binds source list adapter.
  - `showDatePickerDialog()` — custom AlertDialog with a DatePicker; sets `etDate`.
  - `saveIncome()` — validate inputs, parse amount, build `IncomeEntry`, then call `incomeRepository.saveIncome(...)` or `updateIncome(...)` depending on edit mode.
  - `populateIfEditing()` — pre-fill fields if editing.
  - `resolveSourceIcon(String)` — helper mapping source text to drawable icons.
- Notes: Uses explicit date picker dialog and prevents manual date editing for consistency.

**AddGoalActivity.java**: `com.team.financeapp.AddGoalActivity`
- Purpose: Create or edit savings goals with a target amount and date.
- Key fields: extras constants, inputs (`etTargetAmount`, `etGoalName`, `etTargetDate`, `etCurrentAmount`), dropdown for `goalTypes`, `GoalRepository`, `isEditMode`, calendar/date formats.
- Important methods:
  - `loadGoalData()` — when editing, read extras and populate fields, compute remaining days.
  - `showDatePickerDialog()` — picks target date and restricts min date.
  - `updateRemainingDays()` — compute and show human-friendly remaining days.
  - `saveGoal()` — validation, parse values, create `Goal` object and call `goalRepository.saveGoal(...)` or `updateGoal(...)`.
- Notes: Extensive validation and user feedback; sets default icons and progress backgrounds.

**AddExpenseActivity.java**: `com.team.financeapp.AddExpenseActivity`
- Purpose: Add or edit expense entries; includes camera-based receipt scanning with ML backend support.
- Key fields: extras, UI inputs, `btnScanReceipt`, camera launchers & permission requesters, `ExpenseRepository`, `AuthManager`, `calendar` and formatters.
- Important methods:
  - `setupCameraLaunchers()` — registers activity result launchers for camera capture and permission requests.
  - `launchCamera()` — create temp file and launch camera intent to capture receipt.
  - `processReceiptImage(Bitmap)` — compress + base64 encode image, POST to AI API via `AIApiService.scanReceipt(...)`, then parse response and fill amount/category/date.
  - `autoCategorizeWithAI()` — calls `ExpenseApiService.categorizeExpense(...)` with description to attempt auto-category selection.
  - `saveExpense()` — validate fields and call `expenseRepository.saveExpense(...)` or `updateExpense(...)`.
- Notes: Uses ML-based receipt scanning and auto-categorization; handles permission flow and temporary file creation.

**AddBillActivity.java**: `com.team.financeapp.AddBillActivity`
- Purpose: Add or edit bills (recurring or one-off), supports marking as paid and deleting.
- Key fields: extras constants, UI inputs, `BillRepository`, `isEditMode`, `editingBillId`, `billStatus`, calendar/date format.
- Important methods:
  - `saveBill()` — validates inputs, creates `Bill` and saves/updates via repository.
  - `confirmMarkAsPaid()` / `markBillAsPaid()` — prompt and persist paid status.
  - `confirmDeleteBill()` / `deleteBill()` — confirm and delete.
  - `populateIfEditing()` — load intent extras into UI and disable editing for `paid` bills.
  - `resolveStatus(...)` / `resolveIndicator(...)` / `resolveCategoryIcon(...)` — helpers mapping dates/status to strings/drawables.

**AppLockManager.java**: `com.team.financeapp.AppLockManager`
- Purpose: Central static utility for managing app lock (PIN + biometric), preferences and session state.
- Key constants: SharedPreferences keys, default timeouts, timeout presets.
- Key fields (static): `sessionUnlocked`, `unlockScreenVisible`, `backgroundTimestamp`.
- Important methods:
  - Preference getters/setters: `isAppLockEnabled`, `setAppLockEnabled`, `hasPin`, `setPin`, `clearPin`.
  - `verifyPin(...)` — compares saved hash with computed hash of provided PIN.
  - Biometric helpers: `isBiometricAvailable`, `isBiometricEnabled`, `setBiometricEnabled`.
  - Session handling: `markSessionUnlocked`, `lockSession`, `onAppBackgrounded`, `onAppForegrounded`, `shouldRequireLock(...)` — decides whether lock screen is required based on timeouts and session state.
  - Utility: `timeoutLabel(...)`, private `hash(pin, salt)` using SHA-256 and base64.
- Notes: PIN hashing uses salt + SHA-256; sensitive operations stored in private SharedPreferences; integrates with Firebase auth presence for requirement.

**AppLockActivity.java**: `com.team.financeapp.AppLockActivity`
- Purpose: Unlock screen activity supporting PIN or biometric unlock.
- Key methods: `unlockWithPin()` verifies via `AppLockManager.verifyPin`, `unlockWithBiometric()` uses `BiometricPrompt` and on success marks session unlocked.
- Notes: Prevents back navigation to avoid bypass.

**AppLockSettingsActivity.java**: `com.team.financeapp.AppLockSettingsActivity`
- Purpose: Settings screen to enable/disable app lock, configure PIN and biometric and timeout.
- Key flows:
  - Save new PIN (with validation and optional verification of current PIN when changing).
  - Remove PIN (requires verification).
  - Toggle biometric (checks availability first).
  - Set timeout via dropdown that maps to AppLockManager constants.
  - Uses biometric prompt to authenticate when disabling lock.
- Notes: Has protections to prevent accidental toggling and ensures UX keeps toggles consistent.

**ForgotPasswordActivity.java**: `com.team.financeapp.ForgotPasswordActivity`
- Purpose: UI for sending a password reset email through Firebase.
- Important methods: `handleSendOTP()` — validates email format and calls `AuthManager.sendPasswordResetEmail(...)`.

**ForecastActivity.java**: `com.team.financeapp.ForecastActivity`
- Purpose: Display AI-powered forecast (income, expense, bills) combining local baseline (ForecastEngine) with backend ML predictions (`ForecastApiService`).
- Important flow:
  - `runForecast()` — run local forecast on IO executor, then call remote ML endpoint. On success, merge ML response into `ForecastResult` and call `displayResult(...)`.
  - `displayResult(ForecastResult)` — formats month, net cash flow, predicted numbers, and builds a category breakdown with dynamic progress bars.
- Notes: Gracefully falls back to local forecast when backend is unavailable.

**auth/UserProfile.java**: `com.team.financeapp.auth.UserProfile`
- Purpose: Simple DTO for user profile data used with backend API.
- Fields: `id, displayName, email, photoUrl, age, phone, updatedAt` with getters/setters and a JSON annotation for serialized name.

**auth/AuthManager.java**: `com.team.financeapp.auth.AuthManager`
- Purpose: Central authentication helper wrapping Firebase Authentication, Google sign-in and a backend user upsert.
- Key fields: `FirebaseAuth firebaseAuth`, `UserApiService userApiService`.
- Public API:
  - `isUserLoggedIn()`, `getCurrentUserId()`
  - `signInWithEmail(email,password,Activity,AuthCallback)` — sign in and upsert profile.
  - `registerWithEmail(fullName,email,password,Activity,AuthCallback)` — create user, set display name, upsert profile.
  - `getGoogleSignInClient(Context)` — build GoogleSignInClient with web client id.
  - `signInWithGoogleIdToken(idToken, Activity, AuthCallback)` — sign in with credential and upsert.
  - `signOut(Context)` — locks session, cancels reminders, clears FCM token and signs out of Firebase and Google.
  - `sendPasswordResetEmail(...)`, `updatePassword(current,new,Activity,AuthCallback)` — reauthenticate then update.
- Internal helpers: `upsertUserProfile(FirebaseUser, fallbackName)` — call backend to create/update user; `resolveError(Exception)` returns user-friendly message.

**LoginActivity.java**: `com.team.financeapp.LoginActivity`
- Purpose: Authentication UI; supports email/password and Google sign-in.
- Key fields: input fields, `AuthManager`, `GoogleSignInClient`, `ActivityResultLauncher` for Google.
- Important methods: `handleLogin()` with validation and callback handling; `startGoogleSignIn()` launching Google flow; `setupGoogleResultLauncher()` to process sign-in result and call `AuthManager.signInWithGoogleIdToken`.
- Notes: On successful login uploads FCM token using `FcmTokenUploader`.

**IncomeHistoryActivity.java**: `com.team.financeapp.IncomeHistoryActivity`
- Purpose: Presents historical income records with date grouping, filters, and totals.
- Key fields: `IncomeRepository`, `IncomeAdapter`, `chipGroupFilter`, `AuthManager`.
- Important methods: `loadIncomeHistory()` fetches incomes, `filterIncomes(int)` supports chip filters (this month, last 7 days, by source), `normalizeEpochMillis(...)` to handle seconds vs milliseconds.
- Notes: Implements IncomeAdapter.OnIncomeItemClickListener to edit/delete incomes.

**IncomeEntry.java**: `com.team.financeapp.IncomeEntry`
- Purpose: Model for an income entry with immutable fields: id, source, amount, note, date, time, sourceIcon.
- Methods: constructors and getters.

**IncomeAdapter.java**: `com.team.financeapp.IncomeAdapter`
- Purpose: RecyclerView adapter that groups incomes by date and renders date headers plus income items.
- Key behavior: `groupByDate()` converts `List<IncomeEntry>` into a mixed list of date-String headers and `IncomeEntry` items; `getDateLabel(...)` returns friendly labels "Today", "Yesterday" or formatted date.
- View types: header and item with corresponding ViewHolders.

**HomeActivity.java**: `com.team.financeapp.HomeActivity`
- Purpose: Launcher/home screen that redirects logged-in users to Dashboard; supports a debug long-press to send test notifications.
- Key methods: `onStart()` redirects if logged in, `sendTestNotification()` uses FinancialNotificationHelper.

**GoalsActivity.java**: `com.team.financeapp.GoalsActivity`
- Purpose: Shows savings goals list with filters, add/edit/delete flows and totals.
- Important methods: `loadGoals()` from `GoalRepository`, `filterGoals(int)` for chips, empty-state logic, `calculateTotalSaved()`.

**GoalDetailsActivity.java**: `com.team.financeapp.GoalDetailsActivity`
- Purpose: Full detail view for a single goal with Add Savings, Edit, and Delete flows.
- Important methods:
  - `showAddSavingsDialog()` — prompts user to add savings, checks goal completion, calls `applySavings(...)` to update repository.
  - `applySavings(String)` — validation, updates `currentAmount`, persists via `GoalRepository.addGoalSavings(...)`, and optionally sends guidance notification.
  - `deleteGoal()` — invokes `goalRepository.deleteGoal(...)` and returns result extras to caller.

**GoalAdapter.java**: `com.team.financeapp.GoalAdapter`
- Purpose: Adapter that renders goal cards with progress bar, status text, amounts, and icons.
- Methods: `updateGoals(List)`, `removeGoal(int)` and `GoalViewHolder` binding.

**Goal.java**: `com.team.financeapp.Goal`
- Purpose: Data model for a savings goal — id, name, description, target/current amounts, targetDate, category, icons.
- Utility methods: `getProgressPercentage()` and `getRemainingAmount()`.

**NotificationsActivity.java**: `com.team.financeapp.NotificationsActivity`
- Purpose: Shows in-app notifications list and user actions (mark read, show details, delete). Integrates with `NotificationRepository`.
- Important flows: `loadNotifications()`, `renderNotifications()`, `updateUnreadBadge()`, `showNotificationDetails(...)`, `deleteNotification(...)`, and opening app notification settings.

**EditProfileActivity.java**: `com.team.financeapp.EditProfileActivity`
- Purpose: Allow editing of user profile (name, email, age, phone) and synchronize to Firebase and backend.
- Important flows: `loadProfileData()` pre-fills from Firebase and backend, `saveProfileData()` validates inputs, updates Firebase `UserProfileChangeRequest` and calls backend `userApiService.createOrUpdateUser(...)`.

**DrawableUtils.java**: `com.team.financeapp.DrawableUtils`
- Purpose: Small utility to safely set image and background resources ignoring `Resources.NotFoundException`.
- Methods: `safeSetImageResource(ImageView, resId, fallbackResId)`, `safeSetBackgroundResource(View, resId, fallbackResId)`.

---
**ExpenseAdapter.java**: `com.team.financeapp.ExpenseAdapter`
- Purpose: RecyclerView adapter that displays expenses grouped by date with date header rows and expense item rows.
- Key fields: mixed `List<Object> items` (String date headers + `Expense` items), `List<Expense> expenses`, `OnExpenseItemClickListener listener`.
- Important methods:
  - `groupExpensesByDate()` — sorts expenses (newest first), creates date header labels and interleaves items into `items` list.
  - `getDateLabel(long)` — returns human-friendly labels ("Today", "Yesterday", or formatted date).
  - `onCreateViewHolder/onBindViewHolder/getItemCount()` — standard adapter lifecycle with two view types: `TYPE_DATE_HEADER` and `TYPE_EXPENSE_ITEM`.
  - `updateExpenses(List<Expense>)` — replace dataset and re-group.
  - `ExpenseViewHolder.bind(...)` — binds category icon, name, time and amount and wires click/long-click events to listener.
- Notes: Uses `DrawableUtils.safeSetImageResource` for icons; time and amount are formatted for display.

**Expense.java**: `com.team.financeapp.Expense`
- Purpose: Simple model representing a single expense transaction.
- Fields: `id, category, amount, description, date (millis), time (HH:mm), categoryIcon (res id)`.
- Methods: constructors (with/without id), getters and setters.

**EducationActivity.java**: `com.team.financeapp.EducationActivity`
- Purpose: Minimal Activity showing educational content; provides a back button to finish the activity.
- Key methods: `onCreate(Bundle)` — inflates `activity_education` layout and attaches back button click listener.

**BudgetWidgetProvider.java**: `com.team.financeapp.widget.BudgetWidgetProvider`
- Purpose: AppWidgetProvider that supplies the `widget_budget` RemoteViews for home screen widgets.
- Important behavior:
  - `onUpdate(...)` — iterates widget ids and calls `updateAppWidget`.
  - `updateAppWidget(...)` — wires PendingIntents to open `AddExpenseActivity` and `AddIncomeActivity` and sets a placeholder balance text.
- Notes: Widget uses static text "Open App to Sync" — actual balance update would require an asynchronous update (AppWidgetService or Worker).

**ChatAdapter.java**: `com.team.financeapp.chatbot.ChatAdapter`
- Purpose: RecyclerView adapter used in the chatbot UI to display user and bot messages.
- Key fields: `List<ChatMessage> messages`.
- Important methods:
  - `addMessage(ChatMessage)` — append message and notify.
  - `setTyping(boolean)` — shows or removes a typing indicator message ("...").
  - `getItemViewType/onCreateViewHolder/onBindViewHolder/getItemCount()` — two view types: user and bot.
- Notes: Simple adapter; view holders bind `tv_message` text only.

**ChatbotActivity.java**: `com.team.financeapp.chatbot.ChatbotActivity`
- Purpose: Activity providing an AI financial assistant chat UI; sends user messages to backend `ChatApiService` and displays replies.
- Key fields: `RecyclerView recyclerChat`, `EditText etMessage`, `ImageButton btnSend`, `ChatAdapter chatAdapter`, `ChatApiService chatApiService`.
- Important methods:
  - `initRetrofit()` — creates `ChatApiService` from `ApiClient`.
  - `sendMessage()` — validates input, displays the user message, shows typing indicator, POSTs to backend `chatApiService.sendMessage(userId, request)` and on response displays bot reply or error.
  - `initViews()` — sets up RecyclerView with `LinearLayoutManager` and adapter.
- Notes: Requires an authenticated Firebase user; shows a welcome prompt on start and disables send button while awaiting reply.

**ChatMessage.java**: `com.team.financeapp.chatbot.ChatMessage`
- Purpose: DTO class representing a chat message with text and sender flag (`isUser`).
- Methods: constructor and getters `getText()` and `isUser()`.

**DashboardActivity.java**: `com.team.financeapp.DashboardActivity`
- Purpose: Main dashboard showing summaries (balance, charts, goals, bills) and quick actions.
- Key fields: repositories (`BillRepository`, `ExpenseRepository`, `IncomeRepository`, `GoalRepository`), many UI views (balance, charts, cards), `AuthManager` and FCM token refresh.
- Important behaviors/methods:
  - `initializeViews()` — binds many UI elements, configures month navigation and quick-action buttons.
  - `loadDashboardData()` — verifies auth and loads bills, expenses, goals and income via their repositories.
  - `loadBills/loadExpenses/loadGoals/loadIncome(...)` — call repositories asynchronously and update charts/cards on callbacks.
  - Chart rendering helpers: `updateExpenseChartFromData()`, `updateIncomeChartFromData()`, `setupPieChart(...)` using `ChartHelper`.
  - Privacy & visibility: `applyBalancePrivacyState()`, `savePrivacyPreference()` store balance masking preference.
  - Date helpers: `normalizeEpochMillis(long)`, `isDateInSelectedMonth(...)`, `formatDueLabel(...)`, `daysFromToday(...)`.
  - Utility aggregators: `sumIncomeForMonth`, `sumExpensesForMonth`, `sumDueBills`, `sumPaidBills`, etc.
- Notes: Handles runtime notification permission on Android 13+, refreshes FCM token on launch, registers logout broadcast receiver, and uses `BottomNavigationFragment` for navigation.

**PendingSyncWorker.java**: `com.team.financeapp.data.sync.PendingSyncWorker`
- Purpose: Worker that synchronizes locally created/modified/deleted `Bill`, `Expense`, and `Income` records to the backend when network is available.
- Key behaviors:
  - `doWork()` — checks current Firebase user, fetches pending local entities via DAOs and attempts to push them to the backend using Retrofit services; marks local `syncState` as `SYNCED` or `FAILED` accordingly.
  - `syncBill/syncExpense/syncIncome(...)` — synchronous (blocking) HTTP calls (`execute()`) used by the worker to create/update/delete remote records. Handles remote ID parsing and local DB updates on success.
  - `schedule(Context)` — public helper to schedule a periodic `WorkManager` job (15-minute periodic request) constrained to network connectivity.
- Notes: Designed for eventual consistency; retries the work on failures. Uses `SyncState` constants for local tracking.

**ExpensesActivity.java**: `com.team.financeapp.ExpensesActivity`
- Purpose: Activity listing expense history in a timeline view with filtering (all, this month, last 7 days, categories) and total calculation for current month.
- Key fields: `ExpenseRepository expenseRepository`, `ExpenseAdapter expenseAdapter`, `List<Expense> expensesList, allExpensesList`, UI components (chips, FAB, RecyclerView).
- Important methods:
  - `loadExpenses()` — loads expenses via `ExpenseRepository.loadExpenses(userId, callback)` and updates adapter.
  - `filterExpenses(int)` — applies chip-based filters using `Stream`/`filter` or manual checks.
  - `calculateTotalAmount()` — sums expenses for the current month and updates `tvTotalAmount`.
  - `showExpenseActions`, `openEditExpense`, `confirmDeleteExpense`, `deleteExpense` — edit/delete flows.

**FinanceApplication.java**: `com.team.financeapp.FinanceApplication`
- Purpose: Application subclass that initializes global components on process start (API client, theme manager, scheduled workers) and handles foreground/background lifecycle to enforce app lock.
- Important flows:
  - `onCreate()` — calls `ApiClient.init`, `ThemePreferenceManager.applySavedTheme`, schedules `PendingSyncWorker`, reschedules reminders and monitor worker, and registers activity lifecycle callbacks to drive `AppLockManager` behavior.
  - Lifecycle callbacks: show `AppLockActivity` when required on resume, and call `AppLockManager.onAppBackgrounded()` when app is backgrounded.

**MainActivity.java**: `com.team.financeapp.MainActivity`
- Purpose: Minimal host activity that enables edge-to-edge UI and applies system window insets to the main view.
- Key methods: `onCreate(Bundle)` — calls `EdgeToEdge.enable(this)` and sets a window insets listener to add padding.

**NotificationAdapter.java**: `com.team.financeapp.NotificationAdapter`
- Purpose: RecyclerView adapter for in-app notifications with actions to mark read, show details, and delete.
- Key features: `NotificationActionListener` callback interface, view holder binds icon/title/message/time, unread indicator toggle, delete button handling.

**NotificationItem.java**: `com.team.financeapp.NotificationItem`
- Purpose: Immutable model representing a single in-app notification (id, title, message, timeLabel, icon, createdAt, notificationId, unread flag).
- Methods: getters and `setUnread(boolean)`.

**SyncState.java**: `com.team.financeapp.data.local.SyncState`
- Purpose: Small constants holder for local sync state values: `PENDING`, `SYNCED`, `FAILED`.

**AIApiService.java**: `com.team.financeapp.data.remote.AIApiService`
- Purpose: Retrofit interface for AI-related endpoints; currently defines `scanReceipt` POST to `ai/scan-receipt` returning a Map payload.

**ApiClient.java**: `com.team.financeapp.data.remote.ApiClient`
- Purpose: Singleton Retrofit provider that configures HTTP client, logging, auth header injection, and token refresh.
- Important details:
  - `init(Context)` stores application context for logout broadcasting.
  - `getClient()` builds `OkHttpClient` with `AuthInterceptor` and `TokenAuthenticator`, and constructs Retrofit with `BuildConfig.BASE_URL`.
  - `AuthInterceptor` attaches `X-User-Id` and `Authorization: Bearer <token>` headers using Firebase `getIdToken(false)`.
  - `TokenAuthenticator` forces token refresh (`getIdToken(true)`) when authentication fails and broadcasts `ACTION_LOGOUT` if user is missing or refresh fails.
- Notes: Timeouts are set to 120s; logging interceptor is enabled at `BODY` level (consider disabling for production).

**BillRepository.java**: `com.team.financeapp.data.repository.BillRepository`
- Purpose: Data repository that exposes bill CRUD operations combining local Room DB operations and remote sync via `BillApiService`.
- Key methods:
  - `loadBills(userId, callback)` — loads local bills synchronously then calls `refreshFromRemote` to update from server.
  - `saveBill(userId, Bill, callback)` — inserts a `BillEntity` locally with temporary UUID, schedules reminders, and pushes to remote (`pushBillToRemote`).
  - `updateBill(...)` and `deleteBill(...)` — update local entity (mark deleted) and schedule remote update/delete.
  - Remote helpers: `pushBillToRemote`, `updateBillInRemote`, `deleteBillInRemote` that perform async Retrofit calls and update `syncState` on success/failure.
- Notes: Uses a single-threaded `ExecutorService` for IO and a `Handler` to post results to main thread. Schedules/cancels reminders during CRUD operations.

**GoalApiService.java**: `com.team.financeapp.data.remote.GoalApiService`
- Purpose: Retrofit interface for goal CRUD and add-savings endpoint.
- Methods: `getGoals()`, `createGoal(GoalEntity)`, `updateGoal(id, GoalEntity)`, `addSavings(id, Map)`, `deleteGoal(id)`.

**ForecastApiService.java**: `com.team.financeapp.data.remote.ForecastApiService`
- Purpose: Retrofit interface to fetch ML forecast predictions from backend (`GET forecasts/ml-predict`).

**ExpenseRemoteRepository.java**: `com.team.financeapp.data.remote.ExpenseRemoteRepository`
- Note: file present but empty in repository — appears to be a placeholder for future remote expense sync helpers.

**ExpenseApiService.java**: `com.team.financeapp.data.remote.ExpenseApiService`
- Purpose: Retrofit interface for expense CRUD and a `categorizeExpense` helper endpoint.
- Methods: `getExpenses(), createExpense(), updateExpense(id), deleteExpense(id), categorizeExpense(Map)`.

**NotificationApiService.java**: `com.team.financeapp.data.remote.NotificationApiService`
- Purpose: Retrofit interface for fetching and mutating in-app notifications: list all, list unread, mark as read, and delete.

**IncomeApiService.java**: `com.team.financeapp.data.remote.IncomeApiService`
- Purpose: Retrofit interface for income CRUD: `getIncomes(), createIncome(), updateIncome(id), deleteIncome(id)`.

**UserApiService.java**: `com.team.financeapp.data.remote.UserApiService`
- Purpose: REST client for user endpoints: `getCurrentUser()` and `createOrUpdateUser(UserProfile)` used by `AuthManager`.

**BudgetApiService.java**: `com.team.financeapp.data.remote.BudgetApiService`
- Purpose: Retrofit interface for budget limits retrieval and management: `getBudgets(monthYear)`, `createOrUpdateBudget`, `deleteBudget(id)`.

**IncomeEntity.java**: `com.team.financeapp.data.local.entity.IncomeEntity`
- Purpose: Room `@Entity` representing an income record in the local database.
- Fields: `localId (PK auto-generated)`, `remoteId`, `userId`, `source`, `amount`, `note`, `date`, `time`, `sourceIcon`, `createdAt`, `updatedAt`, `syncState`, `deleted`.
- Notes: `syncState` defaults to "PENDING" and `remoteId` is used to correlate with backend rows.

**GoalEntity.java**: `com.team.financeapp.data.local.entity.GoalEntity`
- Purpose: Room entity representing a savings/goal record.
- Fields: `localId`, `remoteId`, `userId`, `name`, `description`, `targetAmount`, `currentAmount`, `addedSavingsAmount`, `targetDate`, `category`, `categoryIcon`, `progressCircleBackground`, `createdAt`, `updatedAt`, `syncState`, `deleted`.

**ExpenseEntity.java**: `com.team.financeapp.data.local.entity.ExpenseEntity`
- Purpose: Room entity representing an expense record.
- Fields: `localId`, `remoteId`, `userId`, `category`, `amount`, `description`, `date`, `time`, `categoryIcon`, `createdAt`, `updatedAt`, `syncState`, `deleted`.

**BudgetLimitEntity.java**: `com.team.financeapp.data.local.entity.BudgetLimitEntity`
- Purpose: Room entity for budget limits per category per month.
- Fields: `localId`, `remoteId`, `userId`, `category`, `limitAmount`, `monthYear (yyyy-MM)`, `syncState`, `updatedAt`.

**BillEntity.java**: `com.team.financeapp.data.local.entity.BillEntity`
- Purpose: Room entity representing a bill/reminder (amount, dueDate, status, recurring flag).
- Fields: `localId`, `remoteId`, `userId`, `name`, `description`, `amount`, `dueDate`, `category`, `categoryIcon`, `status`, `indicatorColor`, `createdAt`, `updatedAt`, `syncState`, `deleted`, `isRecurring`.

**ForecastEngine.java**: `com.team.financeapp.forecast.ForecastEngine`
- Purpose: Local mobile forecast utility that computes next-month predictions using a weighted moving average over the last up to 6 months.
- Key methods: `calculateForecast(userId)` — collects monthly totals via DAOs (`ExpenseDao`, `IncomeDao`, `BillDao`), computes weighted averages, estimates recurring bills, and returns `ForecastResult`.
- Notes: Runs offline using Room; helpful fallback when remote ML service is unavailable.

**ForecastResult.java**: `com.team.financeapp.forecast.ForecastResult`
- Purpose: Data holder for forecast outputs: `forecastMonth`, `predictedExpense`, `predictedIncome`, `predictedBills`, `netCashFlow`, `categoryBreakdown`, `monthsOfData`.

**IncomeDao.java**: `com.team.financeapp.data.local.dao.IncomeDao`
- Purpose: Room DAO for income operations: `getByUser`, `getPendingSync`, `getByLocalId`, `insert`, `insertAll`, `deleteAllForUser`, `update`, `deleteByLocalId`, `getTotalForRange`.

**GoalDao.java**: `com.team.financeapp.data.local.dao.GoalDao`
- Purpose: Room DAO for goals: `getByUser`, `getAllByUser`, `getPendingSync`, `insert`, `insertAll`, `update`, `deleteByLocalId`, `getById`, `getByRemoteId`, `deleteAllForUser`.

**ExpenseDao.java**: `com.team.financeapp.data.local.dao.ExpenseDao`
- Purpose: Room DAO for expenses: `getByUser`, `getPendingSync`, `getByLocalId`, `insert`, `insertAll`, `deleteAllForUser`, `update`, `deleteByLocalId`, `getCategoryTotalForMonth`, `getTotalForRange`, `getCategoryTotalsForRange`.

**CategoryTotal.java**: `com.team.financeapp.data.local.dao.CategoryTotal`
- Purpose: POJO result for grouped expense queries with `category` and `total` fields.

**BudgetDao.java**: `com.team.financeapp.data.local.dao.BudgetDao`
- Purpose: Room DAO for budget limits with `insert`, `insertAll`, `update`, `getByUserAndMonth`, `getByCategoryAndMonth`, `deleteAllForUser`, `deleteByCategoryAndMonth`.
Progress update: I scanned and documented the first batch of mobile Java files and created DOCS/Java_Documentation.md with their detailed descriptions. Next: I'll continue scanning the remaining mobile sources (notifications helpers, remote API services, repositories, DAOs, local entities, utils) and then the backend Java sources, appending documentation to the same file until all Java files are covered. Please confirm I should continue and I'll proceed to scan and append the remaining files now.