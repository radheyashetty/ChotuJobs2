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
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

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
        Map<String, Object> userData = new HashMap<>();
        userData.put("name", user.getName());
        if(user.getEmail() != null) {
            userData.put("email", user.getEmail());
        }
        if(user.getPhone() != null){
            userData.put("phone", user.getPhone());
        }
        userData.put("role", user.getRole());

        db.collection(COLLECTION_USERS).document(uid).set(userData)
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
    
    public void updateUserProfile(String userId, String name, OnCompleteListener<Boolean> listener) {
        db.collection(COLLECTION_USERS).document(userId).update("name", name)
                .addOnSuccessListener(aVoid -> listener.onComplete(true))
                .addOnFailureListener(e -> listener.onComplete(false));
    }

    // Alias method for easier usage
    public void getUser(String uid, OnCompleteListener<User> listener) {
        getUserProfile(uid, listener);
    }

    public void getAllUsers(OnCompleteListener<List<User>> listener) {
        db.collection(COLLECTION_USERS).get()
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
                    Log.e(TAG, "Error getting all users", e);
                    listener.onComplete(new ArrayList<>());
                });
    }

    // ========== JOB METHODS ==========

    public void createJob(Job job, OnCompleteListener<String> listener) {
        Map<String, Object> jobData = new HashMap<>();
        jobData.put("contractorId", job.getContractorId());
        jobData.put("title", job.getTitle());
        jobData.put("category", job.getCategory());
        jobData.put("startDate", job.getStartDate());
        jobData.put("location", job.getLocation());
        jobData.put("imagePath", job.getImagePath());
        jobData.put("status", "active");

        db.collection(COLLECTION_JOBS).add(jobData)
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
                        job.setJobId(document.getId());
                        jobs.add(job);
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
                        job.setJobId(document.getId());
                        jobs.add(job);
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
        Map<String, Object> bidData = new HashMap<>();
        bidData.put("bidderId", bid.getBidderId());
        bidData.put("bidAmount", bid.getBidAmount());
        bidData.put("labourerIdIfAgent", bid.getLabourerIdIfAgent());
        bidData.put("status", "pending");

        db.collection(COLLECTION_JOBS).document(bid.getJobId())
                .collection(SUBCOLLECTION_BIDS).add(bidData)
                .addOnSuccessListener(documentReference -> {
                    Log.d(TAG, "Bid created successfully: " + documentReference.getId());
                    listener.onComplete(documentReference.getId());
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error creating bid", e);
                    listener.onComplete(null);
                });
    }

    public void getBidsByJob(String jobId, OnCompleteListener<List<Bid>> listener) {
        db.collection(COLLECTION_JOBS).document(jobId)
                .collection(SUBCOLLECTION_BIDS)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Bid> bids = new ArrayList<>();
                    for (var document : queryDocumentSnapshots.getDocuments()) {
                        Bid bid = document.toObject(Bid.class);
                        bid.setBidId(document.getId());
                        bid.setJobId(jobId);
                        bids.add(bid);
                    }
                    listener.onComplete(bids);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error getting bids", e);
                    listener.onComplete(new ArrayList<>());
                });
    }

    public void updateBidWinner(String jobId, String bidId, OnCompleteListener<Boolean> listener) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("winnerFlag", 1);

        db.collection(COLLECTION_JOBS).document(jobId)
                .collection(SUBCOLLECTION_BIDS).document(bidId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Bid marked as winner successfully");
                    listener.onComplete(true);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error marking bid as winner", e);
                    listener.onComplete(false);
                });
    }
    
    // ========== CHAT METHODS ==========

    public void createChat(String userId1, String userId2, OnCompleteListener<String> listener) {
        String chatId = (userId1.compareTo(userId2) > 0) ? userId1 + userId2 : userId2 + userId1;
        
        db.collection(COLLECTION_CHATS).document(chatId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if(!documentSnapshot.exists()){
                        Map<String, Object> chatData = new HashMap<>();
                        chatData.put("userIds", Arrays.asList(userId1, userId2));
                        db.collection(COLLECTION_CHATS).document(chatId).set(chatData)
                                .addOnSuccessListener(aVoid -> listener.onComplete(chatId))
                                .addOnFailureListener(e -> listener.onComplete(null));
                    } else {
                        listener.onComplete(chatId);
                    }
                })
                .addOnFailureListener(e -> listener.onComplete(null));
    }
    
    public void sendMessage(String chatId, Message message, OnCompleteListener<Boolean> listener) {
        db.collection(COLLECTION_CHATS).document(chatId)
                .collection(SUBCOLLECTION_MESSAGES).add(message)
                .addOnSuccessListener(documentReference -> {
                    Map<String, Object> chatUpdates = new HashMap<>();
                    chatUpdates.put("lastMessage", message.getMessage());
                    chatUpdates.put("lastMessageTimestamp", System.currentTimeMillis());
                    db.collection(COLLECTION_CHATS).document(chatId).update(chatUpdates);
                    listener.onComplete(true);
                })
                .addOnFailureListener(e -> listener.onComplete(false));
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


    // ========== CALLBACK INTERFACE ==========

    public interface OnCompleteListener<T> {
        void onComplete(T result);
    }
}
