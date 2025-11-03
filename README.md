# ChotuJobs - Labor Marketplace Android App

A simple Android application that connects daily wage laborers, contractors, and agents for job opportunities with a bidding system.

## 📋 Project Overview

**Purpose**: Digital platform for connecting laborers, agents, and contractors in India for short-term work opportunities.

**Target Users**:
- **Labourers**: View active jobs and place bids
- **Agents**: Place bids on behalf of multiple laborers
- **Contractors**: Post jobs and select winning bids

## 🎯 Key Features

### ✅ Core Features (Implemented)

1. **Firebase Authentication & Login System**
   - Email/Password authentication via Firebase
   - User registration with automatic profile creation
   - Secure authentication with Firebase Auth

2. **Firestore Database**
   - `users` collection: Stores user profiles (name, email, role)
   - `jobs` collection: Stores job postings with location (GeoPoint), images, and status
   - `bids` sub-collection: Stores bids under each job with bidder info and winner flag
   - Real-time sync across devices

3. **Contractor Features**
   - Create new jobs with:
     - Title and Category (spinner)
     - Start date (date picker)
     - Location selection via Google Maps
   - View all their posted jobs
   - See bids for each job
   - Select winning bid (closes job and exports to CSV)

4. **Labourer Features**
   - View list of active jobs
   - Place bid amount on jobs
   - Bid for themselves only

5. **Agent Features**
   - View same job listings as labourers
   - Select labourer from dropdown before bidding
   - Place bid on behalf of selected labourer

6. **CSV Export**
   - Closed jobs automatically export to CSV
   - Saved to device storage
   - Includes job details, winner info, and all bids

7. **Google Maps Integration**
   - Location picker for job creation
   - Stores latitude/longitude as GeoPoint in Firestore

## 🏗️ Architecture & Tech Stack

### Technologies Used

- **Language**: Java
- **Authentication**: Firebase Authentication (Email/Password)
- **Database**: Cloud Firestore
- **Storage**: Device local storage (CSV files)
- **Maps**: Google Maps SDK
- **UI/UX**: Material Design 3
- **Build Tool**: Gradle with Kotlin DSL

### Project Structure

```
app/src/main/java/com/chotujobs/
├── MainActivity.java                 # Main dashboard with role-based fragments
├── LoginActivity.java                # Firebase Auth login/registration
├── CreateJobActivity.java            # Job creation form for contractors
├── MapsActivity.java                 # Google Maps location picker
├── models/
│   ├── User.java                     # User data model
│   ├── Job.java                      # Job data model
│   └── Bid.java                      # Bid data model
├── fragments/
│   ├── LabourFragment.java          # Labourer dashboard
│   ├── AgentFragment.java           # Agent dashboard
│   ├── ContractorFragment.java      # Contractor dashboard
│   ├── BidDialogFragment.java       # Bid placement dialog
│   └── JobDetailsDialogFragment.java # Job details with bids
├── adapters/
│   ├── JobAdapter.java              # RecyclerView adapter for jobs
│   └── ContractorJobAdapter.java    # RecyclerView adapter for contractor jobs
├── services/
│   └── FirestoreService.java        # Firestore CRUD operations
└── util/
    └── CSVExporter.java              # CSV export utility
```

## 🚀 Setup Instructions

### Prerequisites

1. **Android Studio**: Arctic Fox or later
2. **Java JDK**: Version 17 or later
3. **Firebase Account**: Free tier
4. **Google Cloud Account**: For Maps API

### Firebase Setup

