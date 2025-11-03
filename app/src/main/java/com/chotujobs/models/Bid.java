package com.chotujobs.models;

import com.google.firebase.firestore.ServerTimestamp;
import java.util.Date;

public class Bid {
    private String bidId;
    private String jobId;
    private String bidderId; // The user who placed the bid (labourer or agent)
    private double bidAmount;
    private String labourerIdIfAgent; // nullable - only set if bidder is an agent
    private int winnerFlag; // 0 or 1
    private @ServerTimestamp Date timestamp;
    
    public Bid() {}
    
    public String getBidId() {
        return bidId;
    }
    
    public void setBidId(String bidId) {
        this.bidId = bidId;
    }
    
    public String getJobId() {
        return jobId;
    }
    
    public void setJobId(String jobId) {
        this.jobId = jobId;
    }
    
    public String getBidderId() {
        return bidderId;
    }
    
    public void setBidderId(String bidderId) {
        this.bidderId = bidderId;
    }
    
    public String getLabourerIdIfAgent() {
        return labourerIdIfAgent;
    }
    
    public void setLabourerIdIfAgent(String labourerIdIfAgent) {
        this.labourerIdIfAgent = labourerIdIfAgent;
    }
    
    public double getBidAmount() {
        return bidAmount;
    }
    
    public void setBidAmount(double bidAmount) {
        this.bidAmount = bidAmount;
    }
    
    public int getWinnerFlag() {
        return winnerFlag;
    }
    
    public void setWinnerFlag(int winnerFlag) {
        this.winnerFlag = winnerFlag;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }
}
