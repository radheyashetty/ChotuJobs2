package com.chotujobs.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.chotujobs.R;
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
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_contractor_job, parent, false);
        return new JobViewHolder(view);
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
        private TextView titleTextView;
        private TextView categoryTextView;
        private TextView statusTextView;
        private TextView dateTextView;
        
        public JobViewHolder(@NonNull View itemView) {
            super(itemView);
            titleTextView = itemView.findViewById(R.id.titleTextView);
            categoryTextView = itemView.findViewById(R.id.categoryTextView);
            statusTextView = itemView.findViewById(R.id.statusTextView);
            dateTextView = itemView.findViewById(R.id.dateTextView);
            
            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onJobClick(jobList.get(position));
                }
            });
        }
        
        public void bind(Job job) {
            titleTextView.setText(job.getTitle());
            categoryTextView.setText(job.getCategory());
            statusTextView.setText("Status: " + job.getStatus().toUpperCase());
            dateTextView.setText("Start: " + job.getStartDate());
        }
    }
}

