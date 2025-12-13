# Tiny Experiments - System Design & Implementation Plan

## Project Overview

**Goal:** Mobile app (Android-first) for running small habit experiments based on Anne-Laure Le Cunff's framework.

**Timeline:** 8-10 weeks (10-20 hours/week)
**Tech Stack:** Native Android (Kotlin), Firebase (Auth, Firestore, Cloud Messaging, Functions in Go)
**Monetization:** Free tier (3 active experiments), paid tier (unlimited)

---

## System Architecture

```
┌─────────────────────────────────────────┐
│  Native Android App (Kotlin)            │
│  - MVVM Architecture                    │
│  - Jetpack Compose UI                   │
│  - Experiment CRUD                      │
│  - Daily check-in interface             │
│  - Actionable push notifications        │
│  - Reflection forms                     │
└─────────────────────────────────────────┘
              ↓
┌─────────────────────────────────────────┐
│  Firebase Auth                          │
│  - Google OAuth                         │
│  - Email/Password                       │
└─────────────────────────────────────────┘
              ↓
┌─────────────────────────────────────────┐
│  Firestore (Direct Access)              │
│  - Experiments, check-ins, reflections  │
│  - Security Rules enforcement           │
└─────────────────────────────────────────┘
              ↓
┌─────────────────────────────────────────┐
│  Cloud Functions (Go)                   │
│  - Validate 3-experiment limit          │
│  - Send scheduled daily notifications   │
│  - Future: Payment processing           │
└─────────────────────────────────────────┘
              ↓
┌─────────────────────────────────────────┐
│  Firebase Cloud Messaging (FCM)         │
│  - Push notifications to Android        │
│  - Actionable buttons (Yes/No)          │
└─────────────────────────────────────────┘
```

---

## Data Model

### Firestore Structure

```
users/
  {userId}/
    profile/
      email: string
      tier: "free" | "paid"
      fcmToken: string                    // For push notifications
      notificationTime: string            // e.g., "09:00" - user's preferred time
      createdAt: timestamp

    experiments/
      {experimentId}/
        name: string
        description: string
        durationValue: number             // e.g., 7, 14, 30
        durationUnit: "days" | "weeks"
        startDate: timestamp
        status: "active" | "completed"
        createdAt: timestamp
        completedAt: timestamp | null

        checkIns/
          {checkInId}/                    // Format: YYYY-MM-DD
            completed: boolean            // true if user did the experiment
            timestamp: timestamp          // When check-in was recorded
            note: string | null           // Optional daily reflection

        reflections/
          {reflectionId}/
            content: string
            timestamp: timestamp
            isEndReflection: boolean      // true for end-of-experiment reflection
            nextAction: "continue" | "modify" | "end" | null
```

---

## Features & User Flows

### 1. Authentication
- **Sign Up:** Email/password or Google OAuth
- **Sign In:** Same methods
- **Onboarding:** Brief intro to Tiny Experiments concept (optional)

### 2. Create Experiment
**Flow:**
1. User taps "New Experiment"
2. Form fields:
   - Name (required)
   - Description (optional)
   - Duration: number input + unit selector (days/weeks)
   - Start date: date picker (default: today)
3. On submit → Call Cloud Function to validate limit
4. If allowed → Create in Firestore
5. If blocked → Show "Upgrade to add more experiments"

**Validation:**
- Free tier: max 3 active experiments
- Cloud Function checks count before creation

### 3. Dashboard / Home Screen
**Displays:**
- List of active experiments
- Progress indicators (days completed / total days)
- "Today's Check-ins" section at top
- Button to add new experiment

**Today's Check-ins:**
- Shows experiments that are active today
- Simple checkbox or Yes/No buttons
- Tap to mark as done
- Optional: Add quick note

### 4. Daily Check-ins

**Option A: In-App**
- User opens app
- Sees today's experiments
- Taps checkbox/button to record

**Option B: Push Notification (Primary)**
- Daily notification at user's preferred time
- Notification shows: "Did you do [Experiment Name]?"
- Actionable buttons: "Yes" | "No" | "Open App"
- Tapping Yes/No records check-in directly (no app open needed)
- Cloud Function handles notification action

**Data recorded:**
- Date (YYYY-MM-DD)
- Completed (boolean)
- Timestamp
- Optional note (if user opens app to add detail)

### 5. View Experiment Details
**Displays:**
- Experiment info (name, description, duration, progress)
- Calendar view or list of check-ins
- Streak counter (consecutive days completed)
- "Add Reflection" button
- "End Experiment" button (triggers end reflection)

### 6. Reflections

