# Final Project Presentation Preparation Plan

Date: April 1, 2026
Presentation date: Sunday (5th)
Project: Personal Finance Management App

This plan is designed to help the team prepare for:
- Project Presentation
- Viva / Question Session
- Code Test / Code Explanation

## 1) Goal

By presentation day, the team should be able to:
1. Clearly explain the problem and solution
2. Confidently demo key features without failure
3. Defend architecture and implementation choices
4. Explain each member's contribution with evidence
5. Answer technical and viva questions with clarity

## 2) Preparation Timeline (Apr 1 -> Apr 5)

### Apr 1 (Today) - Foundation
- Finalize project status and architecture notes
- Freeze core demo flow (only critical fixes after this)
- Assign presentation and viva ownership per member
- Start slide draft

### Apr 2 - Demo Stability Day
- Run full app test on real device(s)
- Verify login, CRUD, edit/delete, dashboard, goals
- Record backup demo video
- Create fallback plan (APK + video + screenshots)

### Apr 3 - Content & Story Day
- Finalize all slides
- Prepare architecture diagram and data flow explanation
- Write challenge/solution section with real examples
- Prepare member contribution matrix

### Apr 4 - Rehearsal Day
- Full timed rehearsal (2 rounds)
- Mock viva with difficult questions
- Code walkthrough rehearsal (each member)
- Final polish of slide visuals and speaking notes

### Apr 5 - Presentation Day Checklist
- Verify app runs before leaving
- Carry APK, source code, slides, demo video
- Arrive early and check display/projector compatibility

## 3) Slide Plan (Recommended 10-12 slides)

1. Title slide
- Project name
- Team members
- Module/theme

2. Problem statement
- What user pain this app solves
- Why it matters

3. Proposed solution
- What your app does at a high level
- Core idea: local-first finance management with cloud sync

4. Main features
- Auth (email + Google + password flows)
- Expenses, Bills, Income, Goals CRUD
- Dashboard insights and summaries

5. App flow
- Home -> Auth -> Dashboard -> Modules
- Navigation approach

6. Backend architecture
- Room + Repository + Firestore + WorkManager sync
- Sync states: PENDING, SYNCED, FAILED

7. Technology stack
- Android Java, XML, Material 3
- Firebase Auth, Firestore
- Room, WorkManager

8. Team contributions
- Member-wise responsibilities and outcomes

9. Challenges and solutions
- Merge/API mismatch resolution
- Sync reliability implementation
- Calendar visibility fix
- UI/feature regression fixes

10. Current outcome
- What is completed
- What remains (honest and clear)

11. Future improvements
- Notification backend integration
- Stronger automated tests
- Summary pipeline hardening

12. Thank you / Q&A

## 4) Demo Plan (5-7 minutes)

Use this exact order:
1. Launch app and login
2. Add expense
3. Add income
4. Add bill
5. Add goal
6. Edit/Delete one record (show restored feature)
7. Show dashboard updates
8. Briefly explain offline-first + sync retry concept

Demo rules:
- No random navigation
- Keep one happy-path flow
- Use prepared test account and predictable data

## 5) Viva Preparation Plan

### Common questions to prepare
1. What problem does this app solve?
2. Why Room + Firebase together?
3. How does offline sync work?
4. How do you handle failures and retries?
5. How is user data secured?
6. What was your biggest challenge?
7. What is incomplete and why?
8. If you had more time, what next?

### Answer style (recommended)
- 1 sentence context
- 2-3 sentence technical explanation
- 1 sentence outcome/benefit

## 6) Code Explanation Plan

Each member must prepare 3 files:
1. One UI/activity file
2. One repository/data file
3. One infrastructure file (Auth/Room/Worker)

For each file, explain:
- Purpose of this file
- Main methods and responsibilities
- Error handling and edge cases
- How it connects with other layers

## 7) Team Contribution Matrix Template

Use this in slides:

- Member A
  - Module ownership
  - Key files
  - Key completed outcomes

- Member B
  - Module ownership
  - Key files
  - Key completed outcomes

- Member C
  - Module ownership
  - Key files
  - Key completed outcomes

(Keep it evidence-based, not generic.)

## 8) Deliverables to keep ready

1. Slide deck (PPT + PDF)
2. Latest APK
3. Source code on latest branch
4. Demo backup video
5. Screenshot pack (for feature proof)
6. Architecture notes document
7. Status/plan document

## 9) Final QA Checklist (Must pass before presentation)

### Functional
- Login works
- Register works
- Add/Edit/Delete: expenses, bills, incomes, goals
- Dashboard values update correctly
- Navigation works without crash

### Backend
- Local save works when network is unstable
- Sync worker is enabled
- Firestore rules are present and valid

### Presentation
- Slides complete and readable
- Contribution slide finalized
- Viva answers rehearsed
- Code walkthrough practiced

## 10) Risk Management (Presentation Day)

Potential issue -> Backup action:
- Device/network issue -> Play demo video
- Login issue -> Use pre-logged-in test account
- Build issue -> Use prebuilt tested APK
- Display issue -> Use PDF version of slides

## 11) Speaking Structure (for confident delivery)

For each section:
1. What
2. Why
3. How
4. Result

Avoid reading slides directly. Speak to outcome and architecture.

## 12) Quick Success Criteria

Presentation considered strong if:
- Team can explain problem and architecture clearly
- Live demo runs cleanly
- Each member explains own code confidently
- Questions are answered honestly and technically
- Limitations and next steps are clearly communicated

---

## Optional Add-ons (if time permits)
- One-slide architecture diagram image
- One-slide sync sequence diagram
- One-page viva cheat sheet per member
