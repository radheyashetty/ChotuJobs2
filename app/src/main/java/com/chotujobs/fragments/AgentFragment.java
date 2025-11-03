package com.chotujobs.fragments;

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

import com.chotujobs.adapters.JobAdapter;
import com.chotujobs.databinding.FragmentAgentBinding;
import com.chotujobs.models.Job;
import com.chotujobs.services.FirestoreService;

import java.util.ArrayList;
import java.util.List;

public class AgentFragment extends Fragment {

    private FragmentAgentBinding binding;
    private JobAdapter adapter;
    private FirestoreService firestoreService;
    private String currentUserId;
    private List<Job> jobList;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentAgentBinding.inflate(inflater, container, false);

        firestoreService = FirestoreService.getInstance();
        SharedPreferences prefs = requireContext().getSharedPreferences("chotujobs_prefs", 0);
        currentUserId = prefs.getString("user_id", "");

        binding.recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        jobList = new ArrayList<>();
        adapter = new JobAdapter(jobList, job -> {
            showBidDialog(job);
        });
        binding.recyclerView.setAdapter(adapter);

        loadJobs();

        binding.swipeRefreshLayout.setOnRefreshListener(() -> loadJobs());

        return binding.getRoot();
    }

    private void loadJobs() {
        binding.swipeRefreshLayout.setRefreshing(true);
        firestoreService.getAllActiveJobs(jobs -> {
            binding.swipeRefreshLayout.setRefreshing(false);
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
        BidDialogFragment dialog = BidDialogFragment.newInstance(job.getJobId(), currentUserId, "agent");
        dialog.setBidListener(() -> {
            loadJobs();
            Toast.makeText(getContext(), "Bid placed successfully!", Toast.LENGTH_SHORT).show();
        });
        dialog.show(getParentFragmentManager(), "bid_dialog");
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
