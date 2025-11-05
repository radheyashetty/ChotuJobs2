package com.chotujobs.adapters;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.chotujobs.ChatActivity;
import com.chotujobs.databinding.ItemChatBinding;
import com.chotujobs.models.Chat;
import com.chotujobs.models.User;
import com.chotujobs.services.FirestoreService;

import java.util.List;
import java.util.Map;

public class ChatsAdapter extends RecyclerView.Adapter<ChatsAdapter.ChatViewHolder> {

    private List<Chat> chatList;
    private Map<String, User> userMap;
    private String currentUserId;

    public ChatsAdapter(List<Chat> chatList, Map<String, User> userMap) {
        this.chatList = chatList;
        this.userMap = userMap;
        this.currentUserId = FirestoreService.getInstance().getCurrentUserId();
    }

    @NonNull
    @Override
    public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemChatBinding binding = ItemChatBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new ChatViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ChatViewHolder holder, int position) {
        Chat chat = chatList.get(position);
        holder.bind(chat);
    }

    @Override
    public int getItemCount() {
        return chatList.size();
    }

    class ChatViewHolder extends RecyclerView.ViewHolder {
        private ItemChatBinding binding;

        public ChatViewHolder(ItemChatBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(Chat chat) {
            if (chat == null || chat.getUserIds() == null || currentUserId == null) {
                return;
            }
            
            String otherUserId = "";
            for (String userId : chat.getUserIds()) {
                if (userId != null && !userId.equals(currentUserId)) {
                    otherUserId = userId;
                    break;
                }
            }

            if (!otherUserId.isEmpty()) {
                User user = userMap.get(otherUserId);
                if (user != null && user.getName() != null) {
                    binding.userNameTextView.setText(user.getName());
                    if (user.getProfileImageUrl() != null && !user.getProfileImageUrl().isEmpty()) {
                        Glide.with(itemView.getContext()).load(user.getProfileImageUrl()).into(binding.profileImageView);
                    }
                } else {
                    binding.userNameTextView.setText("Unknown User");
                }
            } else {
                binding.userNameTextView.setText("Chat");
            }

            binding.lastMessageTextView.setText(chat.getLastMessage() != null ? chat.getLastMessage() : "");

            final String finalOtherUserId = otherUserId;
            final String chatId = chat.getChatId();
            if (chatId != null && !chatId.isEmpty()) {
                itemView.setOnClickListener(v -> {
                    Intent intent = new Intent(itemView.getContext(), ChatActivity.class);
                    intent.putExtra("chatId", chatId);
                    intent.putExtra("receiverId", finalOtherUserId);
                    itemView.getContext().startActivity(intent);
                });
            }
        }
    }
}
