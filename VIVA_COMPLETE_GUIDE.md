# 📘 DreamSaver — Complete Viva Preparation Guide
### Every File, Every Function, Simply Explained

---

# PART 1: THE JAVA BACKEND (Spring Boot)

The backend lives in `backend/src/main/java/com/example/backend/` and is split into 5 packages: **config**, **controller**, **entity**, **repository**, and **service**.

---

## 📂 Package 1: `config/` — Security & System Setup

These files run **before** any user request reaches your app logic. Think of them as the security guards at the front door.

### 1. `FirebaseConfig.java`
**What it does:** When the Java server boots up, this file initializes the Firebase Admin SDK using a secret JSON key. Without this, the server cannot verify any user tokens.
**Key function:** `firebaseInit()` — Reads the Firebase service account key and calls `FirebaseApp.initializeApp()`.

### 2. `SecurityConfig.java`
**What it does:** Configures Spring Security. It defines which API endpoints are open to the public and which require authentication.
**Key logic:** It registers the `FirebaseAuthFilter` and explicitly permits `/health` (for cron-job keepalive) while locking down everything under `/api/**`.

### 3. `FirebaseAuthFilter.java`
**What it does:** A custom security filter that intercepts every incoming HTTP request. It extracts the `Authorization: Bearer <token>` header, sends it to Google Firebase for cryptographic verification, and either allows or blocks the request.
**Key function:** `doFilterInternal()` — Calls `FirebaseAuth.getInstance().verifyIdToken(token)` to validate the JWT.

### 4. `UserSyncInterceptor.java`
**What it does:** After the token is verified, this interceptor checks if the user already exists in the PostgreSQL database. If not, it automatically creates a new `UserEntity` profile.
**Key function:** `preHandle()` — Runs `userRepository.existsById(uid)`. If `false`, it creates a new user row.
**Why it matters:** It bridges the gap between Firebase Auth and your relational database seamlessly.

### 5. `WebConfig.java`
**What it does:** Registers the `UserSyncInterceptor` into Spring's interceptor chain and configures CORS (Cross-Origin Resource Sharing) so that your React web app can talk to the Java backend without browser security errors.

### 6. `JacksonConfig.java`
**What it does:** Configures the JSON serializer (Jackson). It tells Spring to ignore unknown JSON fields instead of crashing when the frontend sends extra data.

---

## 📂 Package 2: `controller/` — The API Endpoints

Each controller file exposes REST API routes. The mobile app and web app send HTTP requests to these routes.

### 1. `ExpenseController.java`
**Routes:** `GET/POST/PUT/DELETE /api/expenses`
**What it does:** Full CRUD operations for user expenses. Supports bulk sync from the mobile app's offline queue.
**Key functions:**
- `createExpense()` — Saves a new expense, sends a push notification, and invalidates the forecast cache.
- `bulkSync()` — Receives an array of offline expenses from Android and saves them all at once.
- `getExpenses()` — Returns all expenses for the authenticated user, filtered by `userId`.

### 2. `IncomeController.java`
**Routes:** `GET/POST/PUT/DELETE /api/incomes`
**What it does:** CRUD operations for income records. Similar structure to ExpenseController.
**Key logic:** When income is added or updated, it invalidates the forecast cache so the AI recalculates.

### 3. `BillController.java`
**Routes:** `GET/POST/PUT/DELETE /api/bills`
**What it does:** Manages bill tracking (e.g., rent, Netflix, electricity).
**Key logic — Recurring Bills:** When a bill is marked as "Paid" and has `isRecurring == true`, the controller automatically generates a clone of the bill with the due date pushed forward by exactly 1 month.
**Key function:** `updateBill()` — Contains the recurring bill automation logic.

### 4. `GoalController.java`
**Routes:** `GET/POST/PUT/DELETE /api/goals`
**What it does:** Manages savings goals. Users can set a target amount and deadline, and contribute money towards it over time.
**Key function:** `contributeToGoal()` — Adds money to the goal's `currentAmount` and checks if the target has been reached.

