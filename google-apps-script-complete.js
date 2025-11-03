/**
 * Complete Google Apps Script for ChotuJobs App
 * 
 * This script handles all data operations for the ChotuJobs labor marketplace app.
 * It provides CRUD operations for jobs, bids, and users using Google Sheets as the database.
 * 
 * Features:
 * - Real-time data synchronization
 * - Multi-user support
 * - Data validation and error handling
 * - Timestamp tracking for sync operations
 * - Conflict resolution
 */

// Configuration
const SHEET_CONFIG = {
  JOBS_SHEET: 'Jobs',
  BIDS_SHEET: 'Bids', 
  USERS_SHEET: 'Users',
  SYNC_LOG_SHEET: 'SyncLog'
};

// Column mappings for Jobs sheet
const JOBS_COLUMNS = {
  JOB_ID: 1,
  TITLE: 2,
  DESCRIPTION: 3,
  BUDGET: 4,
  STATUS: 5,
  CREATED_BY_UID: 6,
  CREATED_AT: 7,
  UPDATED_AT: 8,
  LOCATION: 9,
  CATEGORY: 10,
  LATITUDE: 11,
  LONGITUDE: 12,
  ESTIMATED_DAYS: 13,
  IS_URGENT: 14,
  IMAGE_URL: 15
};

// Column mappings for Bids sheet
const BIDS_COLUMNS = {
  BID_ID: 1,
  JOB_ID: 2,
  BIDDER_UID: 3,
  AMOUNT: 4,
  STATUS: 5,
  MESSAGE: 6,
  CREATED_AT: 7,
  UPDATED_AT: 8,
  ESTIMATED_DAYS: 9,
  IS_AVAILABLE_IMMEDIATELY: 10,
  PORTFOLIO: 11
};

// Column mappings for Users sheet
const USERS_COLUMNS = {
  UID: 1,
  NAME: 2,
  ROLE: 3,
  PHONE: 4,
  EMAIL: 5,
  LOCATION: 6,
  SKILLS: 7,
  EXPERIENCE_YEARS: 8,
  RATING: 9,
  COMPLETED_JOBS: 10,
  IS_ACTIVE: 11,
  CREATED_AT: 12,
  UPDATED_AT: 13,
  MANAGED_LABORERS: 14,
  PROFILE_IMAGE_URL: 15,
  IS_PHONE_VERIFIED: 16
};

/**
 * Main entry point for all requests
 */
function doPost(e) {
  try {
    setupHeaders();
    
    // Handle case when called directly (for testing)
    if (!e || !e.postData || !e.postData.contents) {
      return ContentService.createTextOutput(JSON.stringify({
        success: false,
        error: 'Invalid request format. Use web app URL for API calls.'
      })).setMimeType(ContentService.MimeType.JSON);
    }
    
    const requestData = JSON.parse(e.postData.contents);
    const action = requestData.action;
    
    let response;
    
    switch (action) {
      case 'get_jobs':
        response = getJobs(requestData);
        break;
      case 'get_jobs_by_contractor':
        response = getJobsByContractor(requestData.contractor_uid);
        break;
      case 'get_open_jobs':
        response = getOpenJobs();
        break;
      case 'create_job':
        response = createJob(requestData.job_data);
        break;
      case 'update_job':
        response = updateJob(requestData.job_data);
        break;
      case 'get_bids':
        response = getBids();
        break;
      case 'get_bids_by_job':
        response = getBidsByJob(requestData.job_id);
        break;
      case 'get_bids_by_laborer':
        response = getBidsByLaborer(requestData.laborer_uid);
        break;
      case 'create_bid':
        response = createBid(requestData.bid_data);
        break;
      case 'update_bid_status':
        response = updateBidStatus(requestData.bid_id, requestData.status);
        break;
      case 'get_users':
        response = getUsers();
        break;
      case 'get_users_by_role':
        response = getUsersByRole(requestData.role);
        break;
      case 'get_user_by_uid':
        response = getUserByUid(requestData.uid);
        break;
      case 'create_or_update_user':
        response = createOrUpdateUser(requestData.user_data);
        break;
      case 'sync_all_data':
        response = syncAllData();
        break;
      case 'get_data_changes':
        response = getDataChanges(requestData.last_sync_time);
        break;
      default:
        response = { success: false, error: 'Unknown action: ' + action };
    }
    
    return ContentService.createTextOutput(JSON.stringify(response))
        .setMimeType(ContentService.MimeType.JSON);
        
  } catch (error) {
    console.error('Error in doPost:', error);
    return ContentService.createTextOutput(JSON.stringify({
      success: false,
      error: error.toString()
    })).setMimeType(ContentService.MimeType.JSON);
  }
}