**Daily Reflection (Optional):**
- User can add reflection anytime
- Simple text input
- Stored with isEndReflection: false

**End Reflection (Required at experiment end):**
- Triggered when:
  - User manually taps "End Experiment"
  - OR automatically at end date
- Prompts:
  - "What did you learn?"
  - "What worked well?"
  - "What challenges did you face?"
- After submitting → Choose next action:
  - **Continue as is:** Mark as completed, optionally create new identical experiment
  - **Modify:** Create new experiment with pre-filled modified values
  - **End:** Mark as completed, done

### 7. Settings
- Notification time preference
- Account info
- Tier display (Free/Paid)
- (Future) Payment/upgrade option

---

## Technical Implementation Details

### Android Architecture & Libraries

**Architecture:**
- **MVVM** (Model-View-ViewModel)
- **Repository Pattern** for data layer
- **Jetpack Compose** for UI (modern declarative UI)
- **Coroutines** for asynchronous operations
- **Flow** for reactive data streams

**Core Android Jetpack Components:**
- `androidx.compose.*` - Jetpack Compose UI
- `androidx.navigation:navigation-compose` - Navigation
- `androidx.lifecycle:lifecycle-viewmodel-compose` - ViewModel
- `androidx.hilt:hilt-android` - Dependency Injection (Dagger Hilt)
- `androidx.work:work-runtime-ktx` - Background work (WorkManager)
- `androidx.datastore:datastore-preferences` - Local data storage

**Firebase SDK (Android):**
- `com.google.firebase:firebase-auth` - Authentication
- `com.google.firebase:firebase-firestore` - Firestore database
- `com.google.firebase:firebase-messaging` - FCM push notifications
- `com.google.firebase:firebase-functions` - Cloud Functions calls
- `com.google.firebase:firebase-analytics` - Analytics (optional)
- `com.google.android.gms:play-services-auth` - Google Sign-In

**Other Libraries:**
- `org.jetbrains.kotlinx:kotlinx-coroutines-android` - Coroutines
- `com.google.accompanist:accompanist-permissions` - Runtime permissions
- Material3 components (via Compose)
- `com.google.code.gson:gson` - JSON serialization (if needed)

---

### Cloud Functions (Go)

**Function 1: Validate Experiment Creation**
```go
// Triggered by: Client call before creating experiment
// Purpose: Enforce free tier limit (3 active experiments)

func ValidateExperimentCreation(userId string) (bool, error) {
  // Query Firestore
  // Count documents in users/{userId}/experiments where status == "active"
  // Return allowed: count < 3 || user.tier == "paid"
}
```

**Function 2: Send Daily Notifications**
```go
// Triggered by: Cloud Scheduler (cron job, runs daily)
// Purpose: Send push notifications for active experiments

func SendDailyReminders() {
  // Query all users with active experiments
  // For each user:
  //   - Check notification time preference
  //   - Get list of active experiments for today
  //   - Send FCM notification with experiment names
  //   - Notification includes actionable buttons
}
```

**Function 3: Handle Notification Actions**
```go
// Triggered by: FCM notification button tap
// Purpose: Record check-in from notification

func HandleCheckInAction(userId, experimentId, date string, completed bool) {
  // Write to Firestore: users/{userId}/experiments/{experimentId}/checkIns/{date}
  // Set completed: true/false, timestamp: now
}
```

**Function 4 (Future): Process Payment**
```go
func HandlePayment(userId, paymentToken string) {
  // Integrate with Stripe/payment provider
  // Update user tier to "paid"
}
```

---