### 5. `BudgetController.java`
**Routes:** `GET/POST/PUT/DELETE /api/budgets`
**What it does:** Manages monthly budget limits per category (e.g., "Food: LKR 15,000/month").
**Key logic:** The SmartAlertJob reads these limits to trigger push notifications when users exceed 80% or 100% of a category budget.

### 6. `ChatController.java`
**Routes:** `POST /api/chat`
**What it does:** Powers the AI Chatbot. It receives the user's message, queries the database for their real financial data (expenses, incomes, budgets, bills), builds a detailed context prompt, and sends it to Google Gemini.
**Key function:** `buildFinancialContext(userId)` — Queries 4 different database tables and formats the user's entire financial profile into a text string that Gemini can understand.

### 7. `ForecastController.java`
**Routes:** `GET /api/forecast`
**What it does:** Returns the AI-predicted expense for next month. Delegates all logic to `ForecastService`.
**Key logic:** Uses a caching strategy — if a fresh forecast already exists in the database, it returns the cached value instead of calling the expensive Python ML server again.

### 8. `UserController.java`
**Routes:** `GET/PUT /api/users`
**What it does:** Manages user profiles (display name, email, currency preference, FCM token for push notifications).
**Key function:** `updateFcmToken()` — The mobile app calls this on login to register its push notification device token.

### 9. `NotificationController.java`
**Routes:** `GET /api/notifications`
**What it does:** Returns the notification history for a user (all past push notifications they received). Supports marking notifications as read.

### 10. `AuditController.java`
**Routes:** `GET /api/audit`
**What it does:** Returns a complete audit trail log. Every create/update/delete action across the entire app is logged here with timestamps for security and compliance.

### 11. `AIApiController.java`
**Routes:** `POST /api/ai/receipt`
**What it does:** Receives a Base64-encoded receipt image from the mobile app, sends it to Gemini's Vision API, and returns the extracted amount, date, and category.

### 12. `HealthController.java`
**Routes:** `GET /health`
**What it does:** A simple endpoint that returns `{"status":"UP"}`. It is pinged every 5 minutes by cron-job.org to prevent the free Render server from going to sleep.

---

## 📂 Package 3: `entity/` — The Database Tables

Each entity file maps directly to a PostgreSQL database table using JPA annotations.

| Entity File | Database Table | Key Columns |
|---|---|---|
| `UserEntity.java` | `users` | id, displayName, email, fcmToken, currency |
| `ExpenseEntity.java` | `expenses` | id, userId, amount, category, date, description, isSynced |
| `IncomeEntity.java` | `incomes` | id, userId, amount, source, date |
| `BillEntity.java` | `bills` | id, userId, name, amount, dueDate, status, isRecurring |
| `GoalEntity.java` | `goals` | id, userId, name, targetAmount, currentAmount, targetDate |
| `BudgetLimitEntity.java` | `budget_limits` | id, userId, category, limitAmount, monthYear |
| `ForecastEntity.java` | `forecasts` | id, userId, forecastMonth, predictedExpense, isFallback |
| `NotificationEntity.java` | `notifications` | id, userId, title, message, type, isRead |
| `AuditLogEntity.java` | `audit_logs` | id, userId, entityType, action, entityId, details, timestamp |

---

## 📂 Package 4: `repository/` — The Database Queries

Each repository extends Spring Data JPA's `JpaRepository` interface, which auto-generates SQL queries.

