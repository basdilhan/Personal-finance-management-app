# DreamSaver - Ultimate Personal Finance Ecosystem

**DreamSaver** is an enterprise-grade, multi-platform personal finance ecosystem. It seamlessly bridges the gap between on-the-go mobile expense tracking and deep, desktop-class financial analytics.

By separating the architecture into **Four Core Pillars**, the project ensures high scalability, security, and the ability to leverage state-of-the-art Artificial Intelligence without compromising the speed of the user interface.

---

## 🏛️ The Four Core Pillars of Architecture

### 1. 📱 Android Mobile Application (The "On-The-Go" Client)
Built natively for Android, this application is designed for speed and convenience when logging daily expenses.
* **Offline-First Sync Engine:** Uses Android Room (SQLite) to store data locally. If you lose internet connection, you can still add expenses. A background `WorkManager` automatically syncs data to the cloud the moment internet is restored.
* **Google ML Kit OCR (Smart Receipt Scanner):** Instead of typing out expenses, users can snap a photo of a receipt. The app uses on-device Machine Learning (Google ML Kit Vision) to extract the transaction text automatically.
* **Biometric Security:** Enterprise-grade security integrating native Android Fingerprint/FaceID to lock the app from unauthorized users.
* **Native AI Chatbot:** A sleek, conversational interface that communicates with the backend AI to answer financial queries.

### 2. 💻 Web Application (The "Analytics" Dashboard)
A premium, desktop-class web dashboard built with React and Vite for deep financial analysis.
* **Aurora Glass UI:** A stunning, custom-built glassmorphism design system featuring interactive micro-animations, vibrant gradients, and a modern dark mode aesthetic.
* **AI Insights Dashboard:** Interactive charts (built with `Recharts`) displaying Time-Series Expense Forecasts, and a Smart Budget Profiling tool.
* **Automated PDF Exporting:** Generates professional, downloadable financial reports instantly using `jspdf`.
* **Centralized Firebase Auth:** Secure web authentication that perfectly syncs user states with the Spring Boot backend.

### 3. ⚙️ Spring Boot Backend (The "Central Nervous System")
A robust, secure Java Spring Boot backend deployed to the cloud (Render), acting as the bridge between all clients and databases.
* **PostgreSQL Database:** A highly structured relational database storing all user data, expenses, incomes, goals, and upcoming bills.
* **Custom Security JWT Filter:** Every API request from the Web or Mobile app is intercepted and verified against Firebase Auth tokens to ensure 100% data privacy.
* **REST API:** Handles all CRUD operations, offline-sync conflict resolution, and data aggregation.
* **ML Proxy:** Securely routes complex AI requests to the isolated Python ML microservice to prevent the main Java server from crashing under heavy mathematical loads.

### 4. 🧠 Python FastAPI Backend (The "AI Brain")
A dedicated Machine Learning microservice built to handle the heavy mathematical lifting for the ecosystem.
* **HuggingFace NLP Classification:** Automatically categorizes transaction descriptions (e.g., "Dialog Bill" -> "Mobile & Internet") using a fine-tuned Transformer model.
* **Amazon Chronos Forecasting:** Utilizes `amazon/chronos-t5-tiny` (a zero-shot time-series language model) to predict future expenses based on the mathematical shape of historical spending curves.
* **K-Means Budget Profiling:** Uses Scikit-Learn clustering to solve the "Cold Start Problem." It groups new users by Age, Income, and Savings Goal to recommend dynamic, highly personalized budget percentages.

---

## 🚀 Complete Setup & Installation Guide

To run the entire ecosystem locally, you will need to start the 3 main services (Backend, ML Brain, and Web App).

### 🛠️ Prerequisites
- JDK 11 or higher
- Node.js v24 or higher
- Python 3.10 or higher
- PostgreSQL Server (Local) or a Cloud Database

### Step 1: Run the Spring Boot Backend
1. Open a terminal and navigate to the `backend` folder.
2. Ensure you have your `application.properties` set up with your Database URL and Firebase credentials.
3. Run the server:
   ```bash
   ./gradlew bootRun
   ```
4. The server will start on `http://localhost:8080`.

### Step 2: Run the Python ML "Brain"
1. Open a new terminal and navigate to the `ml` folder.
2. Create a virtual environment and install dependencies:
   ```bash
   python -m venv venv
   venv\Scripts\activate      # Windows
   pip install -r requirements.txt
   ```
3. Run the FastAPI server:
   ```bash
   uvicorn main:app --reload
   ```
4. The ML Brain will start on `http://localhost:8000`.

### Step 3: Run the React Web App
1. Open a new terminal and navigate to the `web` folder.
2. Install the necessary node modules:
   ```bash
   npm install
   ```
3. Start the Vite development server:
   ```bash
   npm run dev
   ```
4. The Web App will launch on `http://localhost:5173`.

### Step 4: Run the Android App
1. Open the project root folder in **Android Studio**.
2. Wait for the Gradle sync to complete.
3. Click the green `Run` button at the top to launch the app on an Android Emulator or a physical device plugged into your computer via USB.

---

## 🎨 Technology Stack Summary

| Component | Technologies Used |
| :--- | :--- |
| **Mobile App** | Java, Android SDK (API 24-34), Material 3, SQLite (Room), Google ML Kit, Retrofit |
| **Web App** | React 19, Vite, Recharts, Lucide Icons, Vanilla CSS (Aurora Glass UI) |
| **Backend** | Java 17, Spring Boot 4.1.0, Spring Data JPA, PostgreSQL, Firebase Admin SDK |
| **AI / ML** | Python, FastAPI, PyTorch, Scikit-Learn, Amazon Chronos, HuggingFace Inference API |

---

## 🌿 Git Branching & Contribution Workflow

This project enforces strict version control practices. We use a **5-branch system** to prevent code conflicts:

1. `main` → The stable release branch (Production-ready only).
2. `dev` → The shared integration branch.
3. `ime`, `puli`, `sam`, `neth` → Personal feature branches for individual team members.

### ⚠️ Golden Rules of Committing
- **Never push directly to `main`!**
- **Never push directly to `dev`!**
- Always work on your personal branch (e.g., `git checkout sam`).
- When your feature is complete, open a **Pull Request (PR)** on GitHub from your personal branch into `dev`.
- Once tested on `dev`, the code is merged into `main` for release.

---
*DreamSaver was meticulously developed as an advanced coursework project to demonstrate enterprise full-stack architecture, offline-first mobile synchronization, and applied machine learning integration.*