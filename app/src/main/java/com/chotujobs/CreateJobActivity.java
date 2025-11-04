package com.chotujobs;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.annotation.Nullable;
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

    private static final int PICK_IMAGE_REQUEST = 103;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCreateJobBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        firestoreService = FirestoreService.getInstance();
        contractorId = getSharedPreferences("chotujobs_prefs", 0).getString("user_id", "");
        calendar = Calendar.getInstance();

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
                this, android.R.layout.simple_spinner_item, categories);
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
            startActivityForResult(intent, PICK_IMAGE_REQUEST);
        });

        binding.saveButton.setOnClickListener(v -> saveJob());
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK && data != null) {
            if (requestCode == PICK_IMAGE_REQUEST) {
                selectedImageUri = data.getData();
                binding.imageView.setImageURI(selectedImageUri);
                binding.imageView.setVisibility(View.VISIBLE);
            }
        }
    }

    private void saveJob() {
        String title = binding.titleEditText.getText().toString().trim();
        String category = binding.categorySpinner.getSelectedItem().toString();
        String startDate = binding.startDateEditText.getText().toString().trim();
        String location = binding.locationEditText.getText().toString().trim();

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

        binding.progressBar.setVisibility(View.VISIBLE);
        binding.saveButton.setEnabled(false);

        if (selectedImageUri != null) {
            uploadImage(selectedImageUri, (imageUrl) -> {
                createJobInFirestore(title, category, startDate, location, imageUrl);
            });
        } else {
            createJobInFirestore(title, category, startDate, location, null);
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

    private void createJobInFirestore(String title, String category, String startDate, String location, String imageUrl) {
        Job job = new Job();
        job.setContractorId(contractorId);
        job.setTitle(title);
        job.setCategory(category);
        job.setStartDate(startDate);
        job.setLocation(location);
        job.setImageUrl(imageUrl);
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
