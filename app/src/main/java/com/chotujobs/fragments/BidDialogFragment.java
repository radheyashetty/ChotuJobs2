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

        boolean isAgent = "agent".equalsIgnoreCase(userType);
        int visibility = isAgent ? View.VISIBLE : View.GONE;
        binding.labourerSpinner.setVisibility(visibility);
        binding.labourerLabel.setVisibility(visibility);
        
        if (isAgent) {
            setupLabourerSpinner();
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
        firestoreService.getLabourers(users -> {
            if (getContext() != null && users != null && !users.isEmpty()) {
                ArrayAdapter<User> adapter = new ArrayAdapter<>(
                        getContext(), android.R.layout.simple_spinner_item, users) {
                    @Override
                    public android.view.View getView(int position, android.view.View convertView, android.view.ViewGroup parent) {
                        android.view.View view = super.getView(position, convertView, parent);
                        android.widget.TextView textView = (android.widget.TextView) view;
                        textView.setTextColor(getResources().getColor(com.chotujobs.R.color.design_default_color_on_surface, null));
                        return view;
                    }
                    
                    @Override
                    public android.view.View getDropDownView(int position, android.view.View convertView, android.view.ViewGroup parent) {
                        android.view.View view = super.getDropDownView(position, convertView, parent);
                        android.widget.TextView textView = (android.widget.TextView) view;
                        textView.setTextColor(getResources().getColor(com.chotujobs.R.color.design_default_color_on_surface, null));
                        view.setBackgroundColor(getResources().getColor(com.chotujobs.R.color.design_default_color_surface, null));
                        return view;
                    }
                };
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                binding.labourerSpinner.setAdapter(adapter);
            } else if (getContext() != null) {
                Toast.makeText(getContext(), "No labourers available", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void handleBidSubmit() {
        if (getContext() == null) return;
        
        if (!areIdsValid()) return;
        
        Double bidAmount = getValidBidAmount();
        if (bidAmount == null) return;
        
        Bid bid = createBidObject(bidAmount);
        if (bid == null) return;
        
        submitBid(bid);
    }
    
    private boolean areIdsValid() {
        if (jobId == null || jobId.isEmpty()) {
            showToast("Job ID is missing");
            return false;
        }
        if (bidderId == null || bidderId.isEmpty()) {
            showToast("Bidder ID is missing");
            return false;
        }
        return true;
    }
    
    private Double getValidBidAmount() {
        String amountStr = binding.bidAmountEditText.getText().toString().trim();
        if (amountStr.isEmpty()) {
            showToast("Please enter bid amount");
            return null;
        }
        try {
            double amount = Double.parseDouble(amountStr);
            if (amount <= 0) {
                showToast("Bid amount must be greater than zero");
                return null;
            }
            return amount;
        } catch (NumberFormatException e) {
            showToast("Invalid bid amount. Please enter a valid number.");
            return null;
        }
    }
    
    private Bid createBidObject(double bidAmount) {
        Bid bid = new Bid();
        bid.setJobId(jobId);
        bid.setBidderId(bidderId);
        bid.setBidAmount(bidAmount);
        
        if ("agent".equalsIgnoreCase(userType)) {
            User selectedLabourer = (User) binding.labourerSpinner.getSelectedItem();
            if (selectedLabourer == null || selectedLabourer.getUserId() == null || selectedLabourer.getUserId().isEmpty()) {
                showToast("Please select a labourer to bid for");
                return null;
            }
            bid.setLabourerIdIfAgent(selectedLabourer.getUserId());
        }
        return bid;
    }
    
    private void submitBid(Bid bid) {
        firestoreService.createBid(bid, bidId -> {
            if (getDialog() == null || getContext() == null) return;
            
            if (bidId != null) {
                showToast("Bid placed successfully!");
                if (bidListener != null) bidListener.run();
                dismiss();
            } else {
                showToast("Cannot place bid. Check if the job is active and you have permission.");
            }
        });
    }
    
    private void showToast(String message) {
        if (getContext() != null) {
            Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
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