| Repository | Key Custom Queries |
|---|---|
| `ExpenseRepository.java` | `findAllByUserId()`, `sumByCategoryAndDateBetween()`, `sumExpensesGroupedByUser()` |
| `IncomeRepository.java` | `findAllByUserId()` |
| `BillRepository.java` | `findByStatusIgnoreCaseAndIsDeletedFalseAndDueDateBetween()` |
| `GoalRepository.java` | `findByIsDeletedFalseAndTargetDateBetween()` |
| `BudgetLimitRepository.java` | `findByMonthYear()` |
| `ForecastRepository.java` | `findByUserIdAndForecastMonth()` |
| `UserRepository.java` | `findById()`, `existsById()` |
| `NotificationRepository.java` | `findByUserIdOrderByCreatedAtDesc()` |
| `AuditLogRepository.java` | `findByUserIdOrderByTimestampDesc()` |

---

## 📂 Package 5: `service/` — The Business Logic Brain

### 1. `ForecastService.java` (319 lines — The most complex file)
**What it does:** The central brain of the ML pipeline on the Java side. It owns all forecasting logic.
**Key process:**
1. Fetches user's expenses and groups them into monthly totals.
2. Applies **Bayesian Smoothing** to the current month's partial data to project the full-month pace.
3. Builds a 7-item array (6 historical months + 1 projected month).
4. Sends the array to the Python ML server via `MLServiceClient`.
5. Caches the result in the `ForecastEntity` table.
**Key function:** `getOrGenerateForecast(userId)` — The single source of truth for both `ForecastController` and `ChatController`.

### 2. `MLServiceClient.java`
**What it does:** The secure HTTP bridge between the Java backend and the Python FastAPI ML server.
**Key functions:**
- `generateForecast()` — Sends historical expense data to `/api/ml/forecast` and returns the predicted value.
- `getColdStartProfile()` — Sends age/income/savings to `/api/ml/cold_start` and returns the K-Means cluster assignment.
**Security:** Attaches a secret `x-api-key` header to every request so the Python server rejects unauthorized access.

### 3. `GeminiService.java`
**What it does:** Handles all communication with Google's Gemini AI API.
**Key functions:**
- `generateChatResponse(prompt)` — Sends a text prompt to Gemini and returns the AI's reply. Includes retry logic with exponential backoff (up to 4 attempts) and strict network timeouts (5s connect, 10s read).
- `autoCategorize(description)` — Sends an expense description to Gemini and asks it to classify it into one of 10 budget categories.

### 4. `ReceiptParserService.java`
**What it does:** Powers the OCR receipt scanning feature. Takes a Base64 image, sends it to Gemini's Vision API with a structured prompt, and parses the JSON response to extract amount, date, and category.
**Key function:** `parseReceipt(base64Image)` — Constructs a multimodal prompt (text + image) for Gemini's vision model.

### 5. `NotificationService.java`
**What it does:** Sends Firebase Cloud Messaging (FCM) push notifications to users' Android devices.
**Key function:** `sendPushNotification(token, title, body, userId, type)` — Builds a `Message` object using Firebase Admin SDK and fires it. Also saves the notification to the database for history.

### 6. `SmartAlertJob.java` (4 Scheduled Cron Jobs)
**What it does:** Contains 4 automated background tasks that run on a schedule:
- `checkBudgetsAndNotify()` — Runs daily at 9 AM. Checks if users have exceeded 80% or 100% of any budget category.
- `weeklyExpensesSummary()` — Runs every Monday at 8 AM. Sends a weekly spending summary notification.
- `checkDueBills()` — Runs daily at 10 AM. Warns users about bills due within 3 days.
- `checkGoalReminders()` — Runs daily at 9:30 AM. Reminds users about savings goals ending within 7 days.

### 7. `NotificationCronJob.java`
**What it does:** A secondary daily cron job (runs at 9 AM) that checks for upcoming bill due dates and savings goal deadlines, sending reminder push notifications.

### 8. `AuditService.java`
**What it does:** A simple logging utility. Every controller calls `auditService.logAction()` after creating, updating, or deleting any record to build a complete audit trail.
**Key function:** `logAction(userId, entityType, action, entityId, details)` — Saves an `AuditLogEntity` with a timestamp.

---

# PART 2: THE PYTHON ML MICROSERVICE (FastAPI)

