package com.chotujobs.util;

import android.content.Context;
import android.widget.Toast;

import com.chotujobs.models.Bid;
import com.chotujobs.models.Job;
import com.chotujobs.models.User;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class CSVExporter {

    public static void exportJobToCSV(Job job, Bid winningBid, List<Bid> allBids, Map<String, User> userMap, Context context) {
        if (job == null || winningBid == null) {
            if (context != null) {
                Toast.makeText(context, "Error: Job or winning bid data is missing.", Toast.LENGTH_SHORT).show();
            }
            return;
        }

        StringBuilder csv = new StringBuilder();
        csv.append("Job ID,Title,Category,Start Date,Location,Winner,Winner Contact,Winner Bid Amount,Labourer\n");

        // Job details - escape commas in fields
        csv.append(escapeCSV(job.getJobId())).append(",");
        csv.append(escapeCSV(job.getTitle())).append(",");
        csv.append(escapeCSV(job.getCategory())).append(",");
        csv.append(escapeCSV(job.getStartDate())).append(",");
        csv.append(escapeCSV(job.getLocation())).append(",");

        // Winner details
        String bidderId = winningBid != null ? winningBid.getBidderId() : null;
        User winner = (bidderId != null && userMap != null) ? userMap.get(bidderId) : null;
        if (winner != null) {
            csv.append(escapeCSV(winner.getName())).append(",");
            if (winner.getEmail() != null && !winner.getEmail().isEmpty()) {
                csv.append(escapeCSV(winner.getEmail())).append(",");
            } else if (winner.getPhone() != null && !winner.getPhone().isEmpty()) {
                csv.append(escapeCSV(winner.getPhone())).append(",");
            } else {
                csv.append("N/A,");
            }
        } else {
            csv.append("N/A,N/A,");
        }
        csv.append(winningBid != null ? winningBid.getBidAmount() : 0).append(",");

        if (winningBid != null && winningBid.getLabourerIdIfAgent() != null && userMap != null) {
            User labourer = userMap.get(winningBid.getLabourerIdIfAgent());
            if (labourer != null && labourer.getName() != null) {
                csv.append(escapeCSV(labourer.getName()));
            }
        }
        csv.append("\n\n");

        // All bids
        csv.append("All Bids\n");
        csv.append("Bidder,Bid Amount,Labourer\n");
        if (allBids != null && userMap != null) {
            for (Bid bid : allBids) {
                if (bid == null) continue;
                String bidBidderId = bid.getBidderId();
                User bidder = (bidBidderId != null) ? userMap.get(bidBidderId) : null;
                if (bidder != null && bidder.getName() != null) {
                    csv.append(escapeCSV(bidder.getName())).append(",");
                } else {
                    csv.append("N/A,");
                }
                csv.append(bid.getBidAmount()).append(",");
                if (bid.getLabourerIdIfAgent() != null) {
                    User labourer = userMap.get(bid.getLabourerIdIfAgent());
                    if (labourer != null && labourer.getName() != null) {
                        csv.append(escapeCSV(labourer.getName()));
                    }
                }
                csv.append("\n");
            }
        }

        // Timestamp
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault());
        String timestamp = sdf.format(new Date());
        String filename = "job_report_" + job.getJobId() + "_" + timestamp + ".csv";
        saveToFile(context, filename, csv.toString());
    }

    public static void saveToFile(Context context, String filename, String content) {
        if (context == null || filename == null || content == null) {
            return;
        }
        try {
            File dir = context.getExternalFilesDir(null);
            if (dir == null) {
                Toast.makeText(context, "Error: Cannot access external storage", Toast.LENGTH_SHORT).show();
                return;
            }
            File file = new File(dir, filename);
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(content.getBytes());
            fos.close();
            Toast.makeText(context, "CSV saved to: " + file.getAbsolutePath(), Toast.LENGTH_LONG).show();
        } catch (IOException e) {
            Toast.makeText(context, "Error saving CSV: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    
    private static String escapeCSV(String value) {
        if (value == null) {
            return "";
        }
        // Escape quotes and wrap in quotes if contains comma, quote, or newline
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