1. Create a Firebase project at [Firebase Console](https://console.firebase.google.com/)
2. Enable **Authentication** > Email/Password
3. Enable **Cloud Firestore Database**
4. Download `google-services.json` and place it in `app/`
5. Add Firestore security rules (see below)

### Google Maps Setup

1. Go to [Google Cloud Console](https://console.cloud.google.com/)
2. Create/select a project
3. Enable **Maps SDK for Android**
4. Create API key
5. Replace `YOUR_GOOGLE_MAPS_API_KEY_HERE` in `AndroidManifest.xml`

### Firestore Security Rules

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    // Users collection
    match /users/{userId} {
      allow read: if true;
      allow write: if request.auth != null && request.auth.uid == userId;
    }
    
    // Jobs collection
    match /jobs/{jobId} {
      allow read: if true;
      allow create: if request.auth != null;
      allow update: if request.auth != null && 
                       (request.resource.data.diff(resource.data).affectedKeys().hasOnly(['status', 'winnerUserId']));
      
      // Bids sub-collection
      match /bids/{bidId} {
        allow read: if true;
        allow create: if request.auth != null;
        allow update: if request.auth != null;
      }
    }
  }
}
```

### Building the App

1. Clone the repository
2. Open project in Android Studio
3. Sync Gradle files
4. Replace Maps API key in `AndroidManifest.xml`
5. Run on emulator or device

## 📱 User Flows

### Registration & Login

1. Open app → LoginActivity
2. New users: Click "Register"
3. Enter email & password
4. Firebase creates account
5. Select role (Labourer/Agent/Contractor)
6. Profile saved to Firestore
7. Navigate to role-specific dashboard

### Contractor Flow

1. Login → See "My Jobs" fragment
2. Tap "Create Job" button
3. Fill form: title, category, date
4. Pick location on map
5. Save job → Stored in Firestore
6. View job in list
7. Tap job → See bids
8. Select winner → Job closed, CSV exported

### Labourer Flow

1. Login → See "Active Jobs" fragment
2. View all active jobs
3. Tap job → Enter bid amount
4. Submit bid → Stored in Firestore sub-collection
5. Wait for contractor to select winner

### Agent Flow

1. Login → See "Active Jobs" fragment
2. View all active jobs
3. Tap job → Select labourer from dropdown
4. Enter bid amount
5. Submit bid → Stored with labourer reference
6. Bid shows as "Agent (for Labourer Name)"

## 🗄️ Database Schema

### Firestore Collections

#### `users/{uid}`
```json
{
  "name": "John Doe",
  "email": "john@example.com",
  "role": "labourer|agent|contractor"
}
```

#### `jobs/{jobId}`
```json
{
  "contractorId": "contractor_uid",
  "title": "Construction Work",
  "category": "Construction",
  "startDate": "25/12/2024",
  "location": { "lat": 19.0760, "lng": 72.8777 },
  "status": "active|closed",
  "timestamp": 1703001234567,
  "imagePath": "",
  "imageUrl": "",
  "winnerUserId": "winner_uid"
}
```

#### `jobs/{jobId}/bids/{bidId}`
```json
{
  "bidderId": "bidder_uid",
  "bidAmount": 1500,
  "labourerIdIfAgent": "labourer_uid or null",
  "winnerFlag": 0 or 1,
  "timestamp": 1703001234567
}
```

## 📊 CSV Export Format

```csv
ChotuJobs - Job Completion Report
=================================

Job ID: abc123
Title: Construction Work
Category: Construction
Start Date: 25/12/2024
Location: 19.0760, 72.8777

WINNING BID:
-------------
Bidder: John Doe
Email: john@example.com
Labourer: John Doe (if agent)
Amount: ₹1500

ALL BIDS SUMMARY:
-----------------
- John Doe: ₹1500 [WINNER]
- Jane Smith: ₹1700
- Agent (for Bob): ₹1600

Report Generated: 25/12/2024 10:30:00
```

## 🔒 Security & Permissions

### App Permissions
- `INTERNET`: For Firebase/Firestore
- `ACCESS_FINE_LOCATION`: For Maps location picker
- `ACCESS_COARSE_LOCATION`: Fallback location
- `READ_EXTERNAL_STORAGE`: For CSV export (Android 10-)
- `WRITE_EXTERNAL_STORAGE`: For CSV export (Android 9-)
- `CAMERA`: For future image capture

## 🧪 Testing

### Manual Testing Checklist

- [ ] User registration with Firebase
- [ ] Login with valid credentials
- [ ] Role-based dashboard display
- [ ] Contractor: Create job with location
- [ ] Labourer: Place bid
- [ ] Agent: Place bid with labourer selection
- [ ] Contractor: Select winner
- [ ] CSV export file creation
- [ ] Logout functionality

## 📦 Build & Deployment

### Build APK

```bash
./gradlew assembleDebug
```

Output: `app/build/outputs/apk/debug/app-debug.apk`

### Build Release

```bash
./gradlew assembleRelease
```

**Note**: Requires signing configuration in `build.gradle.kts`

## 🐛 Known Issues

1. Firebase Storage not configured (no billing account) - images stored as local paths
2. No offline support yet (Firestore offline persistence can be enabled)
3. CSV export uses app-specific directory (Android 10+ compatible)

## 🔮 Future Enhancements

- [ ] Add image upload to Firebase Storage (requires billing)
- [ ] Implement real-time bid updates
- [ ] Add job search & filters
- [ ] Implement push notifications
- [ ] Add review/rating system
- [ ] Enable Firestore offline persistence
- [ ] Add chat/messaging
- [ ] Implement payment integration
- [ ] Add dark mode

## 📝 License

This project is for educational purposes only.

## 👥 Credits

Built for ChotuJobs - Connecting daily wage workers with opportunities.

---

**Last Updated**: December 2024
**Version**: 1.0.0
**Status**: ✅ Production Ready (Firestore-based)
