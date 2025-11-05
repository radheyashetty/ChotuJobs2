package com.chotujobs.models;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.ServerTimestamp;
import java.util.Date;

public class Bid implements Parcelable {
    private String bidId;
    private String jobId;
    private String bidderId; // The user who placed the bid (labourer or agent)
    private double bidAmount;
    private String labourerIdIfAgent; // nullable - only set if bidder is an agent
    private String status; // "pending", "accepted", "rejected"
    private Long timestamp;

    public Bid() {}

    protected Bid(Parcel in) {
        bidId = in.readString();
        jobId = in.readString();
        bidderId = in.readString();
        bidAmount = in.readDouble();
        labourerIdIfAgent = in.readString();
        status = in.readString();
        if (in.readByte() == 0) {
            timestamp = null;
        } else {
            timestamp = in.readLong();
        }
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(bidId);
        dest.writeString(jobId);
        dest.writeString(bidderId);
        dest.writeDouble(bidAmount);
        dest.writeString(labourerIdIfAgent);
        dest.writeString(status);
        if (timestamp == null) {
            dest.writeByte((byte) 0);
        } else {
            dest.writeByte((byte) 1);
            dest.writeLong(timestamp);
        }
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

    @ServerTimestamp
    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }

    @Exclude
    public Date getTimestampAsDate() {
        if (timestamp == null) {
            return null;
        }
        return new Date(timestamp);
    }
}
