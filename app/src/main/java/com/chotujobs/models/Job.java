package com.chotujobs.models;

import com.google.firebase.firestore.ServerTimestamp;
import java.util.Date;

public class Job {
    private String jobId;
    private String contractorId;
    private String title;
    private String category;
    private String startDate;
    private String imagePath;
    private String location; // Changed from GeoPoint to String
    private String imageUrl; // nullable - local path
    private String status; // "active" or "closed"
    private Object timestamp;

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

    public Date getTimestamp() {
        if (timestamp instanceof Date) {
            return (Date) timestamp;
        } else if (timestamp instanceof Long) {
            return new Date((Long) timestamp);
        } else if (timestamp instanceof com.google.firebase.Timestamp) {
            return ((com.google.firebase.Timestamp) timestamp).toDate();
        }
        return null;
    }
    public String getImagePath() {
        return imagePath;
    }

    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }

    public void setTimestamp(Object timestamp) {
        this.timestamp = timestamp;
    }
}
