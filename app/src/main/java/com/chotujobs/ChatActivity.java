package com.chotujobs;

import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.chotujobs.adapters.MessagesAdapter;
import com.chotujobs.databinding.ActivityChatBinding;
import com.chotujobs.models.Message;
import com.chotujobs.services.FirestoreService;
import com.google.firebase.auth.FirebaseAuth;
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
        Log.d("ChatActivity", "onCreate: activity launched");
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

        listenForMessages();
    }

    private void sendMessage() {
        String messageText = binding.messageEditText.getText().toString().trim();
        if (messageText.isEmpty()) {
            return;
        }

        String senderId = FirebaseAuth.getInstance().getCurrentUser().getUid();
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
        messageListener = firestoreService.getMessages(chatId)
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) {
                        Log.e("ChatActivity", "Listen failed.", e);
                        return;
                    }

                    for (DocumentChange dc : snapshots.getDocumentChanges()) {
                        if (dc.getType() == DocumentChange.Type.ADDED) {
                            Message message = dc.getDocument().toObject(Message.class);
                            messageList.add(message);
                            adapter.notifyItemInserted(messageList.size() - 1);
                            binding.recyclerView.scrollToPosition(messageList.size() - 1);
                        }
                    }
                });
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (messageListener != null) {
            messageListener.remove();
        }
    }
}
