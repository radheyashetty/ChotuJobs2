# ChotuJobs - Labor Marketplace Android App

A modern Android application that connects daily wage laborers, contractors, and agents for job opportunities with a bidding system and real-time messaging.

## 📋 Project Overview

**Purpose**: Digital platform for connecting laborers, agents, and contractors in India for short-term work opportunities.

**Target Users**:
- **Labourers**: View active jobs, place bids, and communicate with contractors
- **Agents**: Place bids on behalf of multiple laborers
- **Contractors**: Post jobs, select winning bids, and communicate with bidders

## 🎯 Key Features

### ✅ Core Features (Implemented)

1. **Firebase Authentication & Login System**
   - Email/Password authentication via Firebase
   - User registration with automatic profile creation
   - Secure authentication with Firebase Auth
   - Profile management with image upload support

2. **Firestore Database**
   - `users` collection: Stores user profiles (name, email, role, profile image)
   - `jobs` collection: Stores job postings with location (GeoPoint), images, and status
   - `bids` sub-collection: Stores bids under each job with bidder info and status
   - `chats` collection: Real-time messaging between users
   - `messages` sub-collection: Chat messages with timestamps
   - Real-time sync across devices

3. **Contractor Features**
   - Create new jobs with:
     - Title and Category (spinner)
     - Start date (date picker)
     - Location selection
     - Job requirements and bid limits
   - View all their posted jobs
   - See bids for each job with bidder details
   - Accept/reject bids
   - Select winning bid (closes job and notifies winner via message)
   - Real-time chat with bidders

4. **Labourer Features**
   - View list of active jobs
   - Place bid amount on jobs
   - View bid status (pending/accepted/rejected)
   - Real-time chat with contractors
   - Receive notifications when bid is accepted

5. **Agent Features**
   - View same job listings as labourers
   - Select labourer from dropdown before bidding
   - Place bid on behalf of selected labourer
   - Manage bids for multiple labourers
   - Real-time chat functionality

6. **Real-time Messaging**
   - Chat with contractors (for labourers/agents)
   - Chat with bidders (for contractors)
   - Real-time message delivery
   - Chat history persistence
   - User-friendly chat interface

7. **Dark Mode Support**
   - Automatic dark mode based on system settings
   - Material Design 3 theming
   - Optimized color scheme for both light and dark themes
   - Custom purple primary color in dark mode

8. **User Profile Management**
   - Edit profile information
   - Upload and change profile images
   - View profile details

## 🏗️ Architecture & Tech Stack

### Technologies Used

- **Language**: Java 17
- **Authentication**: Firebase Authentication (Email/Password)
- **Database**: Cloud Firestore
- **Storage**: Firebase Storage (for profile images)
- **UI/UX**: Material Design 3 with Dark Mode support
- **Image Loading**: Glide
- **Build Tool**: Gradle with Kotlin DSL

### Project Structure

```
app/src/main/java/com/chotujobs/
├── MainActivity.java                    # Main dashboard with role-based fragments
├── LoginActivity.java                   # Firebase Auth login/registration
├── CreateJobActivity.java               # Job creation form for contractors
├── EditProfileActivity.java             # Profile editing
├── ChatActivity.java                    # Real-time messaging
├── models/
│   ├── User.java                        # User data model
│   ├── Job.java                         # Job data model
│   ├── Bid.java                         # Bid data model
│   ├── Chat.java                        # Chat data model
│   └── Message.java                     # Message data model
├── fragments/
│   ├── ProfileFragment.java             # User profile display
│   ├── JobsListFragment.java            # Active jobs list (labourer/agent)
│   ├── ContractorFragment.java          # Contractor's job list
│   ├── ChatsFragment.java               # Chat list
│   ├── BidDialogFragment.java           # Bid placement dialog
│   ├── BidderDetailsDialogFragment.java # Bidder details and winner selection
│   └── JobDetailsDialogFragment.java    # Job details with bids
├── adapters/
│   ├── JobAdapter.java                  # RecyclerView adapter for jobs
│   ├── ContractorJobAdapter.java        # RecyclerView adapter for contractor jobs
│   ├── BidAdapter.java                  # RecyclerView adapter for bids
│   ├── ChatsAdapter.java                # RecyclerView adapter for chats
│   └── MessagesAdapter.java             # RecyclerView adapter for messages
└── services/
    └── FirestoreService.java            # Firestore CRUD operations
```

## 🚀 Setup Instructions

### Prerequisites

1. **Android Studio**: Hedgehog (2023.1.1) or later
2. **Java JDK**: Version 17 or later
3. **Firebase Account**: Free tier
4. **Android SDK**: API 28 (Android 9.0) minimum, API 34 target

### Firebase Setup

