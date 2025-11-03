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
import com.chotujobs.services.FirestoreService;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentChange;

import java.util.ArrayList;
import java.util.List;

public class ChatsFragment extends Fragment {

    private FragmentChatsBinding binding;
    private ChatsAdapter adapter;
    private List<Chat> chatList;
    private FirestoreService firestoreService;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentChatsBinding.inflate(inflater, container, false);

        firestoreService = FirestoreService.getInstance();
        chatList = new ArrayList<>();
        adapter = new ChatsAdapter(chatList);
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

                    for (DocumentChange dc : snapshots.getDocumentChanges()) {
                        if (dc.getType() == DocumentChange.Type.ADDED) {
                            Chat chat = dc.getDocument().toObject(Chat.class);
                            chat.setChatId(dc.getDocument().getId());
                            chatList.add(chat);
                        }
                    }
                    adapter.notifyDataSetChanged();
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
