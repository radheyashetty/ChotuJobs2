package com.chotujobs.fragments;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.chotujobs.R;
import com.chotujobs.adapters.JobAdapter;
import com.chotujobs.models.Job;
import com.chotujobs.services.FirestoreService;

import java.util.ArrayList;
import java.util.List;

public class AgentFragment extends Fragment {
    
    private RecyclerView recyclerView;
    private JobAdapter adapter;
    private FirestoreService firestoreService;
    private String currentUserId;
    private List<Job> jobList;
    private ProgressBar progressBar;
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_labour, container, false);
        
        firestoreService = FirestoreService.getInstance();
        SharedPreferences prefs = requireContext().getSharedPreferences("chotujobs_prefs", 0);
        currentUserId = prefs.getString("user_id", "");
        
        progressBar = view.findViewById(R.id.progressBar);
        recyclerView = view.findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        
        jobList = new ArrayList<>();
        adapter = new JobAdapter(jobList, job -> {
            // Show bid dialog with labourer selection
            showBidDialog(job);
        });
        recyclerView.setAdapter(adapter);
        
        loadJobs();
        
        return view;
    }
    
    private void loadJobs() {
        progressBar.setVisibility(View.VISIBLE);
        firestoreService.getAllActiveJobs(jobs -> {
            progressBar.setVisibility(View.GONE);
            if (jobs != null && !jobs.isEmpty()) {
                jobList.clear();
                jobList.addAll(jobs);
                adapter.notifyDataSetChanged();
            } else {
                Toast.makeText(getContext(), "No active jobs available", Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    private void showBidDialog(Job job) {
        BidDialogFragment dialog = BidDialogFragment.newInstance(job.getJobId(), currentUserId, null);
        dialog.setBidListener(() -> {
            loadJobs();
            Toast.makeText(getContext(), "Bid placed successfully!", Toast.LENGTH_SHORT).show();
        });
        dialog.show(getParentFragmentManager(), "bid_dialog");
    }
}

