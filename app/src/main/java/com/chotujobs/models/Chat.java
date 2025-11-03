package com.chotujobs.models;

import java.util.List;

public class Chat {
    private String chatId;
    private List<String> userIds;
    private String lastMessage;
    private long lastMessageTimestamp;

    public Chat() {}

    public Chat(String chatId, List<String> userIds, String lastMessage, long lastMessageTimestamp) {
        this.chatId = chatId;
        this.userIds = userIds;
        this.lastMessage = lastMessage;
        this.lastMessageTimestamp = lastMessageTimestamp;
    }

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

    public long getLastMessageTimestamp() {
        return lastMessageTimestamp;
    }

    public void setLastMessageTimestamp(long lastMessageTimestamp) {
        this.lastMessageTimestamp = lastMessageTimestamp;
    }
}
