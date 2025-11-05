package com.chotujobs.fragments;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.chotujobs.CreateJobActivity;
import com.chotujobs.adapters.ContractorJobAdapter;
import com.chotujobs.databinding.FragmentContractorBinding;
import com.chotujobs.models.Job;
import com.chotujobs.services.FirestoreService;

public class ContractorFragment extends Fragment {

    private FragmentContractorBinding binding;
    private ContractorJobAdapter adapter;
    private FirestoreService firestoreService;
    private String currentUserId;
    private ActivityResultLauncher<Intent> createJobLauncher;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentContractorBinding.inflate(inflater, container, false);

        firestoreService = FirestoreService.getInstance();
        SharedPreferences prefs = requireContext().getSharedPreferences("chotujobs_prefs", 0);
        String authUserId = firestoreService.getCurrentUserId();
        currentUserId = authUserId != null ? authUserId : prefs.getString("user_id", "");

        binding.recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        adapter = new ContractorJobAdapter(this::showJobDetails);
        binding.recyclerView.setAdapter(adapter);

        createJobLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        loadJobs();
                    }
                });

        binding.btnCreateJob.setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), CreateJobActivity.class);
            createJobLauncher.launch(intent);
        });

        loadJobs();

        binding.swipeRefreshLayout.setOnRefreshListener(this::loadJobs);

        return binding.getRoot();
    }

    private void loadJobs() {
        binding.swipeRefreshLayout.setRefreshing(true);
        firestoreService.getJobsByContractor(currentUserId, jobs -> {
            if (isAdded() && binding != null) {
                binding.swipeRefreshLayout.setRefreshing(false);
                if (jobs != null) {
                    adapter.submitList(jobs);
                }
            }
        });
    }

    private void showJobDetails(Job job) {
        if (job == null || job.getJobId() == null || job.getJobId().isEmpty()) {
            if (getContext() != null) {
                Toast.makeText(getContext(), "Invalid job information", Toast.LENGTH_SHORT).show();
            }
            return;
        }
        
        JobDetailsDialogFragment dialog = JobDetailsDialogFragment.newInstance(job.getJobId());
        dialog.setJobClosedListener(() -> {
            if(isAdded()){
                loadJobs();
                Toast.makeText(getContext(), "Job closed successfully!", Toast.LENGTH_SHORT).show();
            }
        });
        dialog.show(getParentFragmentManager(), "job_details");
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