### Firestore Security Rules

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {

    // Helper function
    function isAuthenticated() {
      return request.auth != null;
    }

    function isOwner(userId) {
      return request.auth.uid == userId;
    }

    match /users/{userId} {
      // Users can only read/write their own data
      allow read, write: if isAuthenticated() && isOwner(userId);

      match /experiments/{experimentId} {
        allow read: if isAuthenticated() && isOwner(userId);
        allow create: if isAuthenticated() && isOwner(userId);
        // Note: 3-experiment limit enforced by Cloud Function
        allow update, delete: if isAuthenticated() && isOwner(userId);

        match /checkIns/{checkInId} {
          allow read, write: if isAuthenticated() && isOwner(userId);
        }

        match /reflections/{reflectionId} {
          allow read, write: if isAuthenticated() && isOwner(userId);
        }
      }
    }
  }
}
```

---

### Push Notifications Flow

**Setup:**
1. User grants notification permission on app launch
2. App gets FCM token from Firebase
3. Store token in Firestore: `users/{userId}/profile/fcmToken`

**Daily Reminder:**
1. Cloud Scheduler triggers `SendDailyReminders` function at intervals (e.g., every hour)
2. Function queries users whose `notificationTime` matches current hour
3. For each user, get active experiments
4. Send FCM notification via Firebase Admin SDK
5. Notification payload:
```json
{
  "notification": {
    "title": "Time for your experiments!",
    "body": "Did you do: Meditate 5 minutes, Drink water?"
  },
  "data": {
    "experiments": "[{id: 'exp1', name: 'Meditate'}, ...]"
  },
  "android": {
    "actions": [
      {"action": "yes", "title": "Yes"},
      {"action": "no", "title": "No"},
      {"action": "open", "title": "Open App"}
    ]
  }
}
```

**Handling Actions:**
- User taps "Yes" or "No" in notification
- Android triggers app's notification handler
- Handler calls Cloud Function `HandleCheckInAction`
- Check-in recorded in Firestore
- App shows confirmation (if open) or silent background update

---

## App Screens Structure

```
App
├── Auth Stack (if not authenticated)
│   ├── Welcome Screen
│   ├── Sign In Screen
│   └── Sign Up Screen
│
└── Main Stack (if authenticated)
    ├── Home/Dashboard
    │   ├── Today's Check-ins (top)
    │   ├── Active Experiments List
    │   └── FAB: Add Experiment
    │
    ├── Create/Edit Experiment
    │   └── Form with validation
    │
    ├── Experiment Details
    │   ├── Info & Progress
    │   ├── Check-ins History
    │   ├── Reflections List
    │   └── Actions: Add Reflection, End Experiment
    │
    ├── Add Reflection
    │   └── Text input form
    │
    ├── End Reflection
    │   └── Multi-step form with next action
    │
    └── Settings
        ├── Notification Time Picker
        ├── Account Info
        └── Tier Status
