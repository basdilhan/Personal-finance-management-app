# DreamSaver - AI-Powered Personal Finance Ecosystem

**DreamSaver** is a comprehensive, multi-platform personal finance management system. It has evolved from a simple Android skeleton into a full-scale microservice architecture featuring an Android App, a modern Web Dashboard, a robust Java Spring Boot backend, and a dedicated Python Machine Learning engine.

---

## 🌟 Ecosystem Architecture

### 1. 📱 Android Mobile Application (Java + XML)
The primary user interface for on-the-go finance management.
- **Offline-First Sync:** Uses SQLite (Room Database) for offline usage, with a background `WorkManager` that automatically syncs to the cloud when internet is restored.
- **Google ML Kit OCR:** Features a smart receipt scanner that uses the device's camera and Google ML Kit to extract transaction amounts and details automatically.
- **Biometric Security:** App lock functionality using native Android Biometrics (Fingerprint/FaceID).
- **AI Chatbot:** A native chat interface communicating with the backend's AI services.

### 2. 💻 Web Application (React + Vite)
A premium, desktop-class web dashboard built for detailed financial analysis.
- **Aurora Glass UI:** A custom-built, modern glassmorphism design system featuring dynamic charts and smooth micro-animations.
- **AI Insights Dashboard:** Interactive tools to generate Smart Budget Profiles (K-Means) and view Time-Series Expense Forecasts (Chronos).
- **PDF Exporting:** Generates professional, downloadable financial reports using `jspdf`.
- **Firebase Auth:** Secure web authentication synchronized with the Spring Boot backend.

### 3. ⚙️ Core Backend (Java Spring Boot)
The central nervous system of the application, deployed on Render.
- **PostgreSQL Database:** Relational database storing users, expenses, incomes, goals, and bills.
- **Security:** Custom JWT filter that verifies Firebase Auth tokens on every request.
- **REST API:** Handles all CRUD operations, offline-sync conflict resolution, and data aggregation for the frontend clients.
- **ML Proxy:** Securely routes AI requests to the Python ML microservice.

### 4. 🧠 AI "Brain" (Python FastAPI)
A dedicated Machine Learning microservice built to handle heavy mathematical computations.
- **HuggingFace NLP:** Automatically categorizes transaction descriptions (e.g. "Dialog Bill" -> "Mobile & Internet") using a fine-tuned Transformer model.
- **Amazon Chronos Forecasting:** Utilizes `amazon/chronos-t5-tiny` (a zero-shot time-series language model) to predict future expenses based on historical spending curves.
- **K-Means Budget Profiling:** Uses Scikit-Learn clustering to solve the "Cold Start Problem". It groups new users by Age, Income, and Savings Goal to recommend highly personalized budget percentages.

---

## 🚀 How to Run Locally

### Prerequisites
- JDK 11+
- Node.js v24+
- Python 3.10+
- PostgreSQL (or use Render cloud DB)

### Running the Spring Boot Backend
```bash
cd backend
./mvnw spring-boot:run
# Runs on http://localhost:8080
```

### Running the Python ML Backend
```bash
cd ml
python -m venv venv
venv\Scripts\activate  # (Windows)
pip install -r requirements.txt
uvicorn main:app --reload
# Runs on http://localhost:8000
```

### Running the React Web App
```bash
cd web
npm install
npm run dev
# Runs on http://localhost:5173
```

### Running the Android App
Open the project root in **Android Studio**, wait for Gradle to sync, and press `Run` to launch on an emulator or physical device.

---

## 🎨 Design & Technologies
- **Mobile:** Java, Android SDK (API 24-34), Material 3, SQLite (Room), Google ML Kit, Retrofit
- **Web:** React 19, Vite, Recharts, Lucide Icons, Vanilla CSS (Aurora Glass)
- **Backend:** Java 17, Spring Boot 3, Spring Data JPA, PostgreSQL, Firebase Admin SDK
- **AI/ML:** Python, FastAPI, PyTorch, Scikit-Learn, Amazon Chronos, HuggingFace Inference API

---

## 🌿 Git Branching Workflow
We use a **5-branch system**:
- `main` → Stable release branch (Production)
- `dev` → Shared integration branch
- `ime`, `puli`, `sam`, `neth` → Personal feature branches

**Always work on your personal branch and create Pull Requests (PRs) into `dev`. Never push directly to `main`!**

---
*Developed as an advanced coursework project demonstrating full-stack architecture, offline-first mobile sync, and applied machine learning.*