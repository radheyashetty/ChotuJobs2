package com.chotujobs.adapters;

import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.chotujobs.databinding.ItemContractorJobBinding;
import com.chotujobs.models.Job;

import java.util.List;

public class ContractorJobAdapter extends RecyclerView.Adapter<ContractorJobAdapter.JobViewHolder> {

    private List<Job> jobList;
    private OnJobClickListener listener;

    public interface OnJobClickListener {
        void onJobClick(Job job);
    }

    public ContractorJobAdapter(List<Job> jobList, OnJobClickListener listener) {
        this.jobList = jobList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public JobViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemContractorJobBinding binding = ItemContractorJobBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new JobViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull JobViewHolder holder, int position) {
        Job job = jobList.get(position);
        holder.bind(job);
    }

    @Override
    public int getItemCount() {
        return jobList.size();
    }

    class JobViewHolder extends RecyclerView.ViewHolder {
        private ItemContractorJobBinding binding;

        public JobViewHolder(ItemContractorJobBinding binding) {
            super(binding.getRoot());
            this.binding = binding;

            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onJobClick(jobList.get(position));
                }
            });
        }

        public void bind(Job job) {
            binding.titleTextView.setText(job.getTitle());
            binding.categoryTextView.setText(job.getCategory());
            binding.statusTextView.setText("Status: " + job.getStatus().toUpperCase());
            binding.dateTextView.setText("Start: " + job.getStartDate());

            if (job.getImagePath() != null && !job.getImagePath().isEmpty()) {
                binding.imageView.setVisibility(View.VISIBLE);
                Glide.with(itemView.getContext())
                        .load(Uri.parse(job.getImagePath()))
                        .into(binding.imageView);
            } else {
                binding.imageView.setVisibility(View.GONE);
            }
        }
    }
}
