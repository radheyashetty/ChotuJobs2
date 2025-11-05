package com.chotujobs.models;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.IgnoreExtraProperties;
import com.google.firebase.firestore.ServerTimestamp;
import java.util.Date;

@IgnoreExtraProperties
public class Bid implements Parcelable {
    @Exclude
    private String bidId;
    private String jobId;
    private String bidderId; // The user who placed the bid (labourer or agent)
    private double bidAmount;
    private String labourerIdIfAgent; // nullable - only set if bidder is an agent
    private String status; // "pending", "accepted", "rejected"
    private @ServerTimestamp Date timestamp;

    public Bid() {}

    protected Bid(Parcel in) {
        bidId = in.readString();
        jobId = in.readString();
        bidderId = in.readString();
        bidAmount = in.readDouble();
        labourerIdIfAgent = in.readString();
        status = in.readString();
        long tmpTimestamp = in.readLong();
        timestamp = tmpTimestamp == -1 ? null : new Date(tmpTimestamp);
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(bidId);
        dest.writeString(jobId);
        dest.writeString(bidderId);
        dest.writeDouble(bidAmount);
        dest.writeString(labourerIdIfAgent);
        dest.writeString(status);
        dest.writeLong(timestamp != null ? timestamp.getTime() : -1);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<Bid> CREATOR = new Creator<Bid>() {
        @Override
        public Bid createFromParcel(Parcel in) {
            return new Bid(in);
        }

        @Override
        public Bid[] newArray(int size) {
            return new Bid[size];
        }
    };

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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }
}
