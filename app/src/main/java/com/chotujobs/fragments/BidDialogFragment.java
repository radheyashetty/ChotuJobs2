package com.chotujobs.fragments;

import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.chotujobs.databinding.DialogBidBinding;
import com.chotujobs.models.Bid;
import com.chotujobs.models.User;
import com.chotujobs.services.FirestoreService;

import java.util.List;

public class BidDialogFragment extends DialogFragment {

    private DialogBidBinding binding;
    private String jobId;
    private String bidderId;
    private String userType;
    private Runnable bidListener;
    private FirestoreService firestoreService;

    public static BidDialogFragment newInstance(String jobId, String bidderId, String userType) {
        BidDialogFragment fragment = new BidDialogFragment();
        Bundle args = new Bundle();
        args.putString("job_id", jobId);
        args.putString("bidder_id", bidderId);
        args.putString("user_type", userType);
        fragment.setArguments(args);
        return fragment;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        firestoreService = FirestoreService.getInstance();

        if (getArguments() != null) {
            jobId = getArguments().getString("job_id");
            bidderId = getArguments().getString("bidder_id");
            userType = getArguments().getString("user_type");
        }

        binding = DialogBidBinding.inflate(requireActivity().getLayoutInflater());

        if ("agent".equals(userType)) {
            binding.labourerSpinner.setVisibility(View.VISIBLE);
            binding.labourerLabel.setVisibility(View.VISIBLE);
            setupLabourerSpinner();
        } else {
            binding.labourerSpinner.setVisibility(View.GONE);
            binding.labourerLabel.setVisibility(View.GONE);
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setView(binding.getRoot())
                .setTitle("Place Bid")
                .setPositiveButton("Submit", null)
                .setNegativeButton("Cancel", null);

        AlertDialog dialog = builder.create();
        dialog.setOnShowListener(d -> {
            android.widget.Button positive = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            if (positive != null) {
                positive.setOnClickListener(v -> handleBidSubmit());
            }
        });

        return dialog;
    }

    private void setupLabourerSpinner() {
        firestoreService.getUsersByRole("labour", users -> {
            if (getContext() != null && users != null && !users.isEmpty()) {
                ArrayAdapter<User> adapter = new ArrayAdapter<>(
                        getContext(), android.R.layout.simple_spinner_item, users);
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                binding.labourerSpinner.setAdapter(adapter);
            }
        });
    }

    private void handleBidSubmit() {
        if (getContext() == null) {
            return;
        }
        
        // Validate required fields
        if (jobId == null || jobId.isEmpty()) {
            Toast.makeText(getContext(), "Job ID is missing", Toast.LENGTH_SHORT).show();
            return;
        }
        if (bidderId == null || bidderId.isEmpty()) {
            Toast.makeText(getContext(), "Bidder ID is missing", Toast.LENGTH_SHORT).show();
            return;
        }
        
        try {
            String amountStr = binding.bidAmountEditText.getText().toString().trim();
            if (amountStr.isEmpty()) {
                Toast.makeText(getContext(), "Please enter bid amount", Toast.LENGTH_SHORT).show();
                return;
            }

            double bidAmount = Double.parseDouble(amountStr);
            if (bidAmount <= 0) {
                Toast.makeText(getContext(), "Bid amount must be greater than zero", Toast.LENGTH_SHORT).show();
                return;
            }

            Bid bid = new Bid();
            bid.setJobId(jobId);
            bid.setBidderId(bidderId);
            bid.setBidAmount(bidAmount);

            if ("agent".equals(userType)) {
                User selectedLabourer = (User) binding.labourerSpinner.getSelectedItem();
                if (selectedLabourer != null && selectedLabourer.getUserId() != null) {
                    bid.setLabourerIdIfAgent(selectedLabourer.getUserId());
                } else {
                    Toast.makeText(getContext(), "Please select a labourer", Toast.LENGTH_SHORT).show();
                    return;
                }
            }

            firestoreService.createBid(bid, bidId -> {
                if (getDialog() != null && getContext() != null) {
                    if (bidId != null) {
                        Toast.makeText(getContext(), "Bid placed successfully!", Toast.LENGTH_SHORT).show();
                        if (bidListener != null) {
                            bidListener.run();
                        }
                        dismiss();
                    } else {
                        String errorMsg = "Failed to place bid. ";
                        if (userType == null || (!userType.equals("labour") && !userType.equals("agent"))) {
                            errorMsg += "Your role must be 'labour' or 'agent'. ";
                        }
                        errorMsg += "Check if job is active and you have permission.";
                        Toast.makeText(getContext(), errorMsg, Toast.LENGTH_LONG).show();
                    }
                }
            });
        } catch (NumberFormatException e) {
            Toast.makeText(getContext(), "Invalid bid amount. Please enter a valid number.", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(getContext(), "Error placing bid: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    public void setBidListener(Runnable listener) {
        this.bidListener = listener;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
