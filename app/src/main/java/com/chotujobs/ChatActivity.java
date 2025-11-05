package com.chotujobs;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.chotujobs.adapters.MessagesAdapter;
import com.chotujobs.databinding.ActivityChatBinding;
import com.chotujobs.models.Message;
import com.chotujobs.services.FirestoreService;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

public class ChatActivity extends AppCompatActivity {

    private ActivityChatBinding binding;
    private MessagesAdapter adapter;
    private List<Message> messageList;
    private FirestoreService firestoreService;
    private String chatId;
    private String receiverId;
    private ListenerRegistration messageListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityChatBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        firestoreService = FirestoreService.getInstance();
        chatId = getIntent().getStringExtra("chatId");
        receiverId = getIntent().getStringExtra("receiverId");

        messageList = new ArrayList<>();
        adapter = new MessagesAdapter(messageList);
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerView.setAdapter(adapter);

        binding.sendButton.setOnClickListener(v -> sendMessage());

        // Load receiver name
        loadReceiverName();

        // Initialize empty state
        updateEmptyState();

        listenForMessages();
    }

    private void loadReceiverName() {
        if (receiverId != null && !receiverId.isEmpty()) {
            firestoreService.getUserProfile(receiverId, user -> {
                if (user != null && user.getName() != null) {
                    binding.receiverNameTextView.setText(user.getName());
                } else {
                    binding.receiverNameTextView.setText("Chat");
                }
            });
        } else {
            binding.receiverNameTextView.setText("Chat");
        }
    }

    private void updateEmptyState() {
        if (messageList.isEmpty()) {
            binding.emptyStateLayout.setVisibility(View.VISIBLE);
            binding.recyclerView.setVisibility(View.GONE);
        } else {
            binding.emptyStateLayout.setVisibility(View.GONE);
            binding.recyclerView.setVisibility(View.VISIBLE);
        }
    }

    private void sendMessage() {
        String messageText = binding.messageEditText.getText().toString().trim();
        if (messageText.isEmpty()) {
            return;
        }

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "You must be logged in to send messages", Toast.LENGTH_SHORT).show();
            return;
        }
        String senderId = currentUser.getUid();
        Message message = new Message(senderId, receiverId, messageText);

        firestoreService.sendMessage(chatId, message, success -> {
            if (success) {
                binding.messageEditText.setText("");
            } else {
                Toast.makeText(this, "Failed to send message", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void listenForMessages() {
        if (chatId == null) {
            Log.e("ChatActivity", "Chat ID is null, cannot listen for messages.");
            Toast.makeText(this, "Chat ID is missing", Toast.LENGTH_SHORT).show();
            return;
        }
        
        messageListener = firestoreService.getMessages(chatId)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) {
                        Log.e("ChatActivity", "Listen failed.", e);
                        Toast.makeText(ChatActivity.this, "Error loading messages: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (snapshots == null) {
                        Log.w("ChatActivity", "Snapshot is null");
                        return;
                    }

                    // Handle initial load and updates
                    for (DocumentChange dc : snapshots.getDocumentChanges()) {
                        Message message = dc.getDocument().toObject(Message.class);
                        if (message == null || message.getSenderId() == null || message.getMessage() == null) {
                            Log.w("ChatActivity", "Skipping invalid message: " + dc.getDocument().getId());
                            continue;
                        }

                        String docId = dc.getDocument().getId();
                        message.setMessageId(docId);
                        
                        switch (dc.getType()) {
                            case ADDED:
                                // Check if message already exists to avoid duplicates
                                boolean exists = false;
                                for (Message m : messageList) {
                                    if (docId.equals(m.getMessageId())) {
                                        exists = true;
                                        break;
                                    }
                                }
                                if (!exists) {
                                    messageList.add(message);
                                    adapter.notifyItemInserted(messageList.size() - 1);
                                    binding.recyclerView.post(() -> {
                                        if (messageList.size() > 0) {
                                            binding.recyclerView.scrollToPosition(messageList.size() - 1);
                                        }
                                        updateEmptyState();
                                    });
                                }
                                break;
                            case MODIFIED:
                                // Update existing message
                                for (int i = 0; i < messageList.size(); i++) {
                                    if (docId.equals(messageList.get(i).getMessageId())) {
                                        messageList.set(i, message);
                                        adapter.notifyItemChanged(i);
                                        break;
                                    }
                                }
                                break;
                            case REMOVED:
                                // Remove message
                                for (int i = 0; i < messageList.size(); i++) {
                                    if (docId.equals(messageList.get(i).getMessageId())) {
                                        messageList.remove(i);
                                        adapter.notifyItemRemoved(i);
                                        updateEmptyState();
                                        break;
                                    }
                                }
                                break;
                        }
                    }
                    
                    // Update empty state after processing all changes
                    updateEmptyState();
                });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (messageListener != null) {
            messageListener.remove();
        }
    }
}
