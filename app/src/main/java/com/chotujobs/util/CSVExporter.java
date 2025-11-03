package com.chotujobs.util;

import android.content.Context;
import android.widget.Toast;

import com.chotujobs.models.Bid;
import com.chotujobs.models.Job;
import com.chotujobs.models.User;
import com.chotujobs.services.FirestoreService;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class CSVExporter {
    
    private static FirestoreService firestoreService = FirestoreService.getInstance();
    
    public static void exportJobToCSV(Job job, Bid winningBid, List<Bid> allBids, Context context) {
        StringBuilder csv = new StringBuilder();
        
        // Job details
        csv.append("ChotuJobs - Job Completion Report\n");
        csv.append("=================================\n\n");
        csv.append("Job ID: ").append(job.getJobId()).append("\n");
        csv.append("Title: ").append(job.getTitle()).append("\n");
        csv.append("Category: ").append(job.getCategory()).append("\n");
        csv.append("Start Date: ").append(job.getStartDate()).append("\n");
        csv.append("Location: ").append(job.getLatitude()).append(", ").append(job.getLongitude()).append("\n\n");
        
        // Winner details - need to fetch user from Firestore
        csv.append("WINNING BID:\n");
        csv.append("-------------\n");
        // Fetch winner user
        firestoreService.getUser(winningBid.getBidderId(), winner -> {
            if (winner != null) {
                csv.append("Bidder: ").append(winner.getName()).append("\n");
                csv.append("Email: ").append(winner.getEmail()).append("\n");
            }
            
            // If agent bid, fetch the laborer
            if (winningBid.getLabourerIdIfAgent() != null && !winningBid.getLabourerIdIfAgent().isEmpty()) {
                firestoreService.getUser(winningBid.getLabourerIdIfAgent(), labourer -> {
                    if (labourer != null) {
                        csv.append("Labourer: ").append(labourer.getName()).append("\n");
                    }
                    
                    csv.append("Amount: ₹").append(winningBid.getBidAmount()).append("\n\n");
                    csv.append("ALL BIDS SUMMARY:\n");
                    csv.append("-----------------\n");
                    
                    // Fetch all bidder names
                    fetchBiddersAndCompleteCSV(allBids, csv, 0, job, winningBid, context);
                });
            } else {
                csv.append("Amount: ₹").append(winningBid.getBidAmount()).append("\n\n");
                csv.append("ALL BIDS SUMMARY:\n");
                csv.append("-----------------\n");
                
                // Fetch all bidder names
                fetchBiddersAndCompleteCSV(allBids, csv, 0, job, winningBid, context);
            }
        });
    }
    
    private static void fetchBiddersAndCompleteCSV(List<Bid> bids, StringBuilder csv, int index, 
                                                   Job job, Bid winningBid, Context context) {
        if (index >= bids.size()) {
            // All bidders fetched, complete CSV
            csv.append("\n");
            
            // Timestamp
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault());
            csv.append("Report Generated: ").append(sdf.format(new Date())).append("\n");
            
            // Save to file
            String filename = "job_" + job.getJobId() + "_" + new Date().getTime() + ".csv";
            saveToFile(context, filename, csv.toString());
            return;
        }
        
        Bid bid = bids.get(index);
        firestoreService.getUser(bid.getBidderId(), bidder -> {
            csv.append("- ");
            if (bidder != null) {
                csv.append(bidder.getName());
            }
            csv.append(": ₹").append(bid.getBidAmount());
            if (bid.getBidId().equals(winningBid.getBidId())) {
                csv.append(" [WINNER]");
            }
            csv.append("\n");
            
            // Recursively fetch next bidder
            fetchBiddersAndCompleteCSV(bids, csv, index + 1, job, winningBid, context);
        });
    }
    
    public static void saveToFile(Context context, String filename, String content) {
        try {
            // For Android 10+, use app-specific directory
            File file = new File(context.getExternalFilesDir(null), filename);
            
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(content.getBytes());
            fos.close();
            
            Toast.makeText(context, "CSV saved to: " + file.getAbsolutePath(), Toast.LENGTH_LONG).show();
        } catch (IOException e) {
            Toast.makeText(context, "Error saving CSV: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}