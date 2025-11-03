/**
 * Google Apps Script for ChotuJobs backup functionality.
 * 
 * This script provides a web app endpoint that accepts job data
 * and stores it in a Google Sheet for backup purposes.
 * 
 * Setup Instructions:
 * 1. Go to script.google.com
 * 2. Create a new project
 * 3. Replace the default code with this script
 * 4. Create a new Google Sheet and note the Sheet ID
 * 5. Update the SHEET_ID variable below
 * 6. Deploy as web app with "Anyone with link" access
 * 7. Copy the web app URL for use in the Android app
 */

// Configuration - Update these values
const SHEET_ID = 'YOUR_GOOGLE_SHEET_ID_HERE'; // Replace with your actual Sheet ID
const SHEET_NAME = 'ChotuJobs_Backup';
const WEBHOOK_SECRET = 'your-webhook-secret-here'; // Optional: for basic security

/**
 * Main function to handle POST requests from the Android app.
 * This function is called when the web app receives a POST request.
 */
function doPost(e) {
  try {
    // Parse the request data
    const requestData = JSON.parse(e.postData.contents);
    
    // Basic security check (optional)
    if (WEBHOOK_SECRET && requestData.secret !== WEBHOOK_SECRET) {
      return ContentService
        .createTextOutput(JSON.stringify({ error: 'Unauthorized' }))
        .setMimeType(ContentService.MimeType.JSON);
    }
    
    // Validate required fields
    if (!requestData.jobId || !requestData.title) {
      return ContentService
        .createTextOutput(JSON.stringify({ error: 'Missing required fields' }))
        .setMimeType(ContentService.MimeType.JSON);
    }
    
    // Process the backup request
    const result = processJobBackup(requestData);
    
    // Return success response
    return ContentService
      .createTextOutput(JSON.stringify({ 
        success: true, 
        message: 'Job backed up successfully',
        rowNumber: result.rowNumber,
        timestamp: new Date().toISOString()
      }))
      .setMimeType(ContentService.MimeType.JSON);
      
  } catch (error) {
    console.error('Error processing backup request:', error);
    
    return ContentService
      .createTextOutput(JSON.stringify({ 
        error: 'Internal server error',
        message: error.toString()
      }))
      .setMimeType(ContentService.MimeType.JSON);
  }
}

/**
 * Process job backup to Google Sheet.
 * 
 * @param {Object} jobData - Job data from the Android app
 * @return {Object} Result object with row number
 */
function processJobBackup(jobData) {
  // Open the Google Sheet
  const spreadsheet = SpreadsheetApp.openById(SHEET_ID);
  let sheet = spreadsheet.getSheetByName(SHEET_NAME);
  
  // Create sheet if it doesn't exist
  if (!sheet) {
    sheet = spreadsheet.insertSheet(SHEET_NAME);
    setupSheetHeaders(sheet);
  }
  
  // Prepare row data
  const rowData = [
    new Date(), // Timestamp
    jobData.jobId || '',
    jobData.title || '',
    jobData.description || '',
    jobData.budget || 0,
    jobData.status || 'Unknown',
    jobData.createdByUID || '',
    jobData.createdAt || new Date().getTime(),
    jobData.location || '',
    jobData.category || '',
    jobData.estimatedDays || '',
    jobData.isUrgent || false,
    jobData.latitude || '',
    jobData.longitude || '',
    JSON.stringify(jobData) // Full data as JSON
  ];
  
  // Append row to sheet
  const range = sheet.appendRow(rowData);
  const rowNumber = range.getRow();
  
  // Auto-resize columns for better readability
  sheet.autoResizeColumns(1, 15);
  
  return {
    rowNumber: rowNumber,
    success: true
  };
}

/**
 * Setup headers for the backup sheet.
 * 
 * @param {Sheet} sheet - Google Sheet object
 */
function setupSheetHeaders(sheet) {
  const headers = [
    'Timestamp',
    'Job ID',
    'Title',
    'Description',
    'Budget',
    'Status',
    'Created By UID',
    'Created At',
    'Location',
    'Category',
    'Estimated Days',
    'Is Urgent',
    'Latitude',
    'Longitude',
    'Full Data (JSON)'
  ];
  
  // Set headers in first row
  sheet.getRange(1, 1, 1, headers.length).setValues([headers]);
  
  // Format headers
  const headerRange = sheet.getRange(1, 1, 1, headers.length);
  headerRange.setBackground('#4285f4');
  headerRange.setFontColor('white');
  headerRange.setFontWeight('bold');
  
  // Freeze header row
  sheet.setFrozenRows(1);
}

