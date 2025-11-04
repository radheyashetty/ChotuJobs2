package com.chotujobs;

import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.chotujobs.databinding.ActivityEditProfileBinding;
import com.chotujobs.models.User;
import com.chotujobs.services.FirestoreService;
import com.google.firebase.auth.FirebaseAuth;

import java.util.Arrays;
import java.util.List;

public class EditProfileActivity extends AppCompatActivity {

    private ActivityEditProfileBinding binding;
    private FirestoreService firestoreService;
    private String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityEditProfileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        firestoreService = FirestoreService.getInstance();
        userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        loadUserProfile();

        binding.btnSave.setOnClickListener(v -> saveUserProfile());
    }

    private void loadUserProfile() {
        firestoreService.getUserProfile(userId, user -> {
            if (user != null) {
                binding.nameEditText.setText(user.getName());
                if (user.getSkills() != null) {
                    binding.skillsEditText.setText(String.join(", ", user.getSkills()));
                }
                binding.addressEditText.setText(user.getAddress());
                binding.experienceEditText.setText(String.valueOf(user.getYearsOfExperience()));
            }
        });
    }

    private void saveUserProfile() {
        String name = binding.nameEditText.getText().toString().trim();
        String skillsStr = binding.skillsEditText.getText().toString().trim();
        String address = binding.addressEditText.getText().toString().trim();
        String experienceStr = binding.experienceEditText.getText().toString().trim();

        if (name.isEmpty()) {
            Toast.makeText(this, "Please enter your name", Toast.LENGTH_SHORT).show();
            return;
        }

        List<String> skills = Arrays.asList(skillsStr.split("\\s*,\\s*"));
        int experience = 0;
        if (!experienceStr.isEmpty()) {
            try {
                experience = Integer.parseInt(experienceStr);
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Invalid number for years of experience", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        User user = new User();
        user.setName(name);
        user.setSkills(skills);
        user.setAddress(address);
        user.setYearsOfExperience(experience);

        firestoreService.updateUserProfile(userId, user, success -> {
            if (success) {
                Toast.makeText(this, "Profile updated successfully", Toast.LENGTH_SHORT).show();
                finish();
            } else {
                Toast.makeText(this, "Failed to update profile", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
