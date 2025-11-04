package com.chotujobs.adapters;

import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.chotujobs.ChatActivity;
import com.chotujobs.databinding.ItemJobBinding;
import com.chotujobs.models.Job;
import com.chotujobs.services.FirestoreService;
import com.google.firebase.auth.FirebaseAuth;

import java.util.List;

public class JobAdapter extends RecyclerView.Adapter<JobAdapter.JobViewHolder> {

    private List<Job> jobList;
    private OnJobClickListener listener;
    private String userRole;

    public interface OnJobClickListener {
        void onJobClick(Job job);
    }

    public JobAdapter(List<Job> jobList, String userRole, OnJobClickListener listener) {
        this.jobList = jobList;
        this.listener = listener;
        this.userRole = userRole;
    }

    @NonNull
    @Override
    public JobViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemJobBinding binding = ItemJobBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
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
        private ItemJobBinding binding;

        public JobViewHolder(ItemJobBinding binding) {
            super(binding.getRoot());
            this.binding = binding;

            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onJobClick(jobList.get(position));
                }
            });
            binding.applyButton.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onJobClick(jobList.get(position));
                }
            });
            binding.messageButton.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    Job job = jobList.get(position);
                    String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
                    FirestoreService.getInstance().createChat(currentUserId, job.getContractorId(), chatId -> {
                        if (chatId != null) {
                            Intent intent = new Intent(itemView.getContext(), ChatActivity.class);
                            intent.putExtra("chatId", chatId);
                            intent.putExtra("receiverId", job.getContractorId());
                            itemView.getContext().startActivity(intent);
                        }
                    });
                }
            });
        }

        public void bind(Job job) {
            binding.titleTextView.setText(job.getTitle());
            binding.categoryTextView.setText(job.getCategory());
            binding.dateTextView.setText("Start: " + job.getStartDate());

            if ("labourer".equals(userRole) || "agent".equals(userRole)) {
                binding.applyButton.setVisibility(View.VISIBLE);
            } else {
                binding.applyButton.setVisibility(View.GONE);
            }

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
