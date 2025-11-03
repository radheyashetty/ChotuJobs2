# 📊🗺️ Google Sheets & Maps Integration Setup Guide

This guide will help you set up Google Sheets backup and OSMDroid maps integration for your ChotuJobs app.

## 📊 **Google Sheets Backup Setup**

### **Step 1: Create Google Apps Script**

1. **Go to Google Apps Script**:
   - Visit: https://script.google.com/
   - Sign in with your Google account

2. **Create New Project**:
   - Click "New Project"
   - Delete the default code and paste the following:

```javascript
function doPost(e) {
  try {
    // Get the data from the POST request
    const data = JSON.parse(e.postData.contents);
    const type = data.type;
    const timestamp = data.timestamp;
    const jobData = data.data;
    
    // Open the Google Sheet
    const sheet = SpreadsheetApp.openById('YOUR_SHEET_ID').getActiveSheet();
    
    // Prepare row data based on type
    let rowData = [];
    
    if (type === 'job') {
      rowData = [
        new Date(timestamp), // Timestamp
        type,               // Type
        jobData.jobId,      // Job ID
        jobData.title,      // Title
        jobData.description, // Description
        jobData.budget,     // Budget
        jobData.status,     // Status
        jobData.createdByUID, // Created By UID
        jobData.createdByName, // Created By Name
        jobData.location,   // Location
        jobData.category    // Category
      ];
    } else if (type === 'user') {
      rowData = [
        new Date(timestamp), // Timestamp
        type,               // Type
        userData.uid,       // UID
        userData.name,      // Name
        userData.role,      // Role
        userData.phone,     // Phone
        userData.location   // Location
      ];
    }
    
    // Append the row to the sheet
    sheet.appendRow(rowData);
    
    // Return success response
    return ContentService
      .createTextOutput(JSON.stringify({success: true}))
      .setMimeType(ContentService.MimeType.JSON);
      
  } catch (error) {
    // Return error response
    return ContentService
      .createTextOutput(JSON.stringify({success: false, error: error.toString()}))
      .setMimeType(ContentService.MimeType.JSON);
  }
}

function setup() {
  // This function sets up the sheet headers
  const sheet = SpreadsheetApp.openById('YOUR_SHEET_ID').getActiveSheet();
  
  // Set headers
  sheet.getRange(1, 1, 1, 11).setValues([[
    'Timestamp', 'Type', 'Job ID', 'Title', 'Description', 
    'Budget', 'Status', 'Created By UID', 'Created By Name', 
    'Location', 'Category'
  ]]);
  
  // Format header row
  sheet.getRange(1, 1, 1, 11).setFontWeight('bold');
  sheet.getRange(1, 1, 1, 11).setBackground('#4285f4');
  sheet.getRange(1, 1, 1, 11).setFontColor('white');
}
```

### **Step 2: Create Google Sheet**

1. **Create New Sheet**:
   - Go to https://sheets.google.com/
   - Click "Blank" to create a new sheet
   - Name it "ChotuJobs Backup"

2. **Get Sheet ID**:
   - Copy the Sheet ID from the URL (the long string between `/d/` and `/edit`)
   - Replace `YOUR_SHEET_ID` in the Apps Script code with this ID

3. **Run Setup Function**:
   - In Apps Script, click "Run" next to the `setup` function
   - Grant permissions when prompted
   - This will create the headers in your sheet

### **Step 3: Deploy as Web App**

1. **Deploy**:
   - In Apps Script, click "Deploy" → "New deployment"
   - Choose "Web app" as the type
   - Set "Execute as" to "Me"
   - Set "Who has access" to "Anyone"
   - Click "Deploy"

2. **Get Web App URL**:
   - Copy the web app URL
   - It will look like: `https://script.google.com/macros/s/YOUR_SCRIPT_ID/exec`

3. **Update App Configuration**:
   - In your Android project, open `GoogleSheetsBackupService.java`
   - Replace `YOUR_SCRIPT_ID` with your actual script ID:

```java
private static final String GOOGLE_APPS_SCRIPT_URL = "https://script.google.com/macros/s/YOUR_ACTUAL_SCRIPT_ID/exec";
```

### **Step 4: Test the Integration**

1. **Build and install** your app
2. **Create a job** as a contractor
3. **Check your Google Sheet** - the job data should appear automatically

---

## 🗺️ **OSMDroid Maps Integration**

### **Step 1: Verify Dependencies**

The required dependencies are already added to your `build.gradle.kts`:

```kotlin
// OSMDroid for maps
implementation("org.osmdroid:osmdroid-android:6.1.18")
```

### **Step 2: Initialize OSMDroid**

1. **In your Application class** (`App.java`), add OSMDroid initialization:

```java
@Override
public void onCreate() {
    super.onCreate();
    
    // Initialize OSMDroid configuration
    Configuration.getInstance().load(this, 
            PreferenceManager.getDefaultSharedPreferences(this));
    
    // Initialize other services...
}
```

