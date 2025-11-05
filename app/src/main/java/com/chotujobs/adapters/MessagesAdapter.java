package com.chotujobs.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.chotujobs.R;
import com.chotujobs.models.Message;
import com.chotujobs.services.FirestoreService;

import java.util.List;

public class MessagesAdapter extends RecyclerView.Adapter<MessagesAdapter.MessageViewHolder> {

    private List<Message> messageList;
    private String currentUserId;
    private static final int VIEW_TYPE_SENT = 1;
    private static final int VIEW_TYPE_RECEIVED = 2;

    public MessagesAdapter(List<Message> messageList) {
        this.messageList = messageList;
        this.currentUserId = FirestoreService.getInstance().getCurrentUserId();
    }

    @Override
    public int getItemViewType(int position) {
        if (position < 0 || position >= messageList.size()) {
            return VIEW_TYPE_RECEIVED;
        }
        Message message = messageList.get(position);
        boolean isSentByMe = currentUserId != null && message != null 
                && message.getSenderId() != null 
                && message.getSenderId().equals(currentUserId);
        return isSentByMe ? VIEW_TYPE_SENT : VIEW_TYPE_RECEIVED;
    }

    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        int layoutId = viewType == VIEW_TYPE_SENT 
                ? R.layout.item_message_sent 
                : R.layout.item_message_received;
        View view = LayoutInflater.from(parent.getContext()).inflate(layoutId, parent, false);
        return new MessageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
        Message message = messageList.get(position);
        holder.bind(message);
    }

    @Override
    public int getItemCount() {
        return messageList.size();
    }

    class MessageViewHolder extends RecyclerView.ViewHolder {
        private TextView messageTextView;

        public MessageViewHolder(@NonNull View itemView) {
            super(itemView);
            messageTextView = itemView.findViewById(R.id.messageTextView);
        }

        public void bind(Message message) {
            if (message != null && message.getMessage() != null) {
                messageTextView.setText(message.getMessage());
            } else {
                messageTextView.setText("");
            }
        }
    }
}
