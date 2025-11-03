package com.chotujobs.fragments;

import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.chotujobs.R;
import com.chotujobs.models.Bid;
import com.chotujobs.models.User;
import com.chotujobs.services.FirestoreService;

import java.util.List;

public class BidDialogFragment extends DialogFragment {
    
    private String jobId;
    private String bidderId;
    private String selectedLabourerId; // nullable for agent
    private Runnable bidListener;
    private FirestoreService firestoreService;
    private Spinner labourerSpinner;
    private EditText bidAmountEditText;
    
    public static BidDialogFragment newInstance(String jobId, String bidderId, String labourerId) {
        BidDialogFragment fragment = new BidDialogFragment();
        Bundle args = new Bundle();
        args.putString("job_id", jobId);
        args.putString("bidder_id", bidderId);
        if (labourerId != null) {
            args.putString("labourer_id", labourerId);
        }
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
            selectedLabourerId = getArguments().getString("labourer_id");
        }
        
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_bid, null);
        
        labourerSpinner = view.findViewById(R.id.labourerSpinner);
        bidAmountEditText = view.findViewById(R.id.bidAmountEditText);
        
        // Load labourers if agent
        if (selectedLabourerId == null && labourerSpinner != null) {
            setupLabourerSpinner();
        } else if (labourerSpinner != null) {
            labourerSpinner.setVisibility(View.GONE);
        }
        
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setView(view)
                .setTitle("Place Bid")
                .setPositiveButton("Submit", (dialog, which) -> handleBidSubmit())
                .setNegativeButton("Cancel", null);
        
        return builder.create();
    }
    
    private void setupLabourerSpinner() {
        firestoreService.getUsersByRole("labour", users -> {
            if (users != null && !users.isEmpty()) {
                android.widget.ArrayAdapter<User> adapter = new android.widget.ArrayAdapter<>(
                        getContext(), android.R.layout.simple_spinner_item, users);
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                labourerSpinner.setAdapter(adapter);
            }
        });
    }
    
    private void handleBidSubmit() {
        try {
            String amountStr = bidAmountEditText.getText().toString().trim();
            if (amountStr.isEmpty()) {
                Toast.makeText(getContext(), "Please enter bid amount", Toast.LENGTH_SHORT).show();
                return;
            }
            
            double bidAmount = Double.parseDouble(amountStr);
            
            Bid bid = new Bid();
            bid.setJobId(jobId);
            bid.setBidderId(bidderId);
            bid.setBidAmount(bidAmount);
            
            // If agent selected a labourer from spinner
            if (labourerSpinner != null && labourerSpinner.getVisibility() == View.VISIBLE) {
                User selectedLabourer = (User) labourerSpinner.getSelectedItem();
                if (selectedLabourer != null) {
                    bid.setLabourerIdIfAgent(selectedLabourer.getUserId());
                }
            } else if (selectedLabourerId != null) {
                bid.setLabourerIdIfAgent(selectedLabourerId);
            }
            
            firestoreService.createBid(bid, bidId -> {
                if (bidId != null && bidListener != null) {
                    bidListener.run();
                }
            });
        } catch (NumberFormatException e) {
            Toast.makeText(getContext(), "Invalid bid amount", Toast.LENGTH_SHORT).show();
        }
    }
    
    public void setBidListener(Runnable listener) {
        this.bidListener = listener;
    }
}
