package com.chotujobs.models;

import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.IgnoreExtraProperties;
import java.util.Date;

@IgnoreExtraProperties
public class Job {
    private String jobId;
    private String contractorId;
    private String title;
    private String category;
    private String startDate;
    private String location; // Changed from GeoPoint to String
    private String imageUrl; // nullable - local path
    private String status; // "active" or "closed"
    private String winnerUserId; // ID of the winning labourer/agent
    private Long timestamp; // stored as milliseconds since epoch
    private String requirements;
    private int bidLimit;

    // Required public no-arg constructor
    public Job() {}

    public String getJobId() {
        return jobId;
    }

    public void setJobId(String jobId) {
        this.jobId = jobId;
    }

    public String getContractorId() {
        return contractorId;
    }

    public void setContractorId(String contractorId) {
        this.contractorId = contractorId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getStartDate() {
        return startDate;
    }

    public void setStartDate(String startDate) {
        this.startDate = startDate;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getWinnerUserId() {
        return winnerUserId;
    }

    public void setWinnerUserId(String winnerUserId) {
        this.winnerUserId = winnerUserId;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    // Main setter for Firestore
    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }


    @Exclude
    public Date getTimestampAsDate() {
        return (timestamp != null) ? new Date(timestamp) : null;
    }

    public String getRequirements() {
        return requirements;
    }

    public void setRequirements(String requirements) {
        this.requirements = requirements;
    }

    public int getBidLimit() {
        return bidLimit;
    }

    public void setBidLimit(int bidLimit) {
        this.bidLimit = bidLimit;
    }
}
