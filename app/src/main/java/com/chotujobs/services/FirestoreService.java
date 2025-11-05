package com.chotujobs.services;

import android.util.Log;
import com.chotujobs.models.Bid;
import com.chotujobs.models.Chat;
import com.chotujobs.models.Job;
import com.chotujobs.models.Message;
import com.chotujobs.models.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.WriteBatch;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FirestoreService {

    private static final String TAG = "FirestoreService";
    private static FirestoreService instance;
    private FirebaseFirestore db;
    private FirebaseAuth auth;

    // Collection names
    private static final String COLLECTION_JOBS = "jobs";
    private static final String COLLECTION_USERS = "users";
    private static final String SUBCOLLECTION_BIDS = "bids";
    private static final String COLLECTION_CHATS = "chats";
    private static final String SUBCOLLECTION_MESSAGES = "messages";

    private FirestoreService() {
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
    }

    public static synchronized FirestoreService getInstance() {
        if (instance == null) {
            instance = new FirestoreService();
        }
        return instance;
    }

    public String getCurrentUserId() {
        if (auth.getCurrentUser() != null) {
            return auth.getCurrentUser().getUid();
        }
        return null;
    }

    // ========== USER METHODS ==========

    public void createUserProfile(User user, String uid, OnCompleteListener<Boolean> listener) {
        db.collection(COLLECTION_USERS).document(uid).set(user)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "User profile created successfully");
                    listener.onComplete(true);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error creating user profile", e);
                    listener.onComplete(false);
                });
    }

    public void getUserProfile(String uid, OnCompleteListener<User> listener) {
        db.collection(COLLECTION_USERS).document(uid).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        User user = documentSnapshot.toObject(User.class);
                        if (user != null) {
                            user.setUserId(uid);
                        }
                        listener.onComplete(user);
                    } else {
                        listener.onComplete(null);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error getting user profile", e);
                    listener.onComplete(null);
                });
    }

    public void getUsersByRole(String role, OnCompleteListener<List<User>> listener) {
        db.collection(COLLECTION_USERS)
                .whereEqualTo("role", role)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<User> users = new ArrayList<>();
                    for (var document : queryDocumentSnapshots.getDocuments()) {
                        User user = document.toObject(User.class);
                        if (user != null) {
                            user.setUserId(document.getId());
                            users.add(user);
                        }
                    }
                    listener.onComplete(users);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error getting users by role", e);
                    listener.onComplete(new ArrayList<>());
                });
    }

    public void getUsersByIds(List<String> userIds, OnCompleteListener<List<User>> listener) {
        if (userIds == null || userIds.isEmpty()) {
            listener.onComplete(new ArrayList<>());
            return;
        }

        // Firestore whereIn has a limit of 10 items, so we need to batch if more
        if (userIds.size() > 10) {
            Log.w(TAG, "getUsersByIds: More than 10 userIds, fetching first 10 only");
            userIds = userIds.subList(0, 10);
        }

        db.collection(COLLECTION_USERS).whereIn(com.google.firebase.firestore.FieldPath.documentId(), userIds).get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<User> users = new ArrayList<>();
                    for (var document : queryDocumentSnapshots.getDocuments()) {
                        User user = document.toObject(User.class);
                        if (user != null) {
                            user.setUserId(document.getId());
                            users.add(user);
                        } else {
                            Log.w(TAG, "Skipping null user document: " + document.getId());
                        }
                    }
                    listener.onComplete(users);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error getting users by ids", e);
                    listener.onComplete(new ArrayList<>());
                });
    }

    public void updateUserProfile(String userId, Map<String, Object> updates, OnCompleteListener<Boolean> listener) {
        if (userId == null || userId.isEmpty()) {
            Log.e(TAG, "Cannot update user profile: userId is null or empty");
            listener.onComplete(false);
            return;
        }
        if (updates == null || updates.isEmpty()) {
            Log.w(TAG, "No updates to apply for user: " + userId);
            listener.onComplete(true);
            return;
        }
        
        Log.d(TAG, "Updating user profile: " + userId + " with fields: " + updates.keySet());
        db.collection(COLLECTION_USERS).document(userId).update(updates)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "User profile updated successfully: " + userId);
                    listener.onComplete(true);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error updating user profile: " + userId, e);
                    listener.onComplete(false);
                });
    }

    // ========== JOB METHODS ==========

    public void createJob(Job job, OnCompleteListener<String> listener) {
        job.setStatus("active");
        // Persist as milliseconds since epoch to match existing data
        job.setTimestamp(System.currentTimeMillis());
        db.collection(COLLECTION_JOBS).add(job)
                .addOnSuccessListener(documentReference -> {
                    Log.d(TAG, "Job created successfully: " + documentReference.getId());
                    listener.onComplete(documentReference.getId());
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error creating job", e);
                    listener.onComplete(null);
                });
    }

    public void getAllActiveJobs(OnCompleteListener<List<Job>> listener) {
        db.collection(COLLECTION_JOBS)
                .whereEqualTo("status", "active")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Job> jobs = new ArrayList<>();
                    for (var document : queryDocumentSnapshots.getDocuments()) {
                        Job job = document.toObject(Job.class);
                        if (job != null) {
                            job.setJobId(document.getId());
                            jobs.add(job);
                        } else {
                            Log.w(TAG, "Skipping null job document: " + document.getId());
                        }
                    }
                    listener.onComplete(jobs);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error getting active jobs", e);
                    listener.onComplete(new ArrayList<>());
                });
    }

    public void getJobsByContractor(String contractorId, OnCompleteListener<List<Job>> listener) {
        db.collection(COLLECTION_JOBS)
                .whereEqualTo("contractorId", contractorId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Job> jobs = new ArrayList<>();
                    for (var document : queryDocumentSnapshots.getDocuments()) {
                        Job job = document.toObject(Job.class);
                        if (job != null) {
                            job.setJobId(document.getId());
                            jobs.add(job);
                        } else {
                            Log.w(TAG, "Skipping null job document: " + document.getId());
                        }
                    }
                    listener.onComplete(jobs);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error getting jobs by contractor", e);
                    listener.onComplete(new ArrayList<>());
                });
    }

    public void getJobById(String jobId, OnCompleteListener<Job> listener) {
        db.collection(COLLECTION_JOBS).document(jobId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Job job = documentSnapshot.toObject(Job.class);
                        if (job != null) {
                            job.setJobId(jobId);
                        }
                        listener.onComplete(job);
                    } else {
                        listener.onComplete(null);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error getting job", e);
                    listener.onComplete(null);
                });
    }

    public void updateJobStatus(String jobId, String status, String winnerUserId, OnCompleteListener<Boolean> listener) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("status", status);
        if (winnerUserId != null) {
            updates.put("winnerUserId", winnerUserId);
        }

        db.collection(COLLECTION_JOBS).document(jobId).update(updates)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Job status updated successfully");
                    listener.onComplete(true);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error updating job status", e);
                    listener.onComplete(false);
                });
    }

    // ========== BID METHODS ==========

    public void createBid(Bid bid, OnCompleteListener<String> listener) {
        if (bid == null || bid.getJobId() == null || bid.getJobId().isEmpty() 
                || bid.getBidderId() == null || bid.getBidderId().isEmpty()) {
            Log.e(TAG, "Cannot create bid: bid is invalid or missing required fields");
            listener.onComplete(null);
            return;
        }
        
        // Validate bid amount
        if (bid.getBidAmount() <= 0) {
            Log.e(TAG, "Cannot create bid: bid amount must be greater than zero");
            listener.onComplete(null);
            return;
        }
        
        // Verify bidderId matches authenticated user
        String currentUserId = getCurrentUserId();
        if (currentUserId == null || !currentUserId.equals(bid.getBidderId())) {
            Log.e(TAG, "Cannot create bid: bidderId (" + bid.getBidderId() + ") doesn't match authenticated user (" + currentUserId + ")");
            listener.onComplete(null);
            return;
        }
        
        bid.setStatus("pending");
        // Don't set timestamp - let @ServerTimestamp handle it automatically
        // bid.setTimestamp(null) would interfere with @ServerTimestamp annotation
        
        // First verify the job exists and is active before creating bid
        db.collection(COLLECTION_JOBS).document(bid.getJobId()).get()
                .addOnSuccessListener(jobSnapshot -> {
                    if (!jobSnapshot.exists()) {
                        Log.e(TAG, "Cannot create bid: job does not exist - " + bid.getJobId());
                        listener.onComplete(null);
                        return;
                    }
                    
                    String jobStatus = jobSnapshot.getString("status");
                    if (!"active".equals(jobStatus)) {
                        Log.e(TAG, "Cannot create bid: job is not active. Current status: " + jobStatus);
                        listener.onComplete(null);
                        return;
                    }
                    
                    // Job exists and is active, proceed with bid creation
                    // Verify user role before creating bid
                    db.collection(COLLECTION_USERS).document(bid.getBidderId()).get()
                            .addOnSuccessListener(userSnapshot -> {
                                if (!userSnapshot.exists()) {
                                    Log.e(TAG, "Cannot create bid: user document does not exist");
                                    listener.onComplete(null);
                                    return;
                                }
                                
                                String userRole = userSnapshot.getString("role");
                                Log.d(TAG, "User role from Firestore: " + userRole + ", bidderId: " + bid.getBidderId());
                                
                                // Normalize role to handle variations (labour, labourer, etc.)
                                String normalizedRole = (userRole != null) ? userRole.toLowerCase().trim() : "";
                                boolean isValidRole = "labour".equals(normalizedRole) || 
                                                     "labourer".equals(normalizedRole) || 
                                                     "agent".equals(normalizedRole);
                                
                                if (!isValidRole) {
                                    Log.e(TAG, "Cannot create bid: user role is '" + userRole + "' but must be 'labour' or 'agent'");
                                    listener.onComplete(null);
                                    return;
                                }
                                
                                Log.d(TAG, "Role validation passed: " + normalizedRole);
                                
                                // All checks passed, create the bid
                                db.collection(COLLECTION_JOBS).document(bid.getJobId())
                                        .collection(SUBCOLLECTION_BIDS).add(bid)
                                        .addOnSuccessListener(documentReference -> {
                                            Log.d(TAG, "Bid created successfully: " + documentReference.getId());
                                            listener.onComplete(documentReference.getId());
                                        })
                                        .addOnFailureListener(e -> {
                                            Log.e(TAG, "Error creating bid in Firestore", e);
                                            if (e instanceof com.google.firebase.firestore.FirebaseFirestoreException) {
                                                com.google.firebase.firestore.FirebaseFirestoreException firestoreException = 
                                                    (com.google.firebase.firestore.FirebaseFirestoreException) e;
                                                Log.e(TAG, "Error code: " + firestoreException.getCode() + ", Message: " + firestoreException.getMessage());
                                                
                                                if (firestoreException.getCode() == com.google.firebase.firestore.FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                                                    Log.e(TAG, "Permission denied. Possible causes:");
                                                    Log.e(TAG, "  - User role is not 'labour' or 'agent' (current: " + userRole + ")");
                                                    Log.e(TAG, "  - bidderId doesn't match auth.uid");
                                                    Log.e(TAG, "  - Job status is not 'active'");
                                                    Log.e(TAG, "  - User document doesn't exist");
                                                }
                                            }
                                            listener.onComplete(null);
                                        });
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Error checking user document", e);
                                listener.onComplete(null);
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error checking job existence", e);
                    listener.onComplete(null);
                });
    }

    public void getBidsByJob(String jobId, OnCompleteListener<List<Bid>> listener) {
        if (jobId == null || jobId.isEmpty()) {
            Log.e(TAG, "Cannot get bids: jobId is null or empty");
            listener.onComplete(new ArrayList<>());
            return;
        }
        
        db.collection(COLLECTION_JOBS).document(jobId)
                .collection(SUBCOLLECTION_BIDS)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Bid> bids = new ArrayList<>();
                    for (var document : queryDocumentSnapshots.getDocuments()) {
                        Bid bid = document.toObject(Bid.class);
                        if (bid != null) {
                            bid.setBidId(document.getId());
                            bid.setJobId(jobId);
                            bids.add(bid);
                        } else {
                            Log.w(TAG, "Skipping null bid document: " + document.getId());
                        }
                    }
                    listener.onComplete(bids);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error getting bids", e);
                    listener.onComplete(new ArrayList<>());
                });
    }

    public void updateBidStatus(String jobId, String bidId, String status, OnCompleteListener<Boolean> listener) {
        db.collection(COLLECTION_JOBS).document(jobId)
                .collection(SUBCOLLECTION_BIDS).document(bidId)
                .update("status", status)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Bid status updated successfully");
                    listener.onComplete(true);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error updating bid status", e);
                    listener.onComplete(false);
                });
    }

    // ========== CHAT METHODS ==========

    public void createChat(String userId1, String userId2, OnCompleteListener<String> listener) {
        if (userId1 == null || userId1.isEmpty() || userId2 == null || userId2.isEmpty()) {
            Log.e(TAG, "Cannot create chat: userIds are null or empty");
            listener.onComplete(null);
            return;
        }
        
        // Ensure consistent chatId by sorting user IDs
        String chatId = (userId1.compareTo(userId2) > 0) ? userId1 + userId2 : userId2 + userId1;
        DocumentReference chatRef = db.collection(COLLECTION_CHATS).document(chatId);

        chatRef.get().addOnSuccessListener(documentSnapshot -> {
            if (!documentSnapshot.exists()) {
                Map<String, Object> chatData = new HashMap<>();
                chatData.put("userIds", Arrays.asList(userId1, userId2));
                chatData.put("lastMessage", "");
                chatData.put("lastMessageTimestamp", FieldValue.serverTimestamp());
                chatRef.set(chatData)
                        .addOnSuccessListener(aVoid -> {
                            Log.d(TAG, "Chat created successfully: " + chatId);
                            // Wait a moment for Firestore to commit the chat document
                            // This ensures the security rules can read it before we send a message
                            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                                listener.onComplete(chatId);
                            }, 300);
                        })
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "Error creating chat", e);
                            listener.onComplete(null);
                        });
            } else {
                Log.d(TAG, "Chat already exists: " + chatId);
                listener.onComplete(chatId);
            }
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Error checking chat existence", e);
            listener.onComplete(null);
        });
    }

    public void sendMessage(String chatId, Message message, OnCompleteListener<Boolean> listener) {
        if (chatId == null || chatId.isEmpty()) {
            Log.e(TAG, "Cannot send message: chatId is null or empty");
            listener.onComplete(false);
            return;
        }
        if (message == null || message.getMessage() == null || message.getSenderId() == null) {
            Log.e(TAG, "Cannot send message: message is invalid");
            listener.onComplete(false);
            return;
        }

        WriteBatch batch = db.batch();

        DocumentReference messageRef = db.collection(COLLECTION_CHATS).document(chatId)
                .collection(SUBCOLLECTION_MESSAGES).document();
        batch.set(messageRef, message);

        DocumentReference chatRef = db.collection(COLLECTION_CHATS).document(chatId);
        Map<String, Object> chatUpdates = new HashMap<>();
        chatUpdates.put("lastMessage", message.getMessage());
        chatUpdates.put("lastMessageTimestamp", FieldValue.serverTimestamp());
        batch.update(chatRef, chatUpdates);

        batch.commit()
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Message sent successfully to chat: " + chatId);
                    listener.onComplete(true);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error sending message", e);
                    if (e instanceof com.google.firebase.firestore.FirebaseFirestoreException) {
                        com.google.firebase.firestore.FirebaseFirestoreException firestoreException = 
                            (com.google.firebase.firestore.FirebaseFirestoreException) e;
                        Log.e(TAG, "Error code: " + firestoreException.getCode() + ", Message: " + firestoreException.getMessage());
                        if (firestoreException.getCode() == com.google.firebase.firestore.FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                            Log.e(TAG, "Permission denied. Possible causes:");
                            Log.e(TAG, "  - User not in chat userIds array");
                            Log.e(TAG, "  - senderId (" + message.getSenderId() + ") doesn't match auth.uid (" + getCurrentUserId() + ")");
                            Log.e(TAG, "  - Chat document might not exist yet");
                        }
                    }
                    listener.onComplete(false);
                });
    }

    public Query getMessages(String chatId) {
        return db.collection(COLLECTION_CHATS).document(chatId)
                .collection(SUBCOLLECTION_MESSAGES)
                .orderBy("timestamp", Query.Direction.ASCENDING);
    }

    public Query getChatsForUser(String userId) {
        return db.collection(COLLECTION_CHATS)
                .whereArrayContains("userIds", userId)
                .orderBy("lastMessageTimestamp", Query.Direction.DESCENDING);
    }

    /**
     * Notifies the labourer that their bid has been accepted
     * @param contractorId The ID of the contractor who accepted the bid
     * @param bid The accepted bid
     * @param jobTitle The title of the job
     * @param listener Callback to indicate success/failure
     */
    public void notifyBidAccepted(String contractorId, Bid bid, String jobTitle, OnCompleteListener<Boolean> listener) {
        if (contractorId == null || contractorId.isEmpty() || bid == null) {
            Log.e(TAG, "Cannot notify bid acceptance: invalid parameters");
            listener.onComplete(false);
            return;
        }

        // Determine the correct labourer ID: if agent bid, use labourerIdIfAgent, otherwise use bidderId
        String labourerId = (bid.getLabourerIdIfAgent() != null && !bid.getLabourerIdIfAgent().isEmpty())
                ? bid.getLabourerIdIfAgent()
                : bid.getBidderId();

        if (labourerId == null || labourerId.isEmpty()) {
            Log.e(TAG, "Cannot notify bid acceptance: labourer ID is null or empty");
            listener.onComplete(false);
            return;
        }

        // Verify contractorId matches authenticated user
        String currentUserId = getCurrentUserId();
        if (currentUserId == null || !currentUserId.equals(contractorId)) {
            Log.e(TAG, "Cannot notify bid acceptance: contractorId (" + contractorId + ") doesn't match authenticated user (" + currentUserId + ")");
            listener.onComplete(false);
            return;
        }

        // Create or get existing chat between contractor and labourer
        createChat(contractorId, labourerId, chatId -> {
            if (chatId == null || chatId.isEmpty()) {
                Log.e(TAG, "Failed to create/get chat for bid acceptance notification");
                listener.onComplete(false);
                return;
            }

            // Create notification message
            String messageText = String.format("Congratulations! Your bid of ₹%.2f for the job \"%s\" has been accepted!", 
                    bid.getBidAmount(), jobTitle != null ? jobTitle : "the job");
            
            Message notificationMessage = new Message(contractorId, labourerId, messageText);

            // Send the message (chat is already committed from createChat)
            sendMessage(chatId, notificationMessage, success -> {
                if (success) {
                    Log.d(TAG, "Bid acceptance notification sent successfully to labourer: " + labourerId);
                } else {
                    Log.e(TAG, "Failed to send bid acceptance notification message");
                }
                listener.onComplete(success);
            });
        });
    }

    // ========== CALLBACK INTERFACE ==========

    public interface OnCompleteListener<T> {
        void onComplete(T result);
    }
}