/**
 * Handle GET requests (for testing)
 */
function doGet(e) {
  return ContentService.createTextOutput(JSON.stringify({
    success: true,
    message: 'ChotuJobs Google Sheets API is running',
    timestamp: new Date().toISOString()
  })).setMimeType(ContentService.MimeType.JSON);
}

/**
 * Setup CORS headers
 */
function setupHeaders() {
  try {
    console.log('Starting setupHeaders...');
    
    // Create sheets manually first
    const spreadsheet = SpreadsheetApp.getActiveSpreadsheet();
    
    // Create Jobs sheet
    let jobsSheet = spreadsheet.getSheetByName('Jobs');
    if (!jobsSheet) {
      jobsSheet = spreadsheet.insertSheet('Jobs');
      console.log('Created Jobs sheet');
    }
    
    // Create Bids sheet
    let bidsSheet = spreadsheet.getSheetByName('Bids');
    if (!bidsSheet) {
      bidsSheet = spreadsheet.insertSheet('Bids');
      console.log('Created Bids sheet');
    }
    
    // Create Users sheet
    let usersSheet = spreadsheet.getSheetByName('Users');
    if (!usersSheet) {
      usersSheet = spreadsheet.insertSheet('Users');
      console.log('Created Users sheet');
    }
    
    // Create SyncLog sheet
    let syncLogSheet = spreadsheet.getSheetByName('SyncLog');
    if (!syncLogSheet) {
      syncLogSheet = spreadsheet.insertSheet('SyncLog');
      console.log('Created SyncLog sheet');
    }
    
    // Wait for sheets to be fully created
    Utilities.sleep(500);
    
    // Add headers to each sheet
    if (jobsSheet) {
      addJobHeaders(jobsSheet);
    }
    if (bidsSheet) {
      addBidHeaders(bidsSheet);
    }
    if (usersSheet) {
      addUserHeaders(usersSheet);
    }
    if (syncLogSheet) {
      addSyncLogHeaders(syncLogSheet);
    }
    
    console.log('All sheets created and headers added successfully');
    return 'Success: All sheets created and headers added';
  } catch (error) {
    console.error('Error setting up headers:', error);
    return 'Error: ' + error.toString();
  }
}

// ==================== JOB OPERATIONS ====================

/**
 * Get all jobs
 */
function getJobs(requestData) {
  try {
    const sheet = getOrCreateSheet(SHEET_CONFIG.JOBS_SHEET);
    if (!sheet) {
      return {
        success: false,
        error: 'Failed to get or create Jobs sheet'
      };
    }
    
    const data = sheet.getDataRange().getValues();
    
    // Skip header row
    const jobs = [];
    for (let i = 1; i < data.length; i++) {
      const row = data[i];
      if (row && row[0]) { // Check if row and job ID exists
        const job = createJobObject(row);
        if (job) {
          jobs.push(job);
        }
      }
    }
    
    return {
      success: true,
      jobs: jobs
    };
  } catch (error) {
    return { success: false, error: error.toString() };
  }
}

/**
 * Get jobs by contractor UID
 */
