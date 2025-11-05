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

    public void updateUserProfile(String userId, User user, OnCompleteListener<Boolean> listener) {
        DocumentReference userRef = db.collection(COLLECTION_USERS).document(userId);
        db.runTransaction(transaction -> {
            transaction.update(userRef, "name", user.getName());
            transaction.update(userRef, "skills", user.getSkills());
            transaction.update(userRef, "address", user.getAddress());
            transaction.update(userRef, "yearsOfExperience", user.getYearsOfExperience());
            if (user.getProfileImageUrl() != null) {
                transaction.update(userRef, "profileImageUrl", user.getProfileImageUrl());
            }
            return null;
        }).addOnSuccessListener(aVoid -> listener.onComplete(true))
                .addOnFailureListener(e -> listener.onComplete(false));
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
        job.setStatus("active");
        job.setTimestamp(null); // Firestore will set this with @ServerTimestamp
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
        bid.setStatus("pending");
        bid.setTimestamp(null);
        db.collection(COLLECTION_JOBS).document(bid.getJobId())
                .collection(SUBCOLLECTION_BIDS).add(bid)
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
        String chatId = (userId1.compareTo(userId2) > 0) ? userId1 + userId2 : userId2 + userId1;
        DocumentReference chatRef = db.collection(COLLECTION_CHATS).document(chatId);

        chatRef.get().addOnSuccessListener(documentSnapshot -> {
            if (!documentSnapshot.exists()) {
                Map<String, Object> chatData = new HashMap<>();
                chatData.put("userIds", Arrays.asList(userId1, userId2));
                chatRef.set(chatData)
                        .addOnSuccessListener(aVoid -> listener.onComplete(chatId))
                        .addOnFailureListener(e -> listener.onComplete(null));
            } else {
                listener.onComplete(chatId);
            }
        }).addOnFailureListener(e -> listener.onComplete(null));
    }

    public void sendMessage(String chatId, Message message, OnCompleteListener<Boolean> listener) {
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
                .addOnSuccessListener(aVoid -> listener.onComplete(true))
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
