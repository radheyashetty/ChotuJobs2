package com.chotujobs.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

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
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.FieldPath;
import com.google.firebase.firestore.FirebaseFirestore;

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

        listenForChats();

        return binding.getRoot();
    }

    private void listenForChats() {
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        firestoreService.getChatsForUser(currentUserId)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) {
                        return;
                    }

                    List<String> userIds = new ArrayList<>();
                    for (DocumentChange dc : snapshots.getDocumentChanges()) {
                        if (dc.getType() == DocumentChange.Type.ADDED) {
                            Chat chat = dc.getDocument().toObject(Chat.class);
                            chat.setChatId(dc.getDocument().getId());
                            chatList.add(chat);
                            for(String userId : chat.getUserIds()){
                                if(!userId.equals(currentUserId)){
                                    userIds.add(userId);
                                }
                            }
                        }
                    }

                    if(!userIds.isEmpty()){
                        FirebaseFirestore.getInstance().collection("users").whereIn(FieldPath.documentId(), userIds)
                                .get().addOnSuccessListener(queryDocumentSnapshots -> {
                                    for(DocumentChange dc : queryDocumentSnapshots.getDocumentChanges()){
                                        if(dc.getType() == DocumentChange.Type.ADDED){
                                            User user = dc.getDocument().toObject(User.class);
                                            userMap.put(dc.getDocument().getId(), user);
                                        }
                                    }
                                    adapter.notifyDataSetChanged();
                                });
                    }
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
