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

import com.bumptech.glide.Glide;
import com.chotujobs.EditProfileActivity;
import com.chotujobs.LoginActivity;
import com.chotujobs.databinding.FragmentProfileBinding;
import com.chotujobs.models.User;
import com.chotujobs.services.FirestoreService;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class ProfileFragment extends Fragment {

    private FragmentProfileBinding binding;
    private FirebaseAuth auth;
    private SharedPreferences prefs;
    private ActivityResultLauncher<Intent> editProfileLauncher;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentProfileBinding.inflate(inflater, container, false);

        auth = FirebaseAuth.getInstance();
        prefs = requireContext().getSharedPreferences("chotujobs_prefs", 0);

        // Register for activity result
        editProfileLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        // Profile was updated, reload it
                        loadUserProfile();
                    }
                });

        loadUserProfile();

        binding.btnEditProfile.setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), EditProfileActivity.class);
            editProfileLauncher.launch(intent);
        });

        binding.btnLogout.setOnClickListener(v -> handleLogout());

        return binding.getRoot();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadUserProfile();
    }

    private void loadUserProfile() {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            if (isAdded() && binding != null) {
                binding.userNameTextView.setText("Not logged in");
            }
            return;
        }
        String userId = currentUser.getUid();
        FirestoreService.getInstance().getUserProfile(userId, user -> {
            if (!isAdded() || binding == null) {
                return;
            }
            
            if (user == null) {
                binding.userNameTextView.setText("Failed to load profile");
                return;
            }
            
            // Load profile image
            if (user.getProfileImageUrl() != null && !user.getProfileImageUrl().isEmpty()) {
                Glide.with(requireContext())
                        .load(user.getProfileImageUrl())
                        .placeholder(android.R.drawable.ic_menu_gallery)
                        .error(android.R.drawable.ic_menu_gallery)
                        .circleCrop()
                        .into(binding.profileImageView);
            } else {
                // Set default placeholder if no image
                binding.profileImageView.setImageResource(android.R.drawable.ic_menu_gallery);
            }
            
            // Display name
            String userName = user.getName();
            if (userName != null && !userName.trim().isEmpty()) {
                binding.userNameTextView.setText(userName);
            } else {
                binding.userNameTextView.setText("Name not set");
            }
            
            // Display role
            String role = user.getRole();
            if (role != null && !role.trim().isEmpty()) {
                String capitalizedRole = role.length() > 1 
                    ? role.substring(0, 1).toUpperCase() + role.substring(1)
                    : role.toUpperCase();
                binding.userRoleTextView.setText("Role: " + capitalizedRole);
            } else {
                binding.userRoleTextView.setText("Role: Unknown");
            }
            
            // Display contact (email or phone)
            if (user.getEmail() != null && !user.getEmail().trim().isEmpty()) {
                binding.userContactTextView.setText(user.getEmail());
            } else if (user.getPhone() != null && !user.getPhone().trim().isEmpty()) {
                binding.userContactTextView.setText(user.getPhone());
            } else {
                binding.userContactTextView.setText("Contact: Not set");
            }
            
            // Display address
            String address = user.getAddress();
            if (address != null && !address.trim().isEmpty()) {
                binding.userAddressTextView.setText("Address: " + address);
            } else {
                binding.userAddressTextView.setText("Address: Not set");
            }
            
            // Display skills
            if (user.getSkills() != null && !user.getSkills().isEmpty()) {
                binding.userSkillsTextView.setText("Skills: " + String.join(", ", user.getSkills()));
            } else {
                binding.userSkillsTextView.setText("Skills: Not set");
            }
            
            // Display experience
            binding.userExperienceTextView.setText("Experience: " + user.getYearsOfExperience() + " years");
        });
    }

    private void handleLogout() {
        auth.signOut();
        SharedPreferences.Editor editor = prefs.edit();
        editor.clear();
        editor.apply();

        if (getContext() != null) {
            Toast.makeText(getContext(), "Logged out successfully", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(getContext(), LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
