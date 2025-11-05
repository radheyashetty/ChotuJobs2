package com.chotujobs;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.chotujobs.databinding.ActivityCreateJobBinding;
import com.chotujobs.models.Job;
import com.chotujobs.services.FirestoreService;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class CreateJobActivity extends AppCompatActivity {

    private ActivityCreateJobBinding binding;
    private FirestoreService firestoreService;
    private String contractorId;
    private Uri selectedImageUri = null;
    private Calendar calendar;
    private ActivityResultLauncher<Intent> imagePickerLauncher;
    private boolean isContractorUser = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCreateJobBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        firestoreService = FirestoreService.getInstance();
        // Prefer authenticated userId; fall back to stored preference
        String authUserId = firestoreService.getCurrentUserId();
        contractorId = authUserId != null ? authUserId : getSharedPreferences("chotujobs_prefs", 0).getString("user_id", "");
        calendar = Calendar.getInstance();

        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        selectedImageUri = result.getData().getData();
                        binding.imageView.setImageURI(selectedImageUri);
                        binding.imageView.setVisibility(View.VISIBLE);
                    }
                });

        // Resolve role to ensure only contractors can post. Disable save until resolved.
        binding.saveButton.setEnabled(false);
        if (contractorId != null && !contractorId.isEmpty()) {
            firestoreService.getUserProfile(contractorId, user -> {
                isContractorUser = (user != null && "contractor".equals(user.getRole()));
                binding.saveButton.setEnabled(isContractorUser);
                if (!isContractorUser) {
                    Toast.makeText(this, "Only contractor accounts can create jobs", Toast.LENGTH_LONG).show();
                }
            });
        } else {
            Toast.makeText(this, "Please log in again before creating a job", Toast.LENGTH_LONG).show();
        }

        setupCategorySpinner();
        setupDatePicker();
        setupListeners();
    }

    private void setupCategorySpinner() {
        List<String> categories = new ArrayList<>();
        categories.add("Construction");
        categories.add("Electricity");
        categories.add("Plumbing");
        categories.add("Painting");
        categories.add("Carpentry");
        categories.add("Other");

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, categories) {
            @Override
            public android.view.View getView(int position, android.view.View convertView, android.view.ViewGroup parent) {
                android.view.View view = super.getView(position, convertView, parent);
                android.widget.TextView textView = (android.widget.TextView) view;
                textView.setTextColor(getResources().getColor(com.chotujobs.R.color.design_default_color_on_surface, null));
                return view;
            }
            
            @Override
            public android.view.View getDropDownView(int position, android.view.View convertView, android.view.ViewGroup parent) {
                android.view.View view = super.getDropDownView(position, convertView, parent);
                android.widget.TextView textView = (android.widget.TextView) view;
                textView.setTextColor(getResources().getColor(com.chotujobs.R.color.design_default_color_on_surface, null));
                view.setBackgroundColor(getResources().getColor(com.chotujobs.R.color.design_default_color_surface, null));
                return view;
            }
        };
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.categorySpinner.setAdapter(adapter);
    }

    private void setupDatePicker() {
        DatePickerDialog.OnDateSetListener dateListener = (view, year, month, dayOfMonth) -> {
            calendar.set(Calendar.YEAR, year);
            calendar.set(Calendar.MONTH, month);
            calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
            updateDateLabel();
        };

        binding.startDateEditText.setOnClickListener(v -> new DatePickerDialog(
                this, dateListener,
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        ).show());
    }

    private void updateDateLabel() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        binding.startDateEditText.setText(sdf.format(calendar.getTime()));
    }

    private void setupListeners() {
        binding.addImageButton.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            imagePickerLauncher.launch(intent);
        });

        binding.saveButton.setOnClickListener(v -> saveJob());
    }

    private void saveJob() {
        if (contractorId == null || contractorId.isEmpty()) {
            Toast.makeText(this, "Please log in again before creating a job", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!isContractorUser) {
            Toast.makeText(this, "Only contractor accounts can create jobs", Toast.LENGTH_SHORT).show();
            return;
        }
        String title = binding.titleEditText.getText().toString().trim();
        Object selectedCategory = binding.categorySpinner.getSelectedItem();
        String category = selectedCategory != null ? selectedCategory.toString() : "Other";
        String startDate = binding.startDateEditText.getText().toString().trim();
        String location = binding.locationEditText.getText().toString().trim();
        String requirements = binding.requirementsEditText.getText().toString().trim();
        String bidLimitStr = binding.bidLimitEditText.getText().toString().trim();

        if (title.isEmpty()) {
            Toast.makeText(this, "Please enter job title", Toast.LENGTH_SHORT).show();
            return;
        }
        if (startDate.isEmpty()) {
            Toast.makeText(this, "Please select start date", Toast.LENGTH_SHORT).show();
            return;
        }
        if (location.isEmpty()) {
            Toast.makeText(this, "Please enter a location", Toast.LENGTH_SHORT).show();
            return;
        }
        int bidLimit = 0;
        if (!bidLimitStr.isEmpty()) {
            try {
                bidLimit = Integer.parseInt(bidLimitStr);
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Please enter a valid bid limit", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        binding.progressBar.setVisibility(View.VISIBLE);
        binding.saveButton.setEnabled(false);
        int finalBidLimit = bidLimit;
        if (selectedImageUri != null) {
            uploadImage(selectedImageUri, (imageUrl) -> {
                createJobInFirestore(title, category, startDate, location, imageUrl, requirements, finalBidLimit);
            });
        } else {
            createJobInFirestore(title, category, startDate, location, null, requirements, bidLimit);
        }
    }

    private void uploadImage(Uri imageUri, OnImageUploadListener listener) {
        StorageReference storageRef = FirebaseStorage.getInstance().getReference().child("job_images/" + System.currentTimeMillis() + ".jpg");
        storageRef.putFile(imageUri)
                .addOnSuccessListener(taskSnapshot -> storageRef.getDownloadUrl()
                        .addOnSuccessListener(uri -> listener.onSuccess(uri.toString()))
                        .addOnFailureListener(e -> listener.onSuccess(null)))
                .addOnFailureListener(e -> listener.onSuccess(null));
    }

    private void createJobInFirestore(String title, String category, String startDate, String location, String imageUrl, String requirements, int bidLimit) {
        Job job = new Job();
        job.setContractorId(contractorId);
        job.setTitle(title);
        job.setCategory(category);
        job.setStartDate(startDate);
        job.setLocation(location);
        job.setImageUrl(imageUrl);
        job.setRequirements(requirements);
        job.setBidLimit(bidLimit);
        job.setStatus("active");

        firestoreService.createJob(job, jobId -> {
            binding.progressBar.setVisibility(View.GONE);
            binding.saveButton.setEnabled(true);

            if (jobId != null) {
                Toast.makeText(this, "Job created successfully!", Toast.LENGTH_SHORT).show();
                setResult(RESULT_OK);
                finish();
            } else {
                Toast.makeText(this, "Error creating job", Toast.LENGTH_SHORT).show();
            }
        });
    }

    interface OnImageUploadListener {
        void onSuccess(String imageUrl);
    }
}