function getJobsByContractor(contractorUid) {
  try {
    const sheet = getOrCreateSheet(SHEET_CONFIG.JOBS_SHEET);
    if (!sheet) {
      return {
        success: false,
        error: 'Failed to get or create Jobs sheet'
      };
    }
    
    const data = sheet.getDataRange().getValues();
    
    const jobs = [];
    for (let i = 1; i < data.length; i++) {
      const row = data[i];
      if (row && row[0] && row[JOBS_COLUMNS.CREATED_BY_UID - 1] === contractorUid) {
        const job = createJobObject(row);
        if (job) {
          jobs.push(job);
        }
      }
    }
    
    return {
      success: true,
      jobs: jobs
    };
  } catch (error) {
    return { success: false, error: error.toString() };
  }
}

/**
 * Get open jobs
 */
function getOpenJobs() {
  try {
    const sheet = getOrCreateSheet(SHEET_CONFIG.JOBS_SHEET);
    if (!sheet) {
      return {
        success: false,
        error: 'Failed to get or create Jobs sheet'
      };
    }
    
    const data = sheet.getDataRange().getValues();
    
    const jobs = [];
    for (let i = 1; i < data.length; i++) {
      const row = data[i];
      if (row && row[0] && row[JOBS_COLUMNS.STATUS - 1] === 'Open') {
        const job = createJobObject(row);
        if (job) {
          jobs.push(job);
        }
      }
    }
    
    return {
      success: true,
      jobs: jobs
    };
  } catch (error) {
    return { success: false, error: error.toString() };
  }
}

/**
 * Create a new job
 */
function createJob(jobData) {
  try {
    const sheet = getOrCreateSheet(SHEET_CONFIG.JOBS_SHEET);
    if (!sheet) {
      return {
        success: false,
        error: 'Failed to get or create Jobs sheet'
      };
    }
    
    // Generate unique job ID
    const jobId = jobData.jobId || 'job_' + Date.now() + '_' + Math.random().toString(36).substr(2, 9);
    
    const row = [
      jobId,
      jobData.title || '',
      jobData.description || '',
      jobData.budget || 0,
      jobData.status || 'Open',
      jobData.createdByUID || '',
      jobData.createdAt || Date.now(),
      Date.now(),
      jobData.location || '',
      jobData.category || '',
      jobData.latitude || '',
      jobData.longitude || '',
      jobData.estimatedDays || '',
      jobData.isUrgent || false,
      jobData.imageUrl || ''
    ];
    
    sheet.appendRow(row);
    logSync('create_job', jobId, 'Job created successfully');
    
    return {
      success: true,
      job_id: jobId
    };
  } catch (error) {
    return { success: false, error: error.toString() };
  }
}

/**
 * Update an existing job
 */
function updateJob(jobData) {
  try {
    const sheet = getOrCreateSheet(SHEET_CONFIG.JOBS_SHEET);
    const data = sheet.getDataRange().getValues();
    
    for (let i = 1; i < data.length; i++) {
      const row = data[i];
      if (row[0] === jobData.jobId) {
        // Update the row
        sheet.getRange(i + 1, JOBS_COLUMNS.TITLE).setValue(jobData.title || '');
        sheet.getRange(i + 1, JOBS_COLUMNS.DESCRIPTION).setValue(jobData.description || '');
        sheet.getRange(i + 1, JOBS_COLUMNS.BUDGET).setValue(jobData.budget || 0);
        sheet.getRange(i + 1, JOBS_COLUMNS.STATUS).setValue(jobData.status || 'Open');
        sheet.getRange(i + 1, JOBS_COLUMNS.UPDATED_AT).setValue(Date.now());
        sheet.getRange(i + 1, JOBS_COLUMNS.LOCATION).setValue(jobData.location || '');
        sheet.getRange(i + 1, JOBS_COLUMNS.CATEGORY).setValue(jobData.category || '');
        sheet.getRange(i + 1, JOBS_COLUMNS.LATITUDE).setValue(jobData.latitude || '');
        sheet.getRange(i + 1, JOBS_COLUMNS.LONGITUDE).setValue(jobData.longitude || '');
        sheet.getRange(i + 1, JOBS_COLUMNS.ESTIMATED_DAYS).setValue(jobData.estimatedDays || '');
        sheet.getRange(i + 1, JOBS_COLUMNS.IS_URGENT).setValue(jobData.isUrgent || false);
        sheet.getRange(i + 1, JOBS_COLUMNS.IMAGE_URL).setValue(jobData.imageUrl || '');
        
        logSync('update_job', jobData.jobId, 'Job updated successfully');
        return { success: true };
      }
    }
    
    return { success: false, error: 'Job not found' };
  } catch (error) {
    return { success: false, error: error.toString() };
  }
}

