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
    private Uri selectedImageUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityEditProfileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        firestoreService = FirestoreService.getInstance();
        userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        loadUserProfile();

        binding.btnSave.setOnClickListener(v -> saveUserProfile());

        binding.btnChangeProfileImage.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(intent, 101);
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 101 && resultCode == RESULT_OK && data != null) {
            selectedImageUri = data.getData();
            binding.profileImageView.setImageURI(selectedImageUri);
        }
    }

    private void uploadImage(Uri imageUri, OnImageUploadListener listener) {
        StorageReference storageRef = FirebaseStorage.getInstance().getReference().child("profile_images/" + userId + ".jpg");
        storageRef.putFile(imageUri)
                .addOnSuccessListener(taskSnapshot -> storageRef.getDownloadUrl()
                        .addOnSuccessListener(uri -> listener.onSuccess(uri.toString()))
                        .addOnFailureListener(e -> listener.onSuccess(null)))
                .addOnFailureListener(e -> listener.onSuccess(null));
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

                if (user.getProfileImageUrl() != null && !user.getProfileImageUrl().isEmpty()) {
                    Glide.with(this).load(user.getProfileImageUrl()).into(binding.profileImageView);
                }
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

        if (selectedImageUri != null) {
            int finalExperience = experience;
            uploadImage(selectedImageUri, imageUrl -> {
                User user = new User();
                user.setName(name);
                user.setSkills(skills);
                user.setAddress(address);
                user.setYearsOfExperience(finalExperience);
                user.setProfileImageUrl(imageUrl);
                updateUser(user);
            });
        } else {
            User user = new User();
            user.setName(name);
            user.setSkills(skills);
            user.setAddress(address);
            user.setYearsOfExperience(experience);
            updateUser(user);
        }
    }

    private void updateUser(User user) {
        firestoreService.updateUserProfile(userId, user, success -> {
            if (success) {
                Toast.makeText(this, "Profile updated successfully", Toast.LENGTH_SHORT).show();
                finish();
            } else {
                Toast.makeText(this, "Failed to update profile", Toast.LENGTH_SHORT).show();
            }
        });
    }

    interface OnImageUploadListener {
        void onSuccess(String imageUrl);
    }
}
