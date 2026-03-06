# DreamSaver - Personal Finance, Bill Reminder & Savings Goal App

**Android Mobile Application** - Java + XML Layouts

---

## 📱 Project Overview

A beginner-friendly Android app for managing personal finances, bill reminders, and savings goals. This project demonstrates fundamental Android development concepts using Java and XML layouts (no Jetpack Compose).

### Current Status: ✅ Foundation & Basic Screens Complete

---

## 🎯 Features Implemented

### 1. **Home/Welcome Screen** (`HomeActivity`)
- App introduction and feature overview
- Beautiful gradient design with app icon
- "Get Started" button to navigate to login

### 2. **Login Screen** (`LoginActivity`)
- Email and password input fields with Material Design
- Client-side validation:
  - Empty field checking
  - Email format validation
- Toast messages for user feedback
- Forgot password and register links (UI only)
- Navigates to Dashboard on successful "login"

### 3. **Dashboard Screen** (`DashboardActivity`)
- Welcome message
- Placeholder for future features
- Logout button (returns to login)
- Clean card-based layout

---

## 🛠️ Technical Specifications

- **Language**: Java
- **UI**: XML Layouts
- **Min SDK**: API 24 (Android 7.0)
- **Target SDK**: API 34 (Android 14)
- **Architecture**: Simple Activity-based (beginner-friendly)
- **Design**: Material Design 3 components
- **Build System**: Gradle with Kotlin DSL

### Dependencies
- AndroidX AppCompat
- Material Components (Material 3)
- ConstraintLayout
- CardView

---

## 📁 Project Structure

```
app/src/main/
├── java/com/team/financeapp/
│   ├── HomeActivity.java          # Welcome/landing screen
│   ├── LoginActivity.java         # Login with validation
│   └── DashboardActivity.java     # Main dashboard
│
├── res/
│   ├── layout/
│   │   ├── activity_home.xml      # Home screen layout
│   │   ├── activity_login.xml     # Login screen layout
│   │   └── activity_dashboard.xml # Dashboard layout
│   │
│   ├── drawable/
│   │   ├── ic_finance.xml         # App icon
│   │   ├── button_gradient.xml    # Gradient button background
│   │   ├── rounded_background.xml # Rounded corners
│   │   └── card_background.xml    # Card backgrounds
│   │
│   ├── values/
│   │   ├── colors.xml             # Color palette
│   │   ├── strings.xml            # All text strings
│   │   └── themes.xml             # Material 3 theme
│   │
│   └── mipmap/                    # App launcher icons
│
└── AndroidManifest.xml            # App configuration
```

---

## 🚀 How to Run

### Option 1: Using the Batch File (Recommended)
1. Open Command Prompt or PowerShell
2. Navigate to project directory
3. Run: `launch-app.bat`
4. Wait for emulator to boot (~70 seconds)
5. App will install and launch automatically

### Option 2: Manual Steps
1. Start Android Emulator:
   ```batch
   cd C:\Users\samudu\AppData\Local\Android\Sdk\emulator
   emulator.exe -avd Pixel_9_Pro_2
   ```

2. Build the project:
   ```batch
   cd "C:\HND note & projcts\Mobile\Mobile CW\Personal-finance-management-app"
   gradlew.bat assembleDebug
   ```

3. Install the APK:
   ```batch
   adb install -r app\build\outputs\apk\debug\app-debug.apk
   ```

4. Launch the app:
   ```batch
   adb shell am start -n com.team.financeapp/.HomeActivity
   ```

### Option 3: Using Android Studio
1. Open project in Android Studio
2. Wait for Gradle sync
3. Click the green "Run" button
4. Select your emulator or connected device

---

## 🎨 Design & Colors

### Color Palette
- **Primary**: `#1E88E5` (Blue)
- **Primary Dark**: `#1565C0`
- **Accent**: `#00897B` (Teal)
- **Background**: `#F5F5F5` (Light Gray)
- **Success**: `#4CAF50` (Green)
- **Error**: `#EF5350` (Red)