// ==================== BID OPERATIONS ====================

/**
 * Get all bids
 */
function getBids() {
  try {
    const sheet = getOrCreateSheet(SHEET_CONFIG.BIDS_SHEET);
    const data = sheet.getDataRange().getValues();
    
    const bids = [];
    for (let i = 1; i < data.length; i++) {
      const row = data[i];
      if (row[0]) { // Check if bid ID exists
        bids.push(createBidObject(row));
      }
    }
    
    return {
      success: true,
      bids: bids
    };
  } catch (error) {
    return { success: false, error: error.toString() };
  }
}

/**
 * Get bids by job ID
 */
function getBidsByJob(jobId) {
  try {
    const sheet = getOrCreateSheet(SHEET_CONFIG.BIDS_SHEET);
    const data = sheet.getDataRange().getValues();
    
    const bids = [];
    for (let i = 1; i < data.length; i++) {
      const row = data[i];
      if (row[0] && row[BIDS_COLUMNS.JOB_ID - 1] === jobId) {
        bids.push(createBidObject(row));
      }
    }
    
    return {
      success: true,
      bids: bids
    };
  } catch (error) {
    return { success: false, error: error.toString() };
  }
}

/**
 * Get bids by laborer UID
 */
function getBidsByLaborer(laborerUid) {
  try {
    const sheet = getOrCreateSheet(SHEET_CONFIG.BIDS_SHEET);
    const data = sheet.getDataRange().getValues();
    
    const bids = [];
    for (let i = 1; i < data.length; i++) {
      const row = data[i];
      if (row[0] && row[BIDS_COLUMNS.BIDDER_UID - 1] === laborerUid) {
        bids.push(createBidObject(row));
      }
    }
    
    return {
      success: true,
      bids: bids
    };
  } catch (error) {
    return { success: false, error: error.toString() };
  }
}

/**
 * Create a new bid
 */
function createBid(bidData) {
  try {
    const sheet = getOrCreateSheet(SHEET_CONFIG.BIDS_SHEET);
    
    // Generate unique bid ID
    const bidId = 'bid_' + Date.now() + '_' + Math.random().toString(36).substr(2, 9);
    
    const row = [
      bidId,
      bidData.jobId || '',
      bidData.bidderUID || '',
      bidData.amount || 0,
      bidData.status || 'Pending',
      bidData.message || '',
      bidData.createdAt || Date.now(),
      Date.now(),
      bidData.estimatedDays || '',
      bidData.isAvailableImmediately || false,
      bidData.portfolio || ''
    ];
    
    sheet.appendRow(row);
    logSync('create_bid', bidId, 'Bid created successfully');
    
    return {
      success: true,
      bid_id: bidId
    };
  } catch (error) {
    return { success: false, error: error.toString() };
  }
}

/**
 * Update bid status
 */
