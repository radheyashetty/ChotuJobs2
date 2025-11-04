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
import com.chotujobs.databinding.FragmentJobsListBinding;
import com.chotujobs.models.Job;
import com.chotujobs.services.FirestoreService;

import java.util.ArrayList;
import java.util.List;

public class JobsListFragment extends Fragment {

    private static final String ARG_USER_ROLE = "user_role";

    private FragmentJobsListBinding binding;
    private JobAdapter adapter;
    private FirestoreService firestoreService;
    private String currentUserId;
    private List<Job> jobList;
    private String userRole;

    public static JobsListFragment newInstance(String userRole) {
        JobsListFragment fragment = new JobsListFragment();
        Bundle args = new Bundle();
        args.putString(ARG_USER_ROLE, userRole);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            userRole = getArguments().getString(ARG_USER_ROLE);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentJobsListBinding.inflate(inflater, container, false);

        firestoreService = FirestoreService.getInstance();
        SharedPreferences prefs = requireContext().getSharedPreferences("chotujobs_prefs", 0);
        currentUserId = prefs.getString("user_id", "");

        binding.recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        jobList = new ArrayList<>();
        adapter = new JobAdapter(jobList, userRole, this::showBidDialog);
        binding.recyclerView.setAdapter(adapter);

        loadJobs();

        binding.swipeRefreshLayout.setOnRefreshListener(this::loadJobs);

        return binding.getRoot();
    }

    private void loadJobs() {
        if (binding == null) return;
        binding.swipeRefreshLayout.setRefreshing(true);
        firestoreService.getAllActiveJobs(jobs -> {
            if (isAdded() && binding != null) {
                binding.swipeRefreshLayout.setRefreshing(false);
                if (jobs != null && !jobs.isEmpty()) {
                    jobList.clear();
                    jobList.addAll(jobs);
                    adapter.notifyDataSetChanged();
                } else {
                    if (getContext() != null) {
                        Toast.makeText(getContext(), "No active jobs available", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });
    }

    private void showBidDialog(Job job) {
        BidDialogFragment dialog = BidDialogFragment.newInstance(job.getJobId(), currentUserId, userRole);
        dialog.setBidListener(() -> {
            if (isAdded()) {
                loadJobs();
                if (getContext() != null) {
                    Toast.makeText(getContext(), "Bid placed successfully!", Toast.LENGTH_SHORT).show();
                }
            }
        });
        dialog.show(getParentFragmentManager(), "bid_dialog");
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