### UI Components
- Material Design 3 components
- Custom gradient buttons
- Rounded corners and elevation
- Card-based layouts

---

## ✅ Build Status

**Last Build**: February 1, 2026
- ✅ Build Successful
- ✅ APK Generated: `app-debug.apk` (5.7 MB)
- ✅ No compilation errors
- ⚠️ Minor warnings (lambda suggestions - safe to ignore)

---

## 📝 Notes for Beginners

### What Works Now:
- ✅ App launches with welcome screen
- ✅ Navigation between screens
- ✅ Email validation on login
- ✅ Toast messages for feedback
- ✅ Material Design components

### What's NOT Implemented (UI Only):
- ❌ Real authentication (no backend)
- ❌ Database storage
- ❌ Bill reminders functionality
- ❌ Savings goals tracking
- ❌ Expense tracking
- ❌ User registration

### Code is Beginner-Friendly:
- Detailed comments explaining each section
- Simple Activity-based navigation
- No complex architecture patterns
- Traditional Java (no lambdas in main code)

---

## 🔧 Requirements

- **JDK**: Version 11 or higher
- **Android SDK**: API 24+ (Android 7.0+)
- **Gradle**: 8.x (included via wrapper)
- **Emulator**: Any Android device/emulator running API 24+

---

## 🐛 Known Issues

1. **Windows Emulator Warning**: `UpdateLayeredWindowIndirect failed` - This is a harmless Windows graphics warning and doesn't affect functionality.

2. **JAVA_HOME**: Make sure JAVA_HOME is set correctly. The project uses:
   ```
   C:\Program Files\Eclipse Adoptium\jdk-11.0.28.6-hotspot
   ```

---

## 📚 Learning Resources

This project demonstrates:
- Android Activity lifecycle
- XML layout design
- Material Design implementation
- Intent-based navigation
- Input validation
- Toast messages
- Button click handling

---

## 👨‍💻 Development Info

**Project Type**: Educational/Coursework
**Development Environment**: VS Code with Android SDK
**Package Name**: `com.team.financeapp`
**Application ID**: `com.team.financeapp`

---

## 📄 License

Educational project for learning purposes.

---

**Happy Coding! 🚀**

## Branching workflow (Main + Dev + 4 personal branches)

We use **5 branches**:
- `main` → stable release branch (only tested code)
- `dev` → shared integration branch (everyone merges here)
- `ime`, `puli`, `sam`, `neth` → personal feature branches

### 1) One-time setup (already done)
These branches should already exist in the remote repository:
- `dev`
- `ime`
- `puli`
- `sam`
- `neth`

### 2) Daily work steps (for each person)
Always work on your **own branch** and never commit directly to `main` or `dev`.

**A. Get the latest `dev`:**
- Switch to `dev`
- Pull latest changes

**B. Update your branch from `dev`:**
- Switch to your branch (ime/puli/sam/neth)
- Merge or rebase from `dev`

**C. Make changes and commit:**
- Add files
- Commit with a clear message
- Push your branch to remote

**D. Create a Pull Request (PR):**
- Open a PR from your branch → `dev`
- Another team member reviews (if possible)
- Merge the PR into `dev`

### 3) When `dev` is stable
Create a PR from `dev` → `main` and merge after final testing.

---

## Example commands

### Update local `dev`
1. `git checkout dev`
2. `git pull`

### Work on your branch (example: ime)
1. `git checkout ime`
2. `git pull`
3. `git merge dev`
4. Make changes
5. `git add .`
6. `git commit -m "Describe your change"`
7. `git push`

### Create PR
Open GitHub and create a PR from your branch → `dev`.

---

## Rules (simple)
- **Never** push directly to `main`
- **Never** push directly to `dev` (use PR)
- Always sync with `dev` before starting new work
- Use clear commit messages