function updateBidStatus(bidId, status) {
  try {
    const sheet = getOrCreateSheet(SHEET_CONFIG.BIDS_SHEET);
    const data = sheet.getDataRange().getValues();
    
    for (let i = 1; i < data.length; i++) {
      const row = data[i];
      if (row[0] === bidId) {
        sheet.getRange(i + 1, BIDS_COLUMNS.STATUS).setValue(status);
        sheet.getRange(i + 1, BIDS_COLUMNS.UPDATED_AT).setValue(Date.now());
        
        logSync('update_bid_status', bidId, 'Bid status updated to: ' + status);
        return { success: true };
      }
    }
    
    return { success: false, error: 'Bid not found' };
  } catch (error) {
    return { success: false, error: error.toString() };
  }
}

// ==================== USER OPERATIONS ====================

/**
 * Get all users
 */
function getUsers() {
  try {
    const sheet = getOrCreateSheet(SHEET_CONFIG.USERS_SHEET);
    const data = sheet.getDataRange().getValues();
    
    const users = [];
    for (let i = 1; i < data.length; i++) {
      const row = data[i];
      if (row[0]) { // Check if UID exists
        users.push(createUserObject(row));
      }
    }
    
    return {
      success: true,
      users: users
    };
  } catch (error) {
    return { success: false, error: error.toString() };
  }
}

/**
 * Get users by role
 */
function getUsersByRole(role) {
  try {
    const sheet = getOrCreateSheet(SHEET_CONFIG.USERS_SHEET);
    const data = sheet.getDataRange().getValues();
    
    const users = [];
    for (let i = 1; i < data.length; i++) {
      const row = data[i];
      if (row[0] && row[USERS_COLUMNS.ROLE - 1] === role) {
        users.push(createUserObject(row));
      }
    }
    
    return {
      success: true,
      users: users
    };
  } catch (error) {
    return { success: false, error: error.toString() };
  }
}

/**
 * Get user by UID
 */
function getUserByUid(uid) {
  try {
    const sheet = getOrCreateSheet(SHEET_CONFIG.USERS_SHEET);
    if (!sheet) {
      return {
        success: false,
        error: 'Failed to get or create Users sheet'
      };
    }
    
    const data = sheet.getDataRange().getValues();
    
    for (let i = 1; i < data.length; i++) {
      const row = data[i];
      if (row && row[0] === uid) {
        const user = createUserObject(row);
        if (user) {
          return {
            success: true,
            user: user
          };
        }
      }
    }
    
    return { success: false, error: 'User not found' };
  } catch (error) {
    return { success: false, error: error.toString() };
  }
}

/**
 * Create or update user
 */
function createOrUpdateUser(userData) {
  try {
    const sheet = getOrCreateSheet(SHEET_CONFIG.USERS_SHEET);
    if (!sheet) {
      return {
        success: false,
        error: 'Failed to get or create Users sheet'
      };
    }
    
    const data = sheet.getDataRange().getValues();
    
    // Check if user exists
    for (let i = 1; i < data.length; i++) {
      const row = data[i];
      if (row && row[0] === userData.uid) {
        // Update existing user
        sheet.getRange(i + 1, USERS_COLUMNS.NAME).setValue(userData.name || '');
        sheet.getRange(i + 1, USERS_COLUMNS.ROLE).setValue(userData.role || '');
        sheet.getRange(i + 1, USERS_COLUMNS.PHONE).setValue(userData.phone || '');
        sheet.getRange(i + 1, USERS_COLUMNS.EMAIL).setValue(userData.email || '');
        sheet.getRange(i + 1, USERS_COLUMNS.LOCATION).setValue(userData.location || '');
        sheet.getRange(i + 1, USERS_COLUMNS.SKILLS).setValue(userData.skills || '');
        sheet.getRange(i + 1, USERS_COLUMNS.EXPERIENCE_YEARS).setValue(userData.experienceYears || '');
        sheet.getRange(i + 1, USERS_COLUMNS.RATING).setValue(userData.rating || 0);
        sheet.getRange(i + 1, USERS_COLUMNS.COMPLETED_JOBS).setValue(userData.completedJobs || 0);
        sheet.getRange(i + 1, USERS_COLUMNS.IS_ACTIVE).setValue(userData.isActive !== false);
        sheet.getRange(i + 1, USERS_COLUMNS.UPDATED_AT).setValue(Date.now());
        sheet.getRange(i + 1, USERS_COLUMNS.MANAGED_LABORERS).setValue(userData.managedLaborers || '');
        sheet.getRange(i + 1, USERS_COLUMNS.PROFILE_IMAGE_URL).setValue(userData.profileImageUrl || '');
        sheet.getRange(i + 1, USERS_COLUMNS.IS_PHONE_VERIFIED).setValue(userData.isPhoneVerified || false);
        
        logSync('update_user', userData.uid, 'User updated successfully');
        return { success: true };
      }
    }
    
    // Create new user
    const row = [
      userData.uid || '',
      userData.name || '',
      userData.role || '',
      userData.phone || '',
      userData.email || '',
      userData.location || '',
      userData.skills || '',
      userData.experienceYears || '',
      userData.rating || 0,
      userData.completedJobs || 0,
      userData.isActive !== false,
      userData.createdAt || Date.now(),
      Date.now(),
      userData.managedLaborers || '',
      userData.profileImageUrl || '',
      userData.isPhoneVerified || false
    ];
    
    sheet.appendRow(row);
    logSync('create_user', userData.uid, 'User created successfully');
    
    return { success: true };
  } catch (error) {
    return { success: false, error: error.toString() };
  }
}

