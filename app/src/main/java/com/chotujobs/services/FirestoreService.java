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
        com.google.firebase.auth.FirebaseUser user = auth.getCurrentUser();
        return user != null ? user.getUid() : null;
    }

    // ========== USER METHODS ==========

    public void createUserProfile(User user, String uid, OnCompleteListener<Boolean> listener) {
        db.collection(COLLECTION_USERS).document(uid).set(user)
                .addOnSuccessListener(aVoid -> listener.onComplete(true))
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error creating user profile: " + e.getMessage());
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
    
    public void getLabourers(OnCompleteListener<List<User>> listener) {
        db.collection(COLLECTION_USERS)
                .whereIn("role", Arrays.asList("labour", "labourer"))
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
                    Log.e(TAG, "Error getting labourers", e);
                    listener.onComplete(new ArrayList<>());
                });
    }

    public void getUsersByIds(List<String> userIds, OnCompleteListener<List<User>> listener) {
        if (userIds == null || userIds.isEmpty()) {
            listener.onComplete(new ArrayList<>());
            return;
        }

        if (userIds.size() > 10) {
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
        if (userId == null || userId.isEmpty() || updates == null || updates.isEmpty()) {
            listener.onComplete(false);
            return;
        }

        db.collection(COLLECTION_USERS).document(userId).update(updates)
                .addOnSuccessListener(aVoid -> listener.onComplete(true))
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error updating user profile: " + e.getMessage());
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
        if (!isValidBid(bid)) {
            listener.onComplete(null);
            return;
        }
        
        bid.setStatus("pending");
        checkJobThenCreateBid(bid, listener);
    }
    
    private boolean isValidBid(Bid bid) {
        if (bid == null) return false;
        if (isEmpty(bid.getJobId()) || isEmpty(bid.getBidderId())) {
            Log.e(TAG, "Bid missing required fields");
            return false;
        }
        if (bid.getBidAmount() <= 0) {
            Log.e(TAG, "Bid amount must be greater than zero");
            return false;
        }
        String currentUserId = getCurrentUserId();
        if (currentUserId == null || !currentUserId.equals(bid.getBidderId())) {
            Log.e(TAG, "Bidder ID doesn't match authenticated user");
            return false;
        }
        return true;
    }
    
    private boolean isEmpty(String str) {
        return str == null || str.isEmpty();
    }
    
    private void checkJobThenCreateBid(Bid bid, OnCompleteListener<String> listener) {
        db.collection(COLLECTION_JOBS).document(bid.getJobId()).get()
                .addOnSuccessListener(jobSnapshot -> {
                    if (!isJobActive(jobSnapshot)) {
                        listener.onComplete(null);
                        return;
                    }
                    checkUserRoleThenCreateBid(bid, listener);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error checking job", e);
                    listener.onComplete(null);
                });
    }
    
    private boolean isJobActive(com.google.firebase.firestore.DocumentSnapshot jobSnapshot) {
        if (!jobSnapshot.exists()) {
            Log.e(TAG, "Job does not exist");
            return false;
        }
        String status = jobSnapshot.getString("status");
        if (!"active".equals(status)) {
            Log.e(TAG, "Job is not active. Status: " + status);
            return false;
        }
        return true;
    }
    
    private void checkUserRoleThenCreateBid(Bid bid, OnCompleteListener<String> listener) {
        db.collection(COLLECTION_USERS).document(bid.getBidderId()).get()
                .addOnSuccessListener(userSnapshot -> {
                    if (!hasValidBidderRole(userSnapshot)) {
                        listener.onComplete(null);
                        return;
                    }
                    saveBidToFirestore(bid, listener);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error checking user role", e);
                    listener.onComplete(null);
                });
    }
    
    public static boolean canPlaceBid(String role) {
        if (role == null || role.isEmpty()) return false;
        String roleLower = role.toLowerCase().trim();
        return roleLower.equals("labour") || roleLower.equals("labourer") || roleLower.equals("agent");
    }
    
    private boolean hasValidBidderRole(com.google.firebase.firestore.DocumentSnapshot userSnapshot) {
        if (!userSnapshot.exists()) {
            Log.e(TAG, "User document does not exist");
            return false;
        }
        String role = userSnapshot.getString("role");
        return canPlaceBid(role);
    }
    
    private void saveBidToFirestore(Bid bid, OnCompleteListener<String> listener) {
        db.collection(COLLECTION_JOBS).document(bid.getJobId())
                .collection(SUBCOLLECTION_BIDS).add(bid)
                .addOnSuccessListener(documentReference -> {
                    Log.d(TAG, "Bid created: " + documentReference.getId());
                    listener.onComplete(documentReference.getId());
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error saving bid", e);
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
                    Log.e(TAG, "Error sending message: " + e.getMessage());
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

    public void notifyBidAccepted(String contractorId, Bid bid, String jobTitle, OnCompleteListener<Boolean> listener) {
        if (contractorId == null || contractorId.isEmpty() || bid == null) {
            Log.e(TAG, "Cannot notify bid acceptance: invalid parameters");
            if (listener != null) listener.onComplete(false);
            return;
        }
        
        String currentUserId = getCurrentUserId();
        if (currentUserId == null || !currentUserId.equals(contractorId)) {
            Log.e(TAG, "Only the contractor can notify bid acceptance");
            if (listener != null) listener.onComplete(false);
            return;
        }

        String tempLabourerId = bid.getLabourerIdIfAgent();
        if (tempLabourerId == null || tempLabourerId.isEmpty()) {
            tempLabourerId = bid.getBidderId();
        }
        
        final String labourerId = tempLabourerId;
        if (labourerId == null || labourerId.isEmpty()) {
            Log.e(TAG, "Cannot determine labourer ID from bid");
            if (listener != null) listener.onComplete(false);
            return;
        }

        // Create or get existing chat between contractor and labourer
        createChat(contractorId, labourerId, chatId -> {
            if (chatId == null || chatId.isEmpty()) {
                Log.e(TAG, "Failed to create/get chat for bid acceptance notification");
                if (listener != null) listener.onComplete(false);
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
                if (listener != null) listener.onComplete(success);
            });
        });
    }

    // ========== CALLBACK INTERFACE ==========

    public interface OnCompleteListener<T> {
        void onComplete(T result);
    }
}
