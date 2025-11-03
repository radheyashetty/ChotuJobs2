package com.chotujobs.fragments;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.chotujobs.CreateJobActivity;
import com.chotujobs.adapters.ContractorJobAdapter;
import com.chotujobs.databinding.FragmentContractorBinding;
import com.chotujobs.models.Job;
import com.chotujobs.services.FirestoreService;

import java.util.ArrayList;
import java.util.List;

public class ContractorFragment extends Fragment {

    private FragmentContractorBinding binding;
    private ContractorJobAdapter adapter;
    private FirestoreService firestoreService;
    private String currentUserId;
    private List<Job> jobList;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentContractorBinding.inflate(inflater, container, false);

        firestoreService = FirestoreService.getInstance();
        SharedPreferences prefs = requireContext().getSharedPreferences("chotujobs_prefs", 0);
        currentUserId = prefs.getString("user_id", "");

        binding.recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        jobList = new ArrayList<>();
        adapter = new ContractorJobAdapter(jobList, job -> {
            showJobDetails(job);
        });
        binding.recyclerView.setAdapter(adapter);

        binding.btnCreateJob.setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), CreateJobActivity.class);
            startActivityForResult(intent, 100);
        });

        loadJobs();

        binding.swipeRefreshLayout.setOnRefreshListener(() -> loadJobs());

        return binding.getRoot();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 100) {
            loadJobs();
        }
    }

    private void loadJobs() {
        binding.swipeRefreshLayout.setRefreshing(true);
        firestoreService.getJobsByContractor(currentUserId, jobs -> {
            binding.swipeRefreshLayout.setRefreshing(false);
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

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
