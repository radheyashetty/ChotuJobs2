package com.chotujobs.fragments;

import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.bumptech.glide.Glide;
import com.chotujobs.databinding.DialogBidderDetailsBinding;
import com.chotujobs.models.Bid;
import com.chotujobs.models.User;
import com.chotujobs.services.FirestoreService;

public class BidderDetailsDialogFragment extends DialogFragment {

    private static final String ARG_BID = "bid";
    private static final String ARG_BIDDER = "bidder";

    private DialogBidderDetailsBinding binding;
    private Bid bid;
    private User bidder;
    private OnWinnerSelectedListener listener;

    public interface OnWinnerSelectedListener {
        void onWinnerSelected(Bid bid);
    }

    public static BidderDetailsDialogFragment newInstance(Bid bid, User bidder) {
        BidderDetailsDialogFragment fragment = new BidderDetailsDialogFragment();
        Bundle args = new Bundle();
        args.putParcelable(ARG_BID, bid);
        args.putParcelable(ARG_BIDDER, bidder);
        fragment.setArguments(args);
        return fragment;
    }

    public void setOnWinnerSelectedListener(OnWinnerSelectedListener listener) {
        this.listener = listener;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            bid = getArguments().getParcelable(ARG_BID);
            bidder = getArguments().getParcelable(ARG_BIDDER);
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        if (getContext() == null) {
            return super.onCreateDialog(savedInstanceState);
        }
        binding = DialogBidderDetailsBinding.inflate(requireActivity().getLayoutInflater());

        if (bidder != null) {
            if (bidder.getProfileImageUrl() != null && !bidder.getProfileImageUrl().isEmpty()) {
                Glide.with(this).load(bidder.getProfileImageUrl()).into(binding.profileImageView);
            }
            binding.bidderNameTextView.setText(bidder.getName());
            if (bidder.getEmail() != null) {
                binding.bidderContactTextView.setText(bidder.getEmail());
            } else {
                binding.bidderContactTextView.setText(bidder.getPhone());
            }
        }

        if (bid != null) {
            binding.bidAmountTextView.setText("Bid Amount: ₹" + bid.getBidAmount());
        }

        binding.btnSelectWinner.setOnClickListener(v -> {
            if (listener != null) {
                listener.onWinnerSelected(bid);
                dismiss();
            }
        });

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setView(binding.getRoot());
        return builder.create();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
