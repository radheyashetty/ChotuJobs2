package com.chotujobs.adapters;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.chotujobs.databinding.ItemJobBinding;
import com.chotujobs.models.Job;

public class JobAdapter extends ListAdapter<Job, JobAdapter.JobViewHolder> {

    private OnJobClickListener listener;
    private String userRole;

    public interface OnJobClickListener {
        void onJobClick(Job job);
        void onMessageClick(Job job);
    }

    public JobAdapter(String userRole, OnJobClickListener listener) {
        super(DIFF_CALLBACK);
        this.listener = listener;
        this.userRole = userRole;
    }

    private static final DiffUtil.ItemCallback<Job> DIFF_CALLBACK = new DiffUtil.ItemCallback<Job>() {
        @Override
        public boolean areItemsTheSame(@NonNull Job oldItem, @NonNull Job newItem) {
            return oldItem.getJobId().equals(newItem.getJobId());
        }

        @Override
        public boolean areContentsTheSame(@NonNull Job oldItem, @NonNull Job newItem) {
            return oldItem.equals(newItem);
        }
    };

    @NonNull
    @Override
    public JobViewHolder onCreateViewHolder(@NonNull android.view.ViewGroup parent, int viewType) {
        ItemJobBinding binding = ItemJobBinding.inflate(android.view.LayoutInflater.from(parent.getContext()), parent, false);
        return new JobViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull JobViewHolder holder, int position) {
        Job job = getItem(position);
        holder.bind(job);
    }

    class JobViewHolder extends RecyclerView.ViewHolder {
        private ItemJobBinding binding;

        public JobViewHolder(ItemJobBinding binding) {
            super(binding.getRoot());
            this.binding = binding;

            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onJobClick(getItem(position));
                }
            });
            binding.applyButton.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onJobClick(getItem(position));
                }
            });

            binding.messageButton.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onMessageClick(getItem(position));
                }
            });
        }

        public void bind(Job job) {
            binding.titleTextView.setText(job.getTitle());
            binding.categoryTextView.setText(job.getCategory());
            binding.dateTextView.setText("Start: " + job.getStartDate());
            binding.locationTextView.setText("Location: " + job.getLocation());

            if (job.getRequirements() != null && !job.getRequirements().isEmpty()) {
                binding.requirementsTextView.setText("Requirements: " + job.getRequirements());
                binding.requirementsTextView.setVisibility(android.view.View.VISIBLE);
            } else {
                binding.requirementsTextView.setVisibility(android.view.View.GONE);
            }

            if (job.getBidLimit() > 0) {
                binding.bidLimitTextView.setText("Expected Amount: " + job.getBidLimit());
                binding.bidLimitTextView.setVisibility(android.view.View.VISIBLE);
            } else {
                binding.bidLimitTextView.setVisibility(android.view.View.GONE);
            }

            if ("labourer".equals(userRole) || "agent".equals(userRole)) {
                binding.applyButton.setVisibility(android.view.View.VISIBLE);
                binding.messageButton.setVisibility(android.view.View.VISIBLE);
            } else {
                binding.applyButton.setVisibility(android.view.View.GONE);
                binding.messageButton.setVisibility(android.view.View.GONE);
            }

            if (job.getImageUrl() != null && !job.getImageUrl().isEmpty()) {
                Glide.with(itemView.getContext())
                        .load(job.getImageUrl())
                        .into(binding.imageView);
                binding.imageView.setVisibility(android.view.View.VISIBLE);
            } else {
                binding.imageView.setVisibility(android.view.View.GONE);
            }
        }
    }
}
