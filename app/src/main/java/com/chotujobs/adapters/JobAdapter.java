package com.chotujobs.adapters;

import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.chotujobs.ChatActivity;
import com.chotujobs.databinding.ItemJobBinding;
import com.chotujobs.models.Job;
import com.chotujobs.services.FirestoreService;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

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
    public JobViewHolder onCreateViewHolder(@NonNull android.view.ViewGroup parent, int viewType) {
        ItemJobBinding binding = ItemJobBinding.inflate(android.view.LayoutInflater.from(parent.getContext()), parent, false);
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
            Context context = itemView.getContext();

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
                    FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
                    if (currentUser == null) {
                        Toast.makeText(context, "You must be logged in to send a message.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    Job job = jobList.get(position);
                    String currentUserId = currentUser.getUid();
                    FirestoreService.getInstance().createChat(currentUserId, job.getContractorId(), chatId -> {
                        if (chatId != null) {
                            Intent intent = new Intent(context, ChatActivity.class);
                            intent.putExtra("chatId", chatId);
                            intent.putExtra("receiverId", job.getContractorId());
                            context.startActivity(intent);
                        } else {
                            Toast.makeText(context, "Failed to create or open chat.", Toast.LENGTH_SHORT).show();
                        }
                    });
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
