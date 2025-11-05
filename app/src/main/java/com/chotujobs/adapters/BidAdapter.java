package com.chotujobs.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import com.chotujobs.databinding.ItemBidNewBinding;
import com.chotujobs.models.Bid;
import com.chotujobs.models.User;
import java.util.Map;

public class BidAdapter extends ListAdapter<Bid, BidAdapter.BidViewHolder> {

    private Map<String, User> userMap;
    private OnBidActionClickListener listener;

    public interface OnBidActionClickListener {
        void onAcceptBidClick(Bid bid);
        void onRejectBidClick(Bid bid);
    }

    public BidAdapter(Map<String, User> userMap, OnBidActionClickListener listener) {
        super(DIFF_CALLBACK);
        this.userMap = userMap;
        this.listener = listener;
    }

    private static final DiffUtil.ItemCallback<Bid> DIFF_CALLBACK = new DiffUtil.ItemCallback<Bid>() {
        @Override
        public boolean areItemsTheSame(@NonNull Bid oldItem, @NonNull Bid newItem) {
            return safeEquals(oldItem.getBidId(), newItem.getBidId());
        }

        @Override
        public boolean areContentsTheSame(@NonNull Bid oldItem, @NonNull Bid newItem) {
            return oldItem.getBidAmount() == newItem.getBidAmount()
                    && safeEquals(oldItem.getStatus(), newItem.getStatus())
                    && safeEquals(oldItem.getBidderId(), newItem.getBidderId())
                    && safeEquals(oldItem.getLabourerIdIfAgent(), newItem.getLabourerIdIfAgent());
        }
    };

    private static boolean safeEquals(Object a, Object b) {
        return a == b || (a != null && a.equals(b));
    }

    @NonNull
    @Override
    public BidViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemBidNewBinding binding = ItemBidNewBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new BidViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull BidViewHolder holder, int position) {
        Bid bid = getItem(position);
        holder.bind(bid);
    }

    class BidViewHolder extends RecyclerView.ViewHolder {
        private ItemBidNewBinding binding;

        public BidViewHolder(ItemBidNewBinding binding) {
            super(binding.getRoot());
            this.binding = binding;

            binding.acceptBidButton.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onAcceptBidClick(getItem(position));
                }
            });

            binding.rejectBidButton.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onRejectBidClick(getItem(position));
                }
            });
        }

        public void bind(Bid bid) {
            if (bid == null) return;
            
            String bidderName = "Unknown";
            if (bid.getBidderId() != null) {
                User bidder = userMap.get(bid.getBidderId());
                if (bidder != null && bidder.getName() != null) {
                    bidderName = bidder.getName();
                    if (bid.getLabourerIdIfAgent() != null) {
                        User labourer = userMap.get(bid.getLabourerIdIfAgent());
                        if (labourer != null && labourer.getName() != null) {
                            bidderName += " (for " + labourer.getName() + ")";
                        }
                    }
                }
            }
            binding.bidderNameTextView.setText(bidderName);

            binding.bidAmountTextView.setText("Bid: ₹" + bid.getBidAmount());
            String status = bid.getStatus() != null ? bid.getStatus() : "pending";
            binding.bidStatusTextView.setText("Status: " + status);

            boolean isPending = "pending".equals(bid.getStatus());
            binding.acceptBidButton.setVisibility(isPending ? View.VISIBLE : View.GONE);
            binding.rejectBidButton.setVisibility(isPending ? View.VISIBLE : View.GONE);
        }
    }
}