2. **Add to your Application class**:

```java
import org.osmdroid.config.Configuration;
import androidx.preference.PreferenceManager;
```

### **Step 3: Test Maps Integration**

1. **Build and install** your app
2. **Navigate to Contractor Dashboard**
3. **Click "Post Job"** - you should see a "Select Location" button
4. **Tap "Select Location"** - this will open the map activity

### **Step 4: Customize Map Features**

#### **Change Default Location**
In `MapFragment.java`, modify the default location:

```java
// Set default location (Mumbai, India)
GeoPoint defaultLocation = new GeoPoint(19.0760, 72.8777);
```

#### **Add Custom Markers**
```java
// Add a marker for a job location
GeoPoint jobLocation = new GeoPoint(19.0760, 72.8777);
mapFragment.addMarker(jobLocation, "Job Location", "Fix leaking tap");
```

#### **Center on User Location**
```java
// Center map on user's current location
mapFragment.setShowUserLocation(true);
```

---

## 🔧 **Usage Examples**

### **Using Google Sheets Backup**

```java
// In your activity or fragment
GoogleSheetsBackupService backupService = new GoogleSheetsBackupService();

// Backup job data
GoogleSheetsBackupService.JobBackupData jobData = 
    new GoogleSheetsBackupService.JobBackupData(
        "job123", "Fix tap", "Fix leaking tap", 500, 
        "Open", "user123", "John Doe", 
        System.currentTimeMillis(), "Mumbai", "Plumbing"
    );

backupService.backupJobData(jobData, new GoogleSheetsBackupService.BackupCallback() {
    @Override
    public void onSuccess() {
        Log.d(TAG, "Job backed up successfully");
    }
    
    @Override
    public void onFailure(String error) {
        Log.e(TAG, "Backup failed: " + error);
    }
});
```

### **Using Maps Integration**

```java
// In your activity
Intent intent = new Intent(this, JobLocationPickerActivity.class);
startActivityForResult(intent, LOCATION_PICKER_REQUEST_CODE);

// Handle result
@Override
protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    
    if (requestCode == LOCATION_PICKER_REQUEST_CODE && resultCode == RESULT_OK) {
        double latitude = data.getDoubleExtra(JobLocationPickerActivity.EXTRA_LATITUDE, 0);
        double longitude = data.getDoubleExtra(JobLocationPickerActivity.EXTRA_LONGITUDE, 0);
        String address = data.getStringExtra(JobLocationPickerActivity.EXTRA_ADDRESS);
        
        // Use the selected location
        Log.d(TAG, "Selected location: " + latitude + ", " + longitude);
    }
}
```

---

## 🚨 **Important Notes**

### **Google Sheets Security**
- **Never expose your Apps Script URL publicly**
- The current setup allows "Anyone" access for simplicity
- For production, implement proper authentication
- Consider using Firebase Functions instead for better security

### **Maps Permissions**
- Location permissions are automatically requested
- Users can deny location access - the app will still work
- Maps will show a default location if user location is unavailable

### **Offline Support**
- OSMDroid works offline with cached tiles
- Google Sheets backup requires internet connection
- Backup failures are logged but don't affect app functionality

---

## 🧪 **Testing Your Setup**

### **Test Google Sheets Backup**
1. Create a job as a contractor
2. Check your Google Sheet for new entries
3. Verify all fields are populated correctly

### **Test Maps Integration**
1. Open the location picker
2. Grant location permission when prompted
3. Verify the map loads and shows your location
4. Test marker placement (if implemented)

### **Test Error Handling**
1. Disable internet and try to create a job
2. Deny location permission and open maps
3. Check logs for proper error messages

---

## 🎯 **Next Steps**

1. **Customize the backup data structure** to match your needs
2. **Implement reverse geocoding** for better address display
3. **Add map clustering** for multiple job markers
4. **Implement offline map caching** for better performance
5. **Add map themes** (dark mode, satellite view)

---

## 🆘 **Troubleshooting**

### **Google Sheets Issues**
- **"Script not found"**: Check your Script ID in the URL
- **"Permission denied"**: Ensure the web app is deployed with "Anyone" access
- **"Data not appearing"**: Check the Apps Script execution logs

### **Maps Issues**
- **"Map not loading"**: Check internet connection and OSMDroid initialization
- **"Location not found"**: Ensure location permissions are granted
- **"App crashes on map"**: Check for missing dependencies in build.gradle

### **General Issues**
- **Build errors**: Clean and rebuild the project
- **Runtime errors**: Check device logs using `adb logcat`

---

## 📞 **Support**

If you encounter issues:
1. Check the Android logs: `adb logcat | grep ChotuJobs`
2. Verify all dependencies are correctly added
3. Ensure all configuration steps are completed
4. Test with a simple example first

Happy coding! 🚀