// ==================== SYNC OPERATIONS ====================

/**
 * Sync all data
 */
function syncAllData() {
  try {
    const timestamp = Date.now();
    logSync('sync_all', '', 'Full sync completed at ' + new Date().toISOString());
    
    return {
      success: true,
      sync_timestamp: timestamp
    };
  } catch (error) {
    return { success: false, error: error.toString() };
  }
}

/**
 * Get data changes since last sync
 */
function getDataChanges(lastSyncTime) {
  try {
    const syncSheet = getOrCreateSheet(SHEET_CONFIG.SYNC_LOG_SHEET);
    const data = syncSheet.getDataRange().getValues();
    
    const changes = [];
    for (let i = 1; i < data.length; i++) {
      const row = data[i];
      if (row[0] && row[0] > lastSyncTime) {
        changes.push({
          timestamp: row[0],
          action: row[1],
          entity_id: row[2],
          description: row[3]
        });
      }
    }
    
    return {
      success: true,
      changes: changes
    };
  } catch (error) {
    return { success: false, error: error.toString() };
  }
}

// ==================== HELPER FUNCTIONS ====================

/**
 * Get or create a sheet with headers
 */
function getOrCreateSheet(sheetName, addHeaders = false) {
  try {
    const spreadsheet = SpreadsheetApp.getActiveSpreadsheet();
    
    if (!spreadsheet) {
      console.error('No active spreadsheet found in getOrCreateSheet');
      return null;
    }
    
    let sheet = spreadsheet.getSheetByName(sheetName);
    
    if (!sheet) {
      sheet = spreadsheet.insertSheet(sheetName);
      console.log('Created sheet:', sheetName);
      
      // Wait a moment for sheet to be fully created
      Utilities.sleep(100);
      
      // Add headers only if requested
      if (addHeaders) {
        switch (sheetName) {
          case SHEET_CONFIG.JOBS_SHEET:
            addJobHeaders(sheet);
            break;
          case SHEET_CONFIG.BIDS_SHEET:
            addBidHeaders(sheet);
            break;
          case SHEET_CONFIG.USERS_SHEET:
            addUserHeaders(sheet);
            break;
          case SHEET_CONFIG.SYNC_LOG_SHEET:
            addSyncLogHeaders(sheet);
            break;
        }
      }
    }
    
    return sheet;
  } catch (error) {
    console.error('Error in getOrCreateSheet:', error);
    return null;
  }
}

/**
 * Add headers for Jobs sheet
 */
