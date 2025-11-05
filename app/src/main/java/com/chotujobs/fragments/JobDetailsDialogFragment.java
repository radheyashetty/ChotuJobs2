package com.chotujobs.fragments;

import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.chotujobs.adapters.BidAdapter;
import com.chotujobs.databinding.DialogJobDetailsBinding;
import com.chotujobs.models.Bid;
import com.chotujobs.models.Job;
import com.chotujobs.models.User;
import com.chotujobs.services.FirestoreService;
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
        
        if (getContext() == null) {
            return super.onCreateDialog(savedInstanceState);
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

        return builder.create();
    }

    private void loadJobAndBids() {
        binding.progressBar.setVisibility(View.VISIBLE);
        firestoreService.getJobById(jobId, job -> {
            if (!isAdded()) return;
            if (job != null) {
                currentJob = job;
                binding.jobTitleTextView.setText(job.getTitle() != null ? job.getTitle() : "");
                String details = buildJobDetails(job);
                binding.jobDetailsTextView.setText(details);

                AlertDialog dialog = (AlertDialog) getDialog();
                if (dialog != null) {
                    if ("active".equals(job.getStatus())) {
                        dialog.setTitle("Select Winning Bid");
                    } else {
                        dialog.setTitle("Job Details (Closed)");
                    }
                }

                firestoreService.getBidsByJob(jobId, bids -> {
                    if (!isAdded()) return;
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
        if (allBids == null || allBids.isEmpty()) {
            displayBids();
            return;
        }

        List<String> userIds = new ArrayList<>();
        for (Bid bid : allBids) {
            if (bid != null && bid.getBidderId() != null && !bid.getBidderId().isEmpty()) {
            userIds.add(bid.getBidderId());
            }
        }

        firestoreService.getUsersByIds(userIds, users -> {
            if (!isAdded()) return;
            userMap.clear();
            for (User user : users) {
                if (user != null && user.getUserId() != null) {
                userMap.put(user.getUserId(), user);
                }
            }
            displayBids();
        });
    }

    private void displayBids() {
        if (!isAdded()) return;
        binding.progressBar.setVisibility(View.GONE);
        if (allBids == null || allBids.isEmpty()) {
            binding.bidsRecyclerView.setAdapter(null);
            return;
        }

        binding.bidsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        BidAdapter adapter = new BidAdapter(userMap, new BidAdapter.OnBidActionClickListener() {
            @Override
            public void onAcceptBidClick(Bid bid) {
                showBidderDetailsDialog(bid);
            }

            @Override
            public void onRejectBidClick(Bid bid) {
                showConfirmRejectDialog(bid);
            }
        });
        binding.bidsRecyclerView.setAdapter(adapter);
        adapter.submitList(allBids);
    }

    private void showBidderDetailsDialog(Bid bid) {
        if (!isAdded() || bid == null) return;
        if (bid.getBidderId() == null) return;
        
        User bidder = userMap.get(bid.getBidderId());
        if (bidder != null) {
            BidderDetailsDialogFragment dialog = BidderDetailsDialogFragment.newInstance(bid, bidder);
            dialog.setOnWinnerSelectedListener(this::showConfirmWinnerDialog);
            dialog.show(getParentFragmentManager(), "bidder_details_dialog");
        }
    }

    private void showConfirmRejectDialog(Bid bid) {
        if (!isAdded() || bid == null || bid.getBidId() == null) return;
        
        new AlertDialog.Builder(requireContext())
                .setTitle("Confirm Rejection")
                .setMessage("Are you sure you want to reject this bid?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    firestoreService.updateBidStatus(jobId, bid.getBidId(), "rejected", success -> {
                        if (!isAdded()) return;
                        if (success) {
                            Toast.makeText(getContext(), "Bid rejected", Toast.LENGTH_SHORT).show();
                            loadJobAndBids();
                        } else {
                            Toast.makeText(getContext(), "Error rejecting bid", Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showConfirmWinnerDialog(Bid bid) {
        if (!isAdded() || bid == null || bid.getBidId() == null || currentJob == null) return;
        
        if (!"active".equals(currentJob.getStatus())) {
            Toast.makeText(getContext(), "Job is already closed", Toast.LENGTH_SHORT).show();
            return;
        }
        
        new AlertDialog.Builder(requireContext())
                .setTitle("Confirm Winner")
                .setMessage("Mark this bid as winner? This will close the job, reject other bids, and notify the labourer.")
                .setPositiveButton("Yes", (dialog, which) -> {
                    selectWinner(bid);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
    
    private void selectWinner(Bid bid) {
        if (!isAdded() || bid == null || bid.getBidId() == null || currentJob == null) return;
        
        if (jobId == null || jobId.isEmpty()) {
            Toast.makeText(getContext(), "Error: Job ID is missing", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (bid.getBidId() == null || bid.getBidId().isEmpty()) {
            Toast.makeText(getContext(), "Error: Bid ID is missing", Toast.LENGTH_SHORT).show();
            return;
        }
        
        String currentUserId = firestoreService.getCurrentUserId();
        if (currentUserId == null || currentUserId.isEmpty()) {
            Toast.makeText(getContext(), "Please log in again", Toast.LENGTH_SHORT).show();
            return;
        }
        
        String tempLabourerId = bid.getLabourerIdIfAgent();
        if (tempLabourerId == null || tempLabourerId.isEmpty()) {
            tempLabourerId = bid.getBidderId();
        }
        
        final String labourerId = tempLabourerId;
        final String acceptedBidId = bid.getBidId();
        final Bid winningBid = bid;
        
        if (labourerId == null || labourerId.isEmpty()) {
            Toast.makeText(getContext(), "Cannot determine winner", Toast.LENGTH_SHORT).show();
            return;
        }
        
        binding.progressBar.setVisibility(View.VISIBLE);
        
        firestoreService.getJobById(jobId, freshJob -> {
            if (!isAdded()) return;
            binding.progressBar.setVisibility(View.GONE);
            
            if (freshJob == null) {
                Toast.makeText(getContext(), "Error: Job not found", Toast.LENGTH_SHORT).show();
                return;
            }
            
            String jobContractorId = freshJob.getContractorId();
            if (jobContractorId == null || jobContractorId.isEmpty()) {
                Toast.makeText(getContext(), "Job has no owner information", Toast.LENGTH_SHORT).show();
                return;
            }
            
            if (!jobContractorId.trim().equals(currentUserId.trim())) {
                Toast.makeText(getContext(), "You can only accept bids for jobs you created", Toast.LENGTH_LONG).show();
                return;
            }
            
            currentJob = freshJob;
            
            firestoreService.updateBidStatus(jobId, acceptedBidId, "accepted", success -> {
                if (!isAdded()) return;
                if (!success) {
                    Toast.makeText(getContext(), "Error marking winner. Please check if you own this job and the bid exists.", Toast.LENGTH_LONG).show();
                    return;
                }
                
                rejectOtherBids(acceptedBidId, labourerId, winningBid);
            });
        });
    }
    
    private void rejectOtherBids(String acceptedBidId, String winnerId, Bid winningBid) {
        if (allBids == null || allBids.isEmpty()) {
            closeJobAndNotify(winnerId, winningBid);
            return;
        }
        
        int pendingCount = 0;
        for (Bid bid : allBids) {
            if (bid != null && bid.getBidId() != null &&
                !bid.getBidId().equals(acceptedBidId) &&
                "pending".equals(bid.getStatus())) {
                pendingCount++;
            }
        }
        
        final int finalPendingCount = pendingCount;
        if (finalPendingCount == 0) {
            closeJobAndNotify(winnerId, winningBid);
            return;
        }
        
        int[] rejectedCount = {0};
        for (Bid bid : allBids) {
            if (bid != null && bid.getBidId() != null &&
                !bid.getBidId().equals(acceptedBidId) &&
                "pending".equals(bid.getStatus())) {
                
                String bidIdToReject = bid.getBidId();
                firestoreService.updateBidStatus(jobId, bidIdToReject, "rejected", success -> {
                    rejectedCount[0]++;
                    if (rejectedCount[0] == finalPendingCount) {
                        closeJobAndNotify(winnerId, winningBid);
                    }
                });
            }
        }
    }
    
    private void closeJobAndNotify(String winnerId, Bid winningBid) {
        if (winningBid == null) {
            Toast.makeText(getContext(), "Error: Winning bid is missing", Toast.LENGTH_SHORT).show();
            return;
        }
        
        firestoreService.updateJobStatus(jobId, "closed", winnerId, updateSuccess -> {
                        if (!isAdded()) return;
            if (!updateSuccess) {
                Toast.makeText(getContext(), "Error closing job", Toast.LENGTH_SHORT).show();
                return;
            }
            
            String contractorId = currentJob.getContractorId();
            String jobTitle = currentJob.getTitle() != null ? currentJob.getTitle() : "";
            
            if (contractorId != null && !contractorId.isEmpty()) {
                firestoreService.notifyBidAccepted(contractorId, winningBid, jobTitle, notified -> {
                                if (!isAdded()) return;
                    
                    String message = "✅ Winner selected!";
                    if (notified) {
                        message += "\n📧 Labourer notified successfully!";
                    } else {
                        message += "\n⚠️ Failed to notify labourer";
                    }
                    
                    new AlertDialog.Builder(requireContext())
                            .setTitle("✅ Job Completed")
                            .setMessage(message)
                            .setPositiveButton("OK", (dialog, which) -> dismiss())
                            .show();
                                    
                                    if (jobClosedListener != null) {
                                        jobClosedListener.run();
                                }
                            });
                        } else {
                Toast.makeText(getContext(), "Error: Missing contractor information", Toast.LENGTH_SHORT).show();
                        }
                    });
    }

    private String buildJobDetails(Job job) {
        if (job == null) return "";
        return "Category: " + (job.getCategory() != null ? job.getCategory() : "") + "\n" +
                "Start Date: " + (job.getStartDate() != null ? job.getStartDate() : "") + "\n" +
                "Location: " + (job.getLocation() != null ? job.getLocation() : "") + "\n" +
                "Requirements: " + (job.getRequirements() != null ? job.getRequirements() : "") + "\n" +
                "Expected Amount: " + job.getBidLimit();
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