/**
 * Handle GET requests (for testing).
 * Returns information about the backup service.
 */
function doGet(e) {
  const info = {
    service: 'ChotuJobs Backup Service',
    version: '1.0.0',
    endpoints: {
      POST: '/exec - Backup job data to Google Sheet'
    },
    requiredFields: [
      'jobId',
      'title'
    ],
    optionalFields: [
      'description',
      'budget',
      'status',
      'createdByUID',
      'createdAt',
      'location',
      'category',
      'estimatedDays',
      'isUrgent',
      'latitude',
      'longitude'
    ],
    timestamp: new Date().toISOString()
  };
  
  return ContentService
    .createTextOutput(JSON.stringify(info, null, 2))
    .setMimeType(ContentService.MimeType.JSON);
}

/**
 * Test function to verify the backup service works.
 * Run this function from the Apps Script editor to test.
 */
function testBackupService() {
  // Create test job data
  const testJobData = {
    jobId: 'test-job-' + Date.now(),
    title: 'Test Job from Apps Script',
    description: 'This is a test job created from Google Apps Script',
    budget: 1000,
    status: 'Open',
    createdByUID: 'test-user-123',
    createdAt: Date.now(),
    location: 'Test Location',
    category: 'Testing',
    estimatedDays: 1,
    isUrgent: false,
    latitude: 12.9716,
    longitude: 77.5946,
    secret: WEBHOOK_SECRET
  };
  
  // Simulate POST request
  const mockRequest = {
    postData: {
      contents: JSON.stringify(testJobData)
    }
  };
  
  // Call the main function
  const result = doPost(mockRequest);
  
  console.log('Test result:', result.getContent());
  return result.getContent();
}

/**
 * Utility function to clear all backup data (use with caution).
 * This function removes all rows except the header.
 */
function clearBackupData() {
  const spreadsheet = SpreadsheetApp.openById(SHEET_ID);
  const sheet = spreadsheet.getSheetByName(SHEET_NAME);
  
  if (sheet) {
    const lastRow = sheet.getLastRow();
    if (lastRow > 1) {
      sheet.getRange(2, 1, lastRow - 1, sheet.getLastColumn()).clear();
      console.log(`Cleared ${lastRow - 1} rows of backup data`);
    }
  }
}

/**
 * Utility function to get backup statistics.
 */
function getBackupStats() {
  const spreadsheet = SpreadsheetApp.openById(SHEET_ID);
  const sheet = spreadsheet.getSheetByName(SHEET_NAME);
  
  if (!sheet) {
    return { error: 'Backup sheet not found' };
  }
  
  const lastRow = sheet.getLastRow();
  const totalRows = lastRow > 1 ? lastRow - 1 : 0; // Subtract header row
  
  return {
    totalBackups: totalRows,
    lastBackup: totalRows > 0 ? sheet.getRange(lastRow, 1).getValue() : null,
    sheetUrl: `https://docs.google.com/spreadsheets/d/${SHEET_ID}/edit#gid=${sheet.getSheetId()}`
  };
}

/**
 * Setup function to initialize the backup service.
 * Run this once after creating the script.
 */
function setupBackupService() {
  console.log('Setting up ChotuJobs backup service...');
  
  // Create the backup sheet
  const spreadsheet = SpreadsheetApp.openById(SHEET_ID);
  let sheet = spreadsheet.getSheetByName(SHEET_NAME);
  
  if (!sheet) {
    sheet = spreadsheet.insertSheet(SHEET_NAME);
    setupSheetHeaders(sheet);
    console.log('Created backup sheet with headers');
  } else {
    console.log('Backup sheet already exists');
  }
  
  // Test the service
  const testResult = testBackupService();
  console.log('Test completed:', testResult);
  
  return {
    success: true,
    message: 'Backup service setup completed',
    sheetUrl: `https://docs.google.com/spreadsheets/d/${SHEET_ID}/edit#gid=${sheet.getSheetId()}`
  };
}