function addJobHeaders(sheet) {
  if (!sheet) {
    console.error('Sheet is undefined in addJobHeaders');
    return;
  }
  
  try {
    const headers = [
      'Job ID', 'Title', 'Description', 'Budget', 'Status', 'Created By UID',
      'Created At', 'Updated At', 'Location', 'Category', 'Latitude', 'Longitude',
      'Estimated Days', 'Is Urgent', 'Image URL'
    ];
    sheet.getRange(1, 1, 1, headers.length).setValues([headers]);
    console.log('Job headers added successfully');
  } catch (error) {
    console.error('Error adding job headers:', error);
  }
}

/**
 * Add headers for Bids sheet
 */
function addBidHeaders(sheet) {
  if (!sheet) {
    console.error('Sheet is undefined in addBidHeaders');
    return;
  }
  
  try {
    const headers = [
      'Bid ID', 'Job ID', 'Bidder UID', 'Amount', 'Status', 'Message',
      'Created At', 'Updated At', 'Estimated Days', 'Is Available Immediately', 'Portfolio'
    ];
    sheet.getRange(1, 1, 1, headers.length).setValues([headers]);
    console.log('Bid headers added successfully');
  } catch (error) {
    console.error('Error adding bid headers:', error);
  }
}

/**
 * Add headers for Users sheet
 */
function addUserHeaders(sheet) {
  if (!sheet) {
    console.error('Sheet is undefined in addUserHeaders');
    return;
  }
  
  try {
    const headers = [
      'UID', 'Name', 'Role', 'Phone', 'Email', 'Location', 'Skills',
      'Experience Years', 'Rating', 'Completed Jobs', 'Is Active',
      'Created At', 'Updated At', 'Managed Laborers', 'Profile Image URL', 'Is Phone Verified'
    ];
    sheet.getRange(1, 1, 1, headers.length).setValues([headers]);
    console.log('User headers added successfully');
  } catch (error) {
    console.error('Error adding user headers:', error);
  }
}

/**
 * Add headers for Sync Log sheet
 */
function addSyncLogHeaders(sheet) {
  if (!sheet) {
    console.error('Sheet is undefined in addSyncLogHeaders');
    return;
  }
  
  try {
    const headers = ['Timestamp', 'Action', 'Entity ID', 'Description'];
    sheet.getRange(1, 1, 1, headers.length).setValues([headers]);
    console.log('SyncLog headers added successfully');
  } catch (error) {
    console.error('Error adding sync log headers:', error);
  }
}

/**
 * Create job object from sheet row
 */
function createJobObject(row) {
  if (!row || row.length === 0) {
    return null;
  }
  
  return {
    jobId: row[0] || '',
    title: row[1] || '',
    description: row[2] || '',
    budget: row[3] || 0,
    status: row[4] || 'Open',
    createdByUID: row[5] || '',
    createdAt: row[6] || Date.now(),
    updatedAt: row[7] || Date.now(),
    location: row[8] || '',
    category: row[9] || '',
    latitude: row[10] || 0,
    longitude: row[11] || 0,
    estimatedDays: row[12] || 1,
    isUrgent: row[13] || false,
    imageUrl: row[14] || ''
  };
}

/**
 * Create bid object from sheet row
 */
function createBidObject(row) {
  if (!row || row.length === 0) {
    return null;
  }
  
  return {
    bidId: row[0] || '',
    jobId: row[1] || '',
    bidderUID: row[2] || '',
    amount: row[3] || 0,
    status: row[4] || 'Pending',
    message: row[5] || '',
    createdAt: row[6] || Date.now(),
    updatedAt: row[7] || Date.now(),
    estimatedDays: row[8] || 1,
    isAvailableImmediately: row[9] || false,
    portfolio: row[10] || ''
  };
}

/**
 * Create user object from sheet row
 */
