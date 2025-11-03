package com.chotujobs.fragments;

import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.chotujobs.R;
import com.chotujobs.models.Bid;
import com.chotujobs.models.Job;
import com.chotujobs.models.User;
import com.chotujobs.services.FirestoreService;
import com.chotujobs.util.CSVExporter;

import java.util.ArrayList;
import java.util.List;

public class JobDetailsDialogFragment extends DialogFragment {
    
    private String jobId;
    private FirestoreService firestoreService;
    private Runnable jobClosedListener;
    private TextView jobTitleTextView;
    private ListView bidsListView;
    private Job currentJob;
    private List<Bid> allBids;
    private List<User> allUsers;
    
    public static JobDetailsDialogFragment newInstance(String jobId) {
        JobDetailsDialogFragment fragment = new JobDetailsDialogFragment();
        Bundle args = new Bundle();
        args.putString("job_id", jobId);
        fragment.setArguments(args);
        return fragment;
    }
    
    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        firestoreService = FirestoreService.getInstance();
        
        if (getArguments() != null) {
            jobId = getArguments().getString("job_id");
        }
        
        if (jobId == null || jobId.isEmpty()) {
            dismiss();
            return new AlertDialog.Builder(getContext()).create();
        }
        
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_job_details, null);
        
        jobTitleTextView = view.findViewById(R.id.jobTitleTextView);
        bidsListView = view.findViewById(R.id.bidsListView);
        
        // Load job and bids
        loadJobAndBids(view);
        
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setView(view)
                .setNegativeButton("Close", null);
        
        AlertDialog dialog = builder.create();
        
        // Handle bid selection will be set up after loading data
        bidsListView.setOnItemClickListener((parent, view1, position, id) -> {
            if (currentJob != null && "active".equals(currentJob.getStatus())) {
                Bid bid = allBids.get(position);
                showConfirmWinnerDialog(bid);
            }
        });
        
        return dialog;
    }
    
    private void loadJobAndBids(View view) {
        // Load job first
        firestoreService.getJobById(jobId, job -> {
            if (job != null) {
                currentJob = job;
                jobTitleTextView.setText(job.getTitle());
                
                // Set title based on status
                AlertDialog dialog = (AlertDialog) getDialog();
                if (dialog != null) {
                    if ("active".equals(job.getStatus())) {
                        dialog.setTitle("Select Winning Bid");
                    } else {
                        dialog.setTitle("Job Details (Closed)");
                    }
                }
                
                // Now load bids
                firestoreService.getBidsByJob(jobId, bids -> {
                    if (bids != null) {
                        allBids = bids;
                        loadUsersAndDisplayBids();
                    }
                });
            } else {
                Toast.makeText(getContext(), "Error loading job", Toast.LENGTH_SHORT).show();
                dismiss();
            }
        });
    }
    
    private void loadUsersAndDisplayBids() {
        // Load all users to get names for bidders
        firestoreService.getAllUsers(users -> {
            allUsers = users;
            displayBids();
        });
    }
    
    private void displayBids() {
        if (allBids == null || allBids.isEmpty()) {
            List<String> emptyList = new ArrayList<>();
            emptyList.add("No bids yet");
            ArrayAdapter<String> adapter = new ArrayAdapter<>(
                    getContext(), android.R.layout.simple_list_item_1, emptyList);
            bidsListView.setAdapter(adapter);
            return;
        }
        
        List<String> bidStrings = new ArrayList<>();
        
        for (Bid bid : allBids) {
            User bidder = findUserById(bid.getBidderId());
            StringBuilder sb = new StringBuilder();
            
            if (bidder != null) {
                sb.append(bidder.getName());
                if (bid.getLabourerIdIfAgent() != null && !bid.getLabourerIdIfAgent().isEmpty()) {
                    User labourer = findUserById(bid.getLabourerIdIfAgent());
                    if (labourer != null) {
                        sb.append(" (for ").append(labourer.getName()).append(")");
                    }
                }
                sb.append("\nBid: ₹").append(bid.getBidAmount());
                if (bid.getWinnerFlag() == 1) {
                    sb.append(" - WINNER");
                }
            }
            bidStrings.add(sb.toString());
        }
        
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                getContext(), android.R.layout.simple_list_item_1, bidStrings);
        bidsListView.setAdapter(adapter);
    }
    
    private User findUserById(String userId) {
        if (allUsers == null || userId == null || userId.isEmpty()) {
            return null;
        }
        for (User user : allUsers) {
            if (userId.equals(user.getUserId())) {
                return user;
            }
        }
        return null;
    }
    
    private void showConfirmWinnerDialog(Bid bid) {
        new AlertDialog.Builder(getContext())
                .setTitle("Confirm Winner")
                .setMessage("Mark this bid as winner? This will close the job.")
                .setPositiveButton("Yes", (dialog, which) -> {
                    // Mark bid as winner
                    firestoreService.updateBidWinner(jobId, bid.getBidId(), success -> {
                        if (success) {
                            // Update job status
                            firestoreService.updateJobStatus(jobId, "closed", bid.getBidderId(), updateSuccess -> {
                                if (updateSuccess) {
                                    // Export to CSV
                                    CSVExporter.exportJobToCSV(currentJob, bid, allBids, getContext());
                                    
                                    Toast.makeText(getContext(), "Winner selected! CSV saved.", Toast.LENGTH_SHORT).show();
                                    
                                    if (jobClosedListener != null) {
                                        jobClosedListener.run();
                                    }
                                    dismiss();
                                } else {
                                    Toast.makeText(getContext(), "Error closing job", Toast.LENGTH_SHORT).show();
                                }
                            });
                        } else {
                            Toast.makeText(getContext(), "Error marking winner", Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
    
    public void setJobClosedListener(Runnable listener) {
        this.jobClosedListener = listener;
    }
}