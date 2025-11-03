package com.chotujobs;

import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.chotujobs.databinding.ActivityEditProfileBinding;
import com.chotujobs.models.User;
import com.chotujobs.services.FirestoreService;
import com.google.firebase.auth.FirebaseAuth;

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
            }
        });
    }

    private void saveUserProfile() {
        String name = binding.nameEditText.getText().toString().trim();
        if (name.isEmpty()) {
            Toast.makeText(this, "Please enter your name", Toast.LENGTH_SHORT).show();
            return;
        }

        firestoreService.updateUserProfile(userId, name, success -> {
            if (success) {
                Toast.makeText(this, "Profile updated successfully", Toast.LENGTH_SHORT).show();
                finish();
            } else {
                Toast.makeText(this, "Failed to update profile", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
