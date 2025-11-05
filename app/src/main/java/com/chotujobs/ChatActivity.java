package com.chotujobs;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.chotujobs.adapters.MessagesAdapter;
import com.chotujobs.databinding.ActivityChatBinding;
import com.chotujobs.models.Message;
import com.chotujobs.services.FirestoreService;
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

        if (chatId == null || chatId.isEmpty()) {
            Toast.makeText(this, "Chat ID is missing", Toast.LENGTH_SHORT).show();
            return;
        }

        String senderId = firestoreService.getCurrentUserId();
        if (senderId == null) {
            Toast.makeText(this, "You must be logged in to send messages", Toast.LENGTH_SHORT).show();
            return;
        }
        String receiver = receiverId != null ? receiverId : "";
        Message message = new Message(senderId, receiver, messageText);

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
            Toast.makeText(this, "Chat ID is missing", Toast.LENGTH_SHORT).show();
            return;
        }
        
        messageListener = firestoreService.getMessages(chatId)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) {
                        Toast.makeText(ChatActivity.this, "Error loading messages", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (snapshots == null) return;

                    // Handle initial load and updates
                    for (DocumentChange dc : snapshots.getDocumentChanges()) {
                        Message message = dc.getDocument().toObject(Message.class);
                        if (message == null || message.getSenderId() == null || message.getMessage() == null) {
                            continue;
                        }

                        String docId = dc.getDocument().getId();
                        message.setMessageId(docId);
                        
                        switch (dc.getType()) {
                            case ADDED:
                                if (!messageExists(docId)) {
                                    messageList.add(message);
                                    adapter.notifyItemInserted(messageList.size() - 1);
                                    binding.recyclerView.post(() -> {
                                        if (!messageList.isEmpty()) {
                                            binding.recyclerView.scrollToPosition(messageList.size() - 1);
                                        }
                                        updateEmptyState();
                                    });
                                }
                                break;
                            case MODIFIED:
                                updateMessage(docId, message);
                                break;
                            case REMOVED:
                                removeMessage(docId);
                                break;
                        }
                    }
                    
                    updateEmptyState();
                });
    }

    private boolean messageExists(String messageId) {
        for (Message m : messageList) {
            if (messageId.equals(m.getMessageId())) {
                return true;
            }
        }
        return false;
    }

    private void updateMessage(String messageId, Message message) {
        for (int i = 0; i < messageList.size(); i++) {
            if (messageId.equals(messageList.get(i).getMessageId())) {
                messageList.set(i, message);
                adapter.notifyItemChanged(i);
                break;
            }
        }
    }

    private void removeMessage(String messageId) {
        for (int i = 0; i < messageList.size(); i++) {
            if (messageId.equals(messageList.get(i).getMessageId())) {
                messageList.remove(i);
                adapter.notifyItemRemoved(i);
                updateEmptyState();
                break;
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (messageListener != null) {
            messageListener.remove();
        }
    }
}