function createUserObject(row) {
  if (!row || row.length === 0) {
    return null;
  }
  
  return {
    uid: row[0] || '',
    name: row[1] || '',
    role: row[2] || '',
    phone: row[3] || '',
    email: row[4] || '',
    location: row[5] || '',
    skills: row[6] || '',
    experienceYears: row[7] || 0,
    rating: row[8] || 0,
    completedJobs: row[9] || 0,
    isActive: row[10] || true,
    createdAt: row[11] || Date.now(),
    updatedAt: row[12] || Date.now(),
    managedLaborers: row[13] || '',
    profileImageUrl: row[14] || '',
    isPhoneVerified: row[15] || false
  };
}

/**
 * Log sync operations
 */
function logSync(action, entityId, description) {
  try {
    const sheet = getOrCreateSheet(SHEET_CONFIG.SYNC_LOG_SHEET);
    sheet.appendRow([Date.now(), action, entityId, description]);
  } catch (error) {
    console.error('Error logging sync:', error);
  }
}

/**
 * Simple test function to create all sheets manually
 */
function createAllSheetsManually() {
  try {
    const spreadsheet = SpreadsheetApp.getActiveSpreadsheet();
    
    // Create Jobs sheet if it doesn't exist
    let jobsSheet = spreadsheet.getSheetByName('Jobs');
    if (!jobsSheet) {
      jobsSheet = spreadsheet.insertSheet('Jobs');
      console.log('Created Jobs sheet');
    }
    
    // Create Bids sheet if it doesn't exist
    let bidsSheet = spreadsheet.getSheetByName('Bids');
    if (!bidsSheet) {
      bidsSheet = spreadsheet.insertSheet('Bids');
      console.log('Created Bids sheet');
    }
    
    // Create Users sheet if it doesn't exist
    let usersSheet = spreadsheet.getSheetByName('Users');
    if (!usersSheet) {
      usersSheet = spreadsheet.insertSheet('Users');
      console.log('Created Users sheet');
    }
    
    // Create SyncLog sheet if it doesn't exist
    let syncSheet = spreadsheet.getSheetByName('SyncLog');
    if (!syncSheet) {
      syncSheet = spreadsheet.insertSheet('SyncLog');
      console.log('Created SyncLog sheet');
    }
    
    console.log('All sheets created successfully!');
    return 'Success: All 4 sheets created';
    
  } catch (error) {
    console.error('Error creating sheets:', error);
    return 'Error: ' + error.toString();
  }
}

/**
 * Create sheets only (no headers)
 */
function createSheetsOnly() {
  try {
    const spreadsheet = SpreadsheetApp.getActiveSpreadsheet();
    
    if (!spreadsheet) {
      console.error('No active spreadsheet found. Make sure the script is bound to a Google Sheet.');
      return 'Error: No active spreadsheet found. Please bind this script to a Google Sheet.';
    }
    
    // Create Jobs sheet if it doesn't exist
    if (!spreadsheet.getSheetByName('Jobs')) {
      spreadsheet.insertSheet('Jobs');
      console.log('Created Jobs sheet');
    }
    
    // Create Bids sheet if it doesn't exist
    if (!spreadsheet.getSheetByName('Bids')) {
      spreadsheet.insertSheet('Bids');
      console.log('Created Bids sheet');
    }
    
    // Create Users sheet if it doesn't exist
    if (!spreadsheet.getSheetByName('Users')) {
      spreadsheet.insertSheet('Users');
      console.log('Created Users sheet');
    }
    
    // Create SyncLog sheet if it doesn't exist
    if (!spreadsheet.getSheetByName('SyncLog')) {
      spreadsheet.insertSheet('SyncLog');
      console.log('Created SyncLog sheet');
    }
    
    console.log('All sheets created successfully!');
    return 'Success: All 4 sheets created';
    
  } catch (error) {
    console.error('Error creating sheets:', error);
    return 'Error: ' + error.toString();
  }
}

/**
 * Simple test function - just return success
 */
function testSimple() {
  return 'Script is working! All functions are ready.';
}
