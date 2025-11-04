package com.chotujobs.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.chotujobs.databinding.ItemBidBinding;
import com.chotujobs.models.Bid;
import com.chotujobs.models.User;

import java.util.List;
import java.util.Map;

public class BidAdapter extends ArrayAdapter<Bid> {

    private Map<String, User> userMap;
    private OnBidActionClickListener listener;

    public interface OnBidActionClickListener {
        void onAcceptBidClick(Bid bid);
        void onRejectBidClick(Bid bid);
    }

    public BidAdapter(@NonNull Context context, List<Bid> bids, Map<String, User> userMap, OnBidActionClickListener listener) {
        super(context, 0, bids);
        this.userMap = userMap;
        this.listener = listener;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        ItemBidBinding binding;
        if (convertView == null) {
            binding = ItemBidBinding.inflate(LayoutInflater.from(getContext()), parent, false);
            convertView = binding.getRoot();
            convertView.setTag(binding);
        } else {
            binding = (ItemBidBinding) convertView.getTag();
        }

        Bid bid = getItem(position);
        if (bid != null) {
            User bidder = userMap.get(bid.getBidderId());
            if (bidder != null) {
                String bidderName = bidder.getName();
                if (bid.getLabourerIdIfAgent() != null) {
                    User labourer = userMap.get(bid.getLabourerIdIfAgent());
                    if (labourer != null) {
                        bidderName += " (for " + labourer.getName() + ")";
                    }
                }
                binding.bidderNameTextView.setText(bidderName);
            }

            binding.bidAmountTextView.setText("Bid: ₹" + bid.getBidAmount());

            binding.bidStatusTextView.setText("Status: " + bid.getStatus());

            if ("accepted".equals(bid.getStatus())) {
                binding.acceptBidButton.setVisibility(View.GONE);
                binding.rejectBidButton.setVisibility(View.GONE);
            } else if ("rejected".equals(bid.getStatus())) {
                binding.acceptBidButton.setVisibility(View.GONE);
                binding.rejectBidButton.setVisibility(View.GONE);
            } else {
                binding.acceptBidButton.setVisibility(View.VISIBLE);
                binding.rejectBidButton.setVisibility(View.VISIBLE);
            }

            binding.acceptBidButton.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onAcceptBidClick(bid);
                }
            });

            binding.rejectBidButton.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onRejectBidClick(bid);
                }
            });
        }

        return convertView;
    }
}
