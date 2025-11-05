package com.chotujobs.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.chotujobs.adapters.ChatsAdapter;
import com.chotujobs.databinding.FragmentChatsBinding;
import com.chotujobs.models.Chat;
import com.chotujobs.models.User;
import com.chotujobs.services.FirestoreService;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChatsFragment extends Fragment {

    private FragmentChatsBinding binding;
    private ChatsAdapter adapter;
    private List<Chat> chatList;
    private Map<String, User> userMap;
    private FirestoreService firestoreService;
    private ListenerRegistration chatListener;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentChatsBinding.inflate(inflater, container, false);

        firestoreService = FirestoreService.getInstance();
        chatList = new ArrayList<>();
        userMap = new HashMap<>();
        adapter = new ChatsAdapter(chatList, userMap);
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.recyclerView.setAdapter(adapter);

        binding.swipeRefreshLayout.setOnRefreshListener(this::listenForChats);

        listenForChats();

        return binding.getRoot();
    }

    private void listenForChats() {
        if (chatListener != null) {
            chatListener.remove();
        }
        binding.swipeRefreshLayout.setRefreshing(true);
        chatList.clear();
        userMap.clear();
        adapter.notifyDataSetChanged();

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            if (isAdded() && getContext() != null) {
                Toast.makeText(getContext(), "You need to be logged in to view chats.", Toast.LENGTH_SHORT).show();
                if (binding != null) {
                    binding.swipeRefreshLayout.setRefreshing(false);
                }
            }
            return;
        }
        String currentUserId = currentUser.getUid();
        chatListener = firestoreService.getChatsForUser(currentUserId)
                .addSnapshotListener((snapshots, e) -> {
                    if (!isAdded() || binding == null) return;
                    binding.swipeRefreshLayout.setRefreshing(false);
                    if (e != null) {
                        android.util.Log.e("ChatsFragment", "Error listening for chats", e);
                        if (getContext() != null) {
                            Toast.makeText(getContext(), "Error loading chats: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                        return;
                    }

                    if (snapshots == null) {
                        return;
                    }

                    List<String> userIds = new ArrayList<>();
                    for (DocumentChange dc : snapshots.getDocumentChanges()) {
                        Chat chat = dc.getDocument().toObject(Chat.class);
                        if (chat == null || chat.getUserIds() == null) {
                            continue;
                        }
                        
                        String docId = dc.getDocument().getId();
                        chat.setChatId(docId);
                        
                        switch (dc.getType()) {
                            case ADDED:
                                boolean exists = false;
                                for (Chat c : chatList) {
                                    if (docId.equals(c.getChatId())) {
                                        exists = true;
                                        break;
                                    }
                                }
                                if (!exists) {
                                    chatList.add(chat);
                                    collectUserIds(chat, currentUserId, userIds);
                                }
                                break;
                            case MODIFIED:
                                for (int i = 0; i < chatList.size(); i++) {
                                    if (docId.equals(chatList.get(i).getChatId())) {
                                        chatList.set(i, chat);
                                        collectUserIds(chat, currentUserId, userIds);
                                        break;
                                    }
                                }
                                break;
                            case REMOVED:
                                for (int i = 0; i < chatList.size(); i++) {
                                    if (docId.equals(chatList.get(i).getChatId())) {
                                        chatList.remove(i);
                                        break;
                                    }
                                }
                                break;
                        }
                    }

                    if (!userIds.isEmpty()) {
                        firestoreService.getUsersByIds(userIds, users -> {
                            if (!isAdded()) return;
                            for (User user : users) {
                                if (user != null && user.getUserId() != null) {
                                    userMap.put(user.getUserId(), user);
                                }
                            }
                            adapter.notifyDataSetChanged();
                        });
                    } else {
                        adapter.notifyDataSetChanged();
                    }
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (chatListener != null) {
            chatListener.remove();
        }
        binding = null;
    }

    private void collectUserIds(Chat chat, String currentUserId, List<String> userIds) {
        if (chat.getUserIds() != null) {
            for (String userId : chat.getUserIds()) {
                if (userId != null && !userId.equals(currentUserId)) {
                    userIds.add(userId);
                }
            }
        }
    }
}
