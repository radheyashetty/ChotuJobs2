package com.chotujobs.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.chotujobs.databinding.ItemContractorJobBinding;
import com.chotujobs.models.Job;

public class ContractorJobAdapter extends ListAdapter<Job, ContractorJobAdapter.JobViewHolder> {

    private OnJobClickListener listener;

    public interface OnJobClickListener {
        void onJobClick(Job job);
    }

    public ContractorJobAdapter(OnJobClickListener listener) {
        super(DIFF_CALLBACK);
        this.listener = listener;
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
    public JobViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemContractorJobBinding binding = ItemContractorJobBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new JobViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull JobViewHolder holder, int position) {
        Job job = getItem(position);
        holder.bind(job);
    }

    class JobViewHolder extends RecyclerView.ViewHolder {
        private ItemContractorJobBinding binding;

        public JobViewHolder(ItemContractorJobBinding binding) {
            super(binding.getRoot());
            this.binding = binding;

            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onJobClick(getItem(position));
                }
            });
        }

        public void bind(Job job) {
            binding.titleTextView.setText(job.getTitle());
            binding.categoryTextView.setText(job.getCategory());
            binding.statusTextView.setText("Status: " + job.getStatus().toUpperCase());
            binding.dateTextView.setText("Start: " + job.getStartDate());
            binding.locationTextView.setText("Location: " + job.getLocation());

            if (job.getImageUrl() != null && !job.getImageUrl().isEmpty()) {
                binding.imageView.setVisibility(View.VISIBLE);
                Glide.with(itemView.getContext())
                        .load(job.getImageUrl())
                        .into(binding.imageView);
            } else {
                binding.imageView.setVisibility(View.GONE);
            }
        }
    }
}
