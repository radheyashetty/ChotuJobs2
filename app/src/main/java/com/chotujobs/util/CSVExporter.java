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
        StringBuilder csv = new StringBuilder();
        csv.append("Job ID,Title,Category,Start Date,Location,Winner,Winner Bid Amount,Labourer\n");

        // Job details
        csv.append(job.getJobId()).append(",");
        csv.append(job.getTitle()).append(",");
        csv.append(job.getCategory()).append(",");
        csv.append(job.getStartDate()).append(",");
        csv.append(job.getLocation()).append(",");

        // Winner details
        User winner = userMap.get(winningBid.getBidderId());
        if (winner != null) {
            csv.append(winner.getName()).append(",");
        } else {
            csv.append("N/A,");
        }
        csv.append(winningBid.getBidAmount()).append(",");

        if (winningBid.getLabourerIdIfAgent() != null) {
            User labourer = userMap.get(winningBid.getLabourerIdIfAgent());
            if (labourer != null) {
                csv.append(labourer.getName());
            }
        }
        csv.append("\n\n");

        // All bids
        csv.append("All Bids\n");
        csv.append("Bidder,Bid Amount,Labourer\n");
        for (Bid bid : allBids) {
            User bidder = userMap.get(bid.getBidderId());
            if (bidder != null) {
                csv.append(bidder.getName()).append(",");
            } else {
                csv.append("N/A,");
            }
            csv.append(bid.getBidAmount()).append(",");
            if (bid.getLabourerIdIfAgent() != null) {
                User labourer = userMap.get(bid.getLabourerIdIfAgent());
                if (labourer != null) {
                    csv.append(labourer.getName());
                }
            }
            csv.append("\n");
        }

        // Timestamp
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault());
        String timestamp = sdf.format(new Date());
        String filename = "job_report_" + job.getJobId() + "_" + timestamp + ".csv";
        saveToFile(context, filename, csv.toString());
    }

    public static void saveToFile(Context context, String filename, String content) {
        try {
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