1. Create a Firebase project at [Firebase Console](https://console.firebase.google.com/)
2. Enable **Authentication** > Email/Password
3. Enable **Cloud Firestore Database**
4. Enable **Firebase Storage** (for profile images)
5. Download `google-services.json` from Firebase Console
6. Copy `app/google-services.json.example` to `app/google-services.json` and replace with your actual Firebase config
7. Add Firestore security rules (see `firestore.rules` file)
8. Add Storage security rules (see `storage.rules` file)

### Building the App

1. Clone the repository
2. Open project in Android Studio
3. Sync Gradle files
4. Copy `app/google-services.json.example` to `app/google-services.json` and add your Firebase configuration
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
3. Fill form: title, category, date, requirements
4. Save job → Stored in Firestore
5. View job in list
6. Tap job → See bids with bidder details
7. Accept/reject bids or select winner
8. Select winner → Job closed, winner notified via message
9. Chat with bidders directly

### Labourer Flow

1. Login → See "Active Jobs" fragment
2. View all active jobs
3. Tap job → Enter bid amount
4. Submit bid → Stored in Firestore sub-collection
5. View bid status updates
6. Receive message when bid is accepted
7. Chat with contractors

### Agent Flow

1. Login → See "Active Jobs" fragment
2. View all active jobs
3. Tap job → Select labourer from dropdown
4. Enter bid amount
5. Submit bid → Stored with labourer reference
6. Bid shows as "Agent (for Labourer Name)"
7. Manage multiple bids for different labourers
8. Chat with contractors

## 🗄️ Database Schema

### Firestore Collections

#### `users/{uid}`
```json
{
  "name": "John Doe",
  "email": "john@example.com",
  "role": "labourer|agent|contractor",
  "profileImageUrl": "https://..."
}
```

#### `jobs/{jobId}`
```json
{
  "contractorId": "contractor_uid",
  "title": "Construction Work",
  "category": "Construction",
  "startDate": "25/12/2024",
  "location": "19.0760, 72.8777",
  "status": "active|closed",
  "timestamp": 1703001234567,
  "imageUrl": "",
  "requirements": "Must have experience",
  "bidLimit": 10,
  "winnerUserId": "winner_uid"
}
```

#### `jobs/{jobId}/bids/{bidId}`
```json
{
  "bidderId": "bidder_uid",
  "bidAmount": 1500,
  "jobId": "job_id",
  "labourerIdIfAgent": "labourer_uid or null",
  "status": "pending|accepted|rejected",
  "timestamp": 1703001234567
}
```

#### `chats/{chatId}`
```json
{
  "userIds": ["user1_uid", "user2_uid"],
  "lastMessage": "Hello",
  "lastMessageTime": 1703001234567
}
```

#### `chats/{chatId}/messages/{messageId}`
```json
{
  "senderId": "sender_uid",
  "message": "Hello, I'm interested in this job",
  "timestamp": 1703001234567
}
```

## 🔒 Security & Permissions

### App Permissions
- `INTERNET`: For Firebase/Firestore
- `ACCESS_FINE_LOCATION`: For location services
- `ACCESS_COARSE_LOCATION`: Fallback location
- `READ_EXTERNAL_STORAGE`: For image selection (Android 10-)
- `CAMERA`: For profile image capture

### Firestore Security Rules

See `firestore.rules` file for complete security rules. Key points:
- Users can only read/write their own profile
- Jobs are readable by all authenticated users
- Only contractors can create jobs
- Only job owners can update job status and select winners
- Bids can be created by labourers/agents, updated by job owners
- Chats are only accessible by participants

## 🧪 Testing

### Manual Testing Checklist

- [ ] User registration with Firebase
- [ ] Login with valid credentials
- [ ] Role-based dashboard display
- [ ] Contractor: Create job
- [ ] Labourer: Place bid
- [ ] Agent: Place bid with labourer selection
- [ ] Contractor: View bids and bidder details
- [ ] Contractor: Accept/reject bids
- [ ] Contractor: Select winner
- [ ] Winner notification via message
- [ ] Real-time chat functionality
- [ ] Profile editing and image upload
- [ ] Dark mode switching
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

1. Firebase Storage requires billing account for production use
2. No offline support yet (Firestore offline persistence can be enabled)
3. Image uploads require Firebase Storage billing

## 🔮 Future Enhancements

- [ ] Enable Firestore offline persistence
- [ ] Implement push notifications
- [ ] Add job search & filters
- [ ] Add review/rating system
- [ ] Implement payment integration
- [ ] Add job categories with icons
- [ ] Implement job image uploads
- [ ] Add location-based job filtering

## 📝 License

This project is for educational purposes only.

## 👥 Credits

Built for ChotuJobs - Connecting daily wage workers with opportunities.

---

**Last Updated**: January 2025
**Version**: 1.0.0
**Status**: ✅ Production Ready