```

---

## Development Phases

### Phase 1: Setup & Authentication (Week 1-2)
**Goals:**
- Set up Android project in Android Studio with Kotlin
- Configure Firebase project (Auth, Firestore, FCM)
- Set up Jetpack Compose
- Implement Hilt dependency injection
- Create base MVVM architecture (Repository, ViewModel, UI)
- Implement authentication (Email/Password + Google OAuth)
- Basic navigation with Compose Navigation
- Connect to Firestore

**Learning Focus:**
- Kotlin basics (if first time)
- Jetpack Compose fundamentals
- MVVM pattern
- Firebase Android SDK setup

**Deliverables:**
- Android project structure set up
- User can sign up / sign in
- Navigation between screens works
- Firebase connected
- Basic repository + ViewModel pattern working

---

### Phase 2: Core Experiment CRUD (Week 3-4)
**Goals:**
- Build Compose UI for experiment creation form
- Implement form validation
- Create ExperimentRepository for Firestore operations
- Display experiments list on Dashboard with LazyColumn
- View experiment details screen
- Edit/delete experiments functionality
- Implement Firestore security rules
- Write & deploy Cloud Function (Go): Validate experiment creation (3-limit)

**Learning Focus:**
- Compose UI components (TextField, Button, DatePicker, etc.)
- State management in Compose
- Firestore CRUD operations
- Cloud Functions deployment

**Deliverables:**
- User can create, view, edit, delete experiments
- Free tier limit enforced via Cloud Function
- Data persists in Firestore
- Clean MVVM structure

---

### Phase 3: Check-ins & Progress Tracking (Week 5-6)
**Goals:**
- Build daily check-in UI on Dashboard (Compose Cards/Checkboxes)
- Implement CheckInRepository for Firestore operations
- Record check-ins in Firestore subcollection
- Display check-in history in Experiment Details (Calendar view or list)
- Calculate progress (days completed / total days)
- Build streak counter logic
- Add optional note field for daily reflections

**Learning Focus:**
- Firestore subcollections
- Date/time handling in Kotlin
- Custom Compose UI components

**Deliverables:**
- User can mark daily check-ins in-app
- Progress bar/indicator displays correctly
- Check-in history displayed
- Streak counter works

---

### Phase 4: Reflections (Week 6-7)
**Goals:**
- Build Compose UI for adding reflections
- Implement ReflectionRepository for Firestore operations
- Add daily reflection (optional) functionality
- Build end experiment flow with required reflection
- Display reflections list in Experiment Details
- Implement "next action" flow (Continue/Modify/End)
- Handle automatic end-of-experiment detection

**Learning Focus:**
- Multi-step forms in Compose
- State management for complex flows

**Deliverables:**
- User can add reflections anytime
- End reflection prompts work correctly
- Next actions (Continue/Modify/End) create new experiments or mark as completed
- Auto-detect experiment completion based on end date

---

### Phase 5: Push Notifications (Week 7-8)
**Goals:**
- Request notification permissions (runtime permissions in Android)
- Set up FCM in Android app (FirebaseMessagingService)
- Get and store FCM token in Firestore
- Build notification handler to receive FCM messages
- Implement actionable notifications with Yes/No buttons
- Write & deploy Cloud Function (Go): Send daily reminders (scheduled via Cloud Scheduler)
- Write & deploy Cloud Function (Go): Handle notification button actions
- Build Settings screen with notification time preference picker
- Use WorkManager for local notification scheduling (backup)

**Learning Focus:**
- Android notifications (NotificationChannel, NotificationCompat)
- FCM integration
- Runtime permissions
- Cloud Scheduler setup
- WorkManager for background tasks

**Deliverables:**
- User receives daily push notifications at preferred time
- Tapping Yes/No in notification records check-in without opening app
- Notification preferences saved
- Cloud Functions deployed and scheduled

---

### Phase 6: UI Polish & Testing (Week 9)
**Goals:**
- Improve UI/UX with Material3 theming
- Add animations (Compose animations)
- Implement proper error states and loading indicators
- Handle edge cases (no internet, errors, empty states)
- Add input validation feedback
- Test on physical Android device(s)
- Test on different Android versions
- Fix bugs found during testing
- Add onboarding flow (optional - simple splash/intro screens)
- Implement proper error handling and user feedback

**Learning Focus:**
- Compose animations
- Material3 theming
- Error handling best practices

**Deliverables:**
- App feels polished with smooth animations
- All major bugs fixed
- Tested on real device(s)
- Error states handled gracefully
- Loading states implemented

---

### Phase 7: Deployment Prep (Week 10)
**Goals:**
- Generate app icon & splash screen (Android adaptive icons)
- Configure app signing (create and secure Android keystore)
- Set up ProGuard/R8 for code obfuscation
- Build release APK/AAB (Android App Bundle)
- Test release build thoroughly
- Create Google Play Store assets:
  - Screenshots (multiple device sizes)
  - Feature graphic
  - App description
  - Privacy policy
  - Store listing content
- Submit to Play Store (internal testing first, then production)
- Set up Firebase Analytics (optional but recommended)
- Set up crash reporting (Firebase Crashlytics)

**Learning Focus:**
- Android release build process
- App signing
- Play Store submission process

**Deliverables:**
- Release APK/AAB built and tested
- Play Store listing created
- App submitted to Play Store
- Waiting for review/approval (typically 1-3 days)

---

## Post-Launch (Future Enhancements)

### Tier 2 Features (After MVP Launch)
- Payment integration (Google Play Billing / Stripe / RevenueCat)
- Upgrade flow from free to paid tier
- iOS version (Swift/SwiftUI - separate project)
- Web version (Kotlin Multiplatform Mobile or separate React app)
- Export experiment history (PDF/CSV)
- Charts & insights (success rate, trends, analytics dashboard)
- Templates for common experiments
- Categories/tags for experiments
- Social features (share experiments, community templates)
- Widgets (home screen widget for quick check-ins)
- Backup & restore functionality
- Dark mode support (if not in MVP)

---

## Cost Estimates (MVP)

### Firebase Free Tier Limits
- **Authentication:** 50,000 MAU (Monthly Active Users)
- **Firestore:** 1 GB storage, 50K reads/day, 20K writes/day
- **Cloud Functions:** 2M invocations/month, 400K GB-sec compute
- **Cloud Messaging:** Unlimited notifications
- **Hosting:** 10 GB storage, 360 MB/day transfer

**Estimate:** Free tier covers MVP with up to ~500-1000 active users.

### If Scaling Beyond Free Tier
- Firebase Blaze plan (pay-as-you-go)
- Estimated cost for 5K MAU: ~$20-50/month
- Cloud Functions most likely to exceed free tier first

### Google Play Store
- One-time developer fee: $25

---

## Risk Mitigation

### Technical Risks
1. **Kotlin & Android learning curve (first time)**
   - Mitigation: Follow official Android & Kotlin docs, use structured learning path (Kotlin Basics → Jetpack Compose → MVVM)
   - Start with simple features, gradually increase complexity
   - Allocate extra time in early phases

2. **Jetpack Compose learning curve**
   - Mitigation: Follow official Compose tutorials, plenty of examples available
   - Compose is actually easier than XML layouts for beginners

3. **Notification reliability (Android background restrictions)**
   - Mitigation: Test on multiple devices/Android versions (especially Android 12+)
   - Implement WorkManager as backup for local notifications
   - Handle battery optimization exemptions properly

4. **Firestore costs with scale**
   - Mitigation: Monitor usage in Firebase console, optimize queries, implement local caching with Room database if needed

5. **MVVM & Dependency Injection complexity**
   - Mitigation: Start with simple architecture, refactor as you learn
   - Hilt has good documentation

### Timeline Risks
1. **First Android/Kotlin app**
   - Mitigation: Timeline extended to 8-10 weeks
   - Phase 1 extended to 2 weeks for learning
   - Can descope Phase 5 (notifications) if severely behind

2. **Notification complexity**
   - Mitigation: Phase 5 is late (Week 7-8), can descope and ship without notifications initially
   - Core functionality (manual check-ins) works without notifications

### Monetization Risks
1. **Low conversion from free to paid**
   - Mitigation: Start gathering user feedback early, adjust value proposition

---

## Success Metrics (Post-Launch)

**MVP Success Criteria:**
- App published on Play Store
- 50+ downloads in first month
- <5% crash rate
- At least 5 users creating experiments daily
- 10% conversion to paid tier (optimistic, may take time)

**Engagement Metrics to Track:**
- Daily Active Users (DAU)
- Experiments created per user
- Check-in completion rate
- Notification open rate
- Reflection completion rate

---

## Notes

- Keep scope tight for MVP - resist feature creep
- Focus on core loop: Create → Check-in → Reflect → Iterate
- User feedback after launch will guide future features
- Start simple, iterate based on real usage

---

## Tech Stack Summary

**Android App:**
- Kotlin
- Jetpack Compose (UI)
- MVVM Architecture
- Coroutines & Flow
- Hilt (Dependency Injection)
- Jetpack Components:
  - Navigation Compose
  - ViewModel
  - WorkManager
  - DataStore
  - Room (future - for caching)

**Backend:**
- Firebase Auth
- Firestore
- Cloud Functions (Go)
- Cloud Messaging (FCM)
- Cloud Scheduler

**Development Tools:**
- Android Studio (Arctic Fox or later)
- Firebase CLI
- Git
- Postman (for testing Cloud Functions)

**Deployment:**
- Google Play Store (Android)
- Firebase Console (backend management)

---

## Next Steps

### Immediate Actions (Before Coding)

1. **Learn Kotlin Basics** (if needed)
   - Complete Kotlin Koans or official Kotlin tutorial (4-8 hours)
   - Focus on: basics, null safety, data classes, coroutines

2. **Set up Development Environment**
   - Ensure Android Studio is up to date
   - Install Android SDK (API 24+ for compatibility, API 34 for target)
   - Set up Android emulator or connect physical device
   - Install Git

3. **Learn Jetpack Compose Basics**
   - Complete official Compose tutorial pathway (8-12 hours)
   - Build simple sample app to understand state, recomposition

4. **Create Firebase Project**
   - Go to Firebase Console
   - Create new project: "tiny-experiments"
   - Enable Authentication (Email/Password + Google)
   - Create Firestore database (start in test mode, will add security rules later)
   - Enable Cloud Messaging
   - Set up billing (Blaze plan for Cloud Functions)

5. **Initialize Android Project**
   - Create new Android Studio project
   - Select: "Empty Compose Activity"
   - Language: Kotlin
   - Minimum SDK: API 24 (covers ~95% devices)
   - Package name: com.yourname.tinyexperiments (or similar)

6. **Project Structure Setup**
   - Set up proper package structure (data, domain, presentation layers)
   - Add Firebase dependencies to build.gradle
   - Set up Hilt for dependency injection
   - Initialize Firebase in app

7. **Start Phase 1: Authentication**

### Learning Resources

**Kotlin:**
- Official Kotlin docs: https://kotlinlang.org/docs/home.html
- Kotlin Koans: https://play.kotlinlang.org/koans

**Jetpack Compose:**
- Official pathway: https://developer.android.com/courses/pathways/compose
- Compose samples: https://github.com/android/compose-samples

**MVVM & Architecture:**
- Android Architecture Guide: https://developer.android.com/topic/architecture
- MVVM pattern: https://developer.android.com/topic/libraries/architecture/viewmodel

**Firebase Android:**
- Firebase Android setup: https://firebase.google.com/docs/android/setup
- Firestore Android: https://firebase.google.com/docs/firestore/quickstart

**Cloud Functions (Go):**
- Firebase Functions Go: https://firebase.google.com/docs/functions/get-started
