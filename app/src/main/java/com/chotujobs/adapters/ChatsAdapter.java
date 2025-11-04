package com.chotujobs.adapters;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.chotujobs.ChatActivity;
import com.chotujobs.databinding.ItemChatBinding;
import com.chotujobs.models.Chat;
import com.chotujobs.models.User;
import com.google.firebase.auth.FirebaseAuth;

import java.util.List;
import java.util.Map;

public class ChatsAdapter extends RecyclerView.Adapter<ChatsAdapter.ChatViewHolder> {

    private List<Chat> chatList;
    private Map<String, User> userMap;

    public ChatsAdapter(List<Chat> chatList, Map<String, User> userMap) {
        this.chatList = chatList;
        this.userMap = userMap;
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
            String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
            String otherUserId = "";
            for (String userId : chat.getUserIds()) {
                if (!userId.equals(currentUserId)) {
                    otherUserId = userId;
                    break;
                }
            }

            if (!otherUserId.isEmpty()) {
                User user = userMap.get(otherUserId);
                if (user != null) {
                    binding.userNameTextView.setText(user.getName());
                    if (user.getProfileImageUrl() != null && !user.getProfileImageUrl().isEmpty()) {
                        Glide.with(itemView.getContext()).load(user.getProfileImageUrl()).into(binding.profileImageView);
                    }
                }
            }

            binding.lastMessageTextView.setText(chat.getLastMessage());

            final String finalOtherUserId = otherUserId;
            itemView.setOnClickListener(v -> {
                Intent intent = new Intent(itemView.getContext(), ChatActivity.class);
                intent.putExtra("chatId", chat.getChatId());
                intent.putExtra("receiverId", finalOtherUserId);
                itemView.getContext().startActivity(intent);
            });
        }
    }
}
