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

    public BidAdapter(@NonNull Context context, List<Bid> bids, Map<String, User> userMap) {
        super(context, 0, bids);
        this.userMap = userMap;
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

            if (bid.getWinnerFlag() == 1) {
                binding.bidderNameTextView.append(" - WINNER");
            }
        }

        return convertView;
    }
}
