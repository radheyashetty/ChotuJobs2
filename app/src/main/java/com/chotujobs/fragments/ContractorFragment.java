package com.chotujobs.fragments;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.chotujobs.CreateJobActivity;
import com.chotujobs.R;
import com.chotujobs.adapters.ContractorJobAdapter;
import com.chotujobs.models.Job;
import com.chotujobs.services.FirestoreService;

import java.util.ArrayList;
import java.util.List;

public class ContractorFragment extends Fragment {
    
    private RecyclerView recyclerView;
    private ContractorJobAdapter adapter;
    private FirestoreService firestoreService;
    private String currentUserId;
    private List<Job> jobList;
    private Button createJobButton;
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_contractor, container, false);
        
        firestoreService = FirestoreService.getInstance();
        SharedPreferences prefs = requireContext().getSharedPreferences("chotujobs_prefs", 0);
        currentUserId = prefs.getString("user_id", "");
        
        createJobButton = view.findViewById(R.id.btnCreateJob);
        recyclerView = view.findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        
        jobList = new ArrayList<>();
        adapter = new ContractorJobAdapter(jobList, job -> {
            // Show job details with bids and select winner
            showJobDetails(job);
        });
        recyclerView.setAdapter(adapter);
        
        createJobButton.setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), CreateJobActivity.class);
            startActivityForResult(intent, 100);
        });
        
        loadJobs();
        
        return view;
    }
    
    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 100) {
            loadJobs();
        }
    }
    
    private void loadJobs() {
        firestoreService.getJobsByContractor(currentUserId, jobs -> {
            if (jobs != null) {
                jobList.clear();
                jobList.addAll(jobs);
                adapter.notifyDataSetChanged();
            } else {
                Toast.makeText(getContext(), "Error loading jobs", Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    private void showJobDetails(Job job) {
        JobDetailsDialogFragment dialog = JobDetailsDialogFragment.newInstance(job.getJobId());
        dialog.setJobClosedListener(() -> {
            loadJobs();
            Toast.makeText(getContext(), "Job closed successfully!", Toast.LENGTH_SHORT).show();
        });
        dialog.show(getParentFragmentManager(), "job_details");
    }
}

