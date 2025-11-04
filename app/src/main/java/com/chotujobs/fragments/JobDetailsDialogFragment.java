package com.chotujobs.fragments;

import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.chotujobs.adapters.BidAdapter;
import com.chotujobs.databinding.DialogJobDetailsBinding;
import com.chotujobs.models.Bid;
import com.chotujobs.models.Job;
import com.chotujobs.models.User;
import com.chotujobs.services.FirestoreService;
import com.chotujobs.util.CSVExporter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JobDetailsDialogFragment extends DialogFragment {

    private DialogJobDetailsBinding binding;
    private String jobId;
    private FirestoreService firestoreService;
    private Runnable jobClosedListener;
    private Job currentJob;
    private List<Bid> allBids;
    private Map<String, User> userMap = new HashMap<>();

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

        binding = DialogJobDetailsBinding.inflate(requireActivity().getLayoutInflater());

        loadJobAndBids();

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setView(binding.getRoot())
                .setNegativeButton("Close", null);

        AlertDialog dialog = builder.create();

        binding.bidsListView.setOnItemClickListener((parent, view1, position, id) -> {
            if (currentJob != null && "active".equals(currentJob.getStatus())) {
                Bid bid = allBids.get(position);
                User bidder = userMap.get(bid.getBidderId());
                if(bidder != null){
                    showBidderDetailsDialog(bid, bidder);
                }
            }
        });

        return dialog;
    }

    private void loadJobAndBids() {
        binding.progressBar.setVisibility(View.VISIBLE);
        firestoreService.getJobById(jobId, job -> {
            if (job != null) {
                currentJob = job;
                binding.jobTitleTextView.setText(job.getTitle());
                String jobDetails = "Category: " + job.getCategory() + "\n" +
                        "Start Date: " + job.getStartDate() + "\n" +
                        "Location: " + job.getLocation() + "\n" +
                        "Requirements: " + job.getRequirements() + "\n" +
                        "Bid Limit: " + job.getBidLimit();
                binding.jobDetailsTextView.setText(jobDetails);

                AlertDialog dialog = (AlertDialog) getDialog();
                if (dialog != null) {
                    if ("active".equals(job.getStatus())) {
                        dialog.setTitle("Select Winning Bid");
                    } else {
                        dialog.setTitle("Job Details (Closed)");
                    }
                }

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
        List<String> userIds = new ArrayList<>();
        for (Bid bid : allBids) {
            userIds.add(bid.getBidderId());
        }

        firestoreService.getUsersByIds(userIds, users -> {
            for (User user : users) {
                userMap.put(user.getUserId(), user);
            }
            displayBids();
        });
    }

    private void displayBids() {
        binding.progressBar.setVisibility(View.GONE);
        if (allBids == null || allBids.isEmpty()) {
            binding.bidsListView.setAdapter(null);
            return;
        }

        BidAdapter adapter = new BidAdapter(getContext(), allBids, userMap);
        binding.bidsListView.setAdapter(adapter);
    }

    private void showBidderDetailsDialog(Bid bid, User bidder) {
        BidderDetailsDialogFragment dialog = BidderDetailsDialogFragment.newInstance(bid, bidder);
        dialog.setOnWinnerSelectedListener(this::showConfirmWinnerDialog);
        dialog.show(getParentFragmentManager(), "bidder_details_dialog");
    }

    private void showConfirmWinnerDialog(Bid bid) {
        new AlertDialog.Builder(getContext())
                .setTitle("Confirm Winner")
                .setMessage("Mark this bid as winner? This will close the job.")
                .setPositiveButton("Yes", (dialog, which) -> {
                    firestoreService.updateBidWinner(jobId, bid.getBidId(), success -> {
                        if (success) {
                            firestoreService.updateJobStatus(jobId, "closed", bid.getBidderId(), updateSuccess -> {
                                if (updateSuccess) {
                                    CSVExporter.exportJobToCSV(currentJob, bid, allBids, userMap, getContext());
                                    
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

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
