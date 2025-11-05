package com.chotujobs.models;

import com.google.firebase.firestore.ServerTimestamp;
import java.util.Date;
import java.util.List;

public class Chat {
    private String chatId;
    private List<String> userIds;
    private String lastMessage;
    private @ServerTimestamp Date lastMessageTimestamp;

    public Chat() {}

    public String getChatId() {
        return chatId;
    }

    public void setChatId(String chatId) {
        this.chatId = chatId;
    }

    public List<String> getUserIds() {
        return userIds;
    }

    public void setUserIds(List<String> userIds) {
        this.userIds = userIds;
    }

    public String getLastMessage() {
        return lastMessage;
    }

    public void setLastMessage(String lastMessage) {
        this.lastMessage = lastMessage;
    }

    public Date getLastMessageTimestamp() {
        return lastMessageTimestamp;
    }

    public void setLastMessageTimestamp(Date lastMessageTimestamp) {
        this.lastMessageTimestamp = lastMessageTimestamp;
    }
}