All files are in the `ml/` folder.

### 1. `main.py` — The Production Server
**What it does:** The FastAPI application that runs 24/7 on Render. It loads 3 AI models on startup:
- **Chronos-T5 Tiny** (Amazon's time-series transformer for expense forecasting)
- **K-Means Cold Start Model** (For new user profiling)
- **Gemini API** (For NLP chat)

**Key endpoints:**
| Endpoint | What It Does |
|---|---|
| `GET /health` | Returns healthy status (for cron keepalive) |
| `POST /api/ml/forecast` | Receives monthly expense array, runs PyTorch Chronos, returns predicted next month |
| `POST /api/ml/cold_start` | Receives age/income/savings, runs K-Means, returns cluster name and budget recommendations |

**Security:** Every endpoint (except `/health`) requires an `x-api-key` header. Without it, the server returns `403 Forbidden`.

### 2. `train_kmeans.py` — The AI Training Script
**What it does:** Generates 1,000 synthetic Sri Lankan user profiles (age 18-68, income 30k-500k LKR using Lognormal Distribution), scales them with `StandardScaler`, trains a K-Means model with 4 clusters, sorts clusters by income, and exports 3 `.pkl` files.

### 3. `requirements.txt` — Python Dependencies
Lists all required libraries: `fastapi`, `uvicorn`, `torch`, `scikit-learn`, `chronos`, `joblib`, `pandas`, `numpy`.

### 4. `Dockerfile` — Cloud Deployment Blueprint
Tells Render how to build the Python container: install dependencies, copy code, expose port, and start the Uvicorn server.

### 5. `download_models.py` — Model Pre-Downloader
Pre-downloads the Chronos-T5 weights from HuggingFace so the server boots faster on restarts.

### 6. `test_distribution.py` — Math Verification Script
A small script to verify that the Lognormal salary distribution produces realistic, right-skewed data before training.

### 7. `colab_finetune_classifier.py` & `colab_forecast_chronos.py`
Heavy training scripts designed for Google Colab (free cloud GPU). Used during development for GPU-intensive model experiments.

### 8. `finetune google colab.ipynb`
Jupyter Notebook version of the Colab scripts with interactive visualizations.

### 9. `test_kmeans_endpoint.py` & `test_ml.py`
Manual debug scripts used during development to quickly test ML endpoints.

### 10. `tests/test_main.py` — Automated Unit Tests (7 tests)
Uses Pytest + FastAPI TestClient to verify: health check, API key security, empty data fallback, insufficient data fallback, zeros-only fallback, chat endpoint, and invalid K-Means input handling.

---

# PART 3: THE REACT WEB DASHBOARD

All files are in the `web/src/` folder.

## Core Architecture Files

### 1. `main.jsx` — The Entry Point
Renders the root `<App />` component into the DOM.

### 2. `App.jsx` — The Router & Layout
**What it does:** Wraps the entire app in `AuthProvider`, `ThemeProvider`, and `LanguageProvider` contexts. Defines all routes using React Router:
- `/login` — Login page
- `/` — Dashboard
- `/reports` — Reports page
- `/chatbot` — AI Chatbot
- `/ml-insights` — ML Forecast Charts
- `/profile` — User Profile
- `/learn` — Financial Education
- `/blogs` — Blog Articles

Uses `PrivateRoute` to protect every route except `/login`.

### 3. `api/apiClient.js` — The Secure HTTP Client
**What it does:** Creates an Axios instance with two interceptors:
- **Request Interceptor:** Automatically attaches the Firebase JWT token and `X-User-Id` header to every outgoing request.
- **Response Interceptor:** If the backend returns `401 Unauthorized`, it auto-signs out the user and redirects to login.

### 4. `services/FinanceService.js` — API Helper Functions
**What it does:** Wraps common API calls (like fetching expenses, incomes, bills) into reusable functions.

## Context Providers (Global State)

### 5. `context/AuthContext.jsx`
**What it does:** Manages the Firebase authentication state. Provides `currentUser`, `login()`, `loginWithGoogle()`, and `logout()` functions to every component.

### 6. `context/ThemeContext.jsx`
**What it does:** Manages Dark Mode / Light Mode toggle. Stores the preference in `localStorage` so it persists across sessions.

### 7. `context/LanguageContext.jsx`
**What it does:** Manages multi-language support (English/Sinhala). Stores the selected language in `localStorage`.

## Pages (The UI Screens)

### 8. `pages/Login.jsx`
**What it does:** The authentication gate. Supports Google Sign-In only. Enforces **Mobile-First Registration** — if a brand new user tries to log in via Google, it detects `isNewUser` from `getAdditionalUserInfo()`, immediately deletes the Firebase account, and shows an error message telling them to register on the mobile app first.

### 9. `pages/Dashboard.jsx`
**What it does:** The main homepage. Displays summary cards for total income, total expenses, and net balance. Shows recent transactions, a monthly expense chart (using Recharts), and quick-action buttons.
**Key logic:** Fetches data from `/api/expenses`, `/api/incomes`, `/api/bills` on mount and calculates totals.

### 10. `pages/Reports.jsx`
**What it does:** Advanced financial analytics page. Shows expense breakdowns by category (pie charts), monthly spending trends (bar charts), and comparative analysis.

### 11. `pages/MLInsights.jsx`
**What it does:** Displays the AI forecast chart. Shows the user's historical monthly spending trend and the Chronos-T5 predicted value for next month, plotted on a line chart.
**Key logic:** Fetches data from `/api/forecast` and renders it using Recharts.

### 12. `pages/Chatbot.jsx`
**What it does:** A real-time chat interface with DreamSaver AI. Messages are sent to `/api/chat`, which queries the user's database and feeds context to Google Gemini.
**Key logic:** Manages a `messages` state array. Each message has `sender: 'user'` or `sender: 'bot'`. Auto-scrolls to the bottom on new messages.

### 13. `pages/Profile.jsx`
**What it does:** User profile management. Shows display name, email, profile picture, currency preference, and notification settings.

### 14. `pages/Learn.jsx`
**What it does:** A financial education hub with articles and tips on budgeting, saving, and investing.

### 15. `pages/Blogs.jsx`
**What it does:** A blog section with financial literacy content and community articles.

## Reusable Components

### 16. `components/Sidebar.jsx`
**What it does:** The dark sidebar navigation panel on the left side of the dashboard. Contains links to all pages, the theme toggle, and a logout button.

### 17. `components/common/UIStates.jsx`
**What it does:** Reusable loading spinner, error state, and empty state components. Used across all pages for consistent UX when data is loading or unavailable.

## Styling

### 18. `index.css`
**What it does:** The global CSS design system. Contains CSS custom properties (variables) for colors, spacing, and typography. Includes the complete dark mode theme, glassmorphism effects, and responsive breakpoints.

---

# PART 4: QUICK-FIRE VIVA Q&A

**Q: How many database tables does your system have?**
A: 9 tables — Users, Expenses, Incomes, Bills, Goals, BudgetLimits, Forecasts, Notifications, AuditLogs.

**Q: How many API endpoints does your backend expose?**
A: Over 30 RESTful endpoints across 12 controllers.

**Q: How many AI/ML models does your system use?**
A: 3 models — Chronos-T5 (forecasting), K-Means (user profiling), and Gemini (chatbot + OCR + categorization).

**Q: How do you prevent unauthorized access?**
A: Triple-layer security — Firebase JWT verification (FirebaseAuthFilter), Secret API key for ML service (x-api-key header), and user-scoped database queries (every query filters by userId).

**Q: What happens if the ML server goes down?**
A: The Java backend catches the exception in MLServiceClient and returns a graceful fallback. The app continues to function for all non-ML features.

**Q: How does the offline sync work?**
A: Android Room saves expenses locally with `isSynced=false`. WorkManager triggers PendingSyncWorker which sends a bulk POST to the Java backend when internet is restored. On success, it marks records as synced.

**Q: Why did you separate Java and Python into two servers?**
A: Microservice architecture — Java excels at enterprise security and relational databases. Python excels at ML/AI. Separating them ensures fault tolerance (ML crash doesn't kill the app) and independent scalability.

---

# PART 5: THE ANDROID MOBILE APP (Java)

The mobile app lives in `app/src/main/java/com/team/financeapp/` and is structured using a robust pattern incorporating Room (local database), Retrofit (API calls), and WorkManager (background sync).

## 📂 `auth/` — Authentication
- **`AuthManager.java`**: The central controller for Firebase Authentication. Handles login, registration, and generating the critical JWT tokens for the Java backend.
- **`UserProfile.java`**: A local data model for storing basic user profile information.

## 📂 `data/local/` — The Offline Database (Room)
- **`AppDatabase.java`**: The core Room database configuration.
- **`dao/` (Data Access Objects)**: Contains interfaces (`ExpenseDao`, `IncomeDao`, `BillDao`, `GoalDao`, `BudgetDao`) that write standard SQL queries (e.g., `@Insert`, `@Query`) to save data onto the phone's physical storage for offline use.
- **`entity/`**: The local representations of your database tables.

## 📂 `data/remote/` — The Cloud API (Retrofit)
- **`ApiClient.java`**: Instantiates Retrofit. Contains an interceptor (just like the React web app) that attaches the Firebase `Authorization` token to every request leaving the phone.
- **`ApiService` interfaces**: (`ExpenseApiService`, `BillApiService`, etc.) Define the GET/POST/PUT/DELETE routes that match your Spring Boot backend endpoints.

## 📂 `data/repository/` — The Single Source of Truth
- Contains files like `ExpenseRepository.java`, `BillRepository.java`, etc.
- **The Logic:** When the UI asks for data, the repository decides whether to fetch it from the Local Room DB (if offline) or the Remote Retrofit API (if online). This is the key to the **Offline-First Architecture**.

## 📂 `data/sync/` — Background Offline Sync
- **`PendingSyncWorker.java`**: A Google `WorkManager` background job. If the user creates an expense while deep in the jungle with no 4G, it saves to Room with `isSynced=false`. When the phone reconnects to WiFi, this Worker wakes up in the background and POSTs all unsynced expenses to the Spring Boot backend.

## 📂 `chatbot/` & `forecast/` — The ML Interfaces
- **`ChatbotActivity.java` & `ChatAdapter.java`**: The UI for talking to DreamSaver AI. Connects to `/api/chat`.
- **`ForecastActivity.java`**: Displays the line chart using the `MPAndroidChart` library, plotting historical data against the Chronos-T5 AI prediction.

## 📱 Key Activities (UI Screens)
- **`DashboardActivity.java`**: The complex homepage. Manages the BottomNavigationView, loads charts (via `ChartHelper`), and displays summary cards.
- **`AddExpenseActivity.java`**: Handles manual entry, but also incorporates the **OCR Camera Logic**. It captures a photo, converts it to Base64, sends it to the backend `AIApiController`, and autofills the form.
- **`AppLockActivity.java` & `AppLockManager.java`**: The biometric/PIN security layer that blocks unauthorized users from opening the app on the phone.
- **`BudgetActivity.java` & `GoalsActivity.java`**: Interfaces for setting limits and targets.

## 📂 `utils/` — Helper Classes
- **`ChartHelper.java`**: A massive utility file that prevents code duplication by standardizing how Bar Charts and Line Charts are drawn using MPAndroidChart across the app.
- **`DateUtils.java`**: Formats timestamps into human-readable strings (e.g., "Aug 15, 2026").
- **`ThemePreferenceManager.java`**: Manages the user's Dark Mode / Light Mode settings using Android's `SharedPreferences`.
