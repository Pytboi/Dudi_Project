package com.example.dudi_project.fragments;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.example.dudi_project.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

public class ProfileFragment extends Fragment {

    private ImageView ivProfilePicture;
    private TextInputEditText etName;
    private ProgressBar progressBar;
    private Uri selectedImageUri;
    private FirebaseAuth mAuth;
    private FirebaseStorage storage;

    private final ActivityResultLauncher<Intent> pickImageLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    selectedImageUri = result.getData().getData();
                    ivProfilePicture.setImageURI(selectedImageUri);
                }
            }
    );

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        mAuth = FirebaseAuth.getInstance();
        storage = FirebaseStorage.getInstance();

        ivProfilePicture = view.findViewById(R.id.iv_profile_picture);
        etName = view.findViewById(R.id.et_profile_name);
        progressBar = view.findViewById(R.id.pb_profile_loading);
        ImageButton btnBack = view.findViewById(R.id.btn_profile_back);
        MaterialButton btnChangePhoto = view.findViewById(R.id.btn_change_photo);
        MaterialButton btnSave = view.findViewById(R.id.btn_save_profile);

        loadUserData();

        btnBack.setOnClickListener(v -> requireActivity().getOnBackPressedDispatcher().onBackPressed());
        btnChangePhoto.setOnClickListener(v -> pickImage());
        btnSave.setOnClickListener(v -> saveProfile());

        return view;
    }

    private void loadUserData() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            etName.setText(user.getDisplayName());
            if (user.getPhotoUrl() != null) {
                Glide.with(this).load(user.getPhotoUrl()).into(ivProfilePicture);
            }
        }
    }

    private void pickImage() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        pickImageLauncher.launch(intent);
    }

    private void saveProfile() {
        String newName = etName.getText().toString().trim();
        if (newName.isEmpty()) {
            Toast.makeText(getContext(), "Name cannot be empty", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);

        if (selectedImageUri != null) {
            uploadImageAndSaveProfile(newName);
        } else {
            updateProfile(newName, null);
        }
    }

    private void uploadImageAndSaveProfile(String name) {
        StorageReference storageRef = storage.getReference().child("profile_pictures/" + mAuth.getUid() + ".jpg");
        storageRef.putFile(selectedImageUri)
                .addOnSuccessListener(taskSnapshot -> storageRef.getDownloadUrl().addOnSuccessListener(uri -> updateProfile(name, uri)))
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(getContext(), "Failed to upload image", Toast.LENGTH_SHORT).show();
                });
    }

    private void updateProfile(String name, Uri photoUri) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            UserProfileChangeRequest.Builder builder = new UserProfileChangeRequest.Builder().setDisplayName(name);
            if (photoUri != null) {
                builder.setPhotoUri(photoUri);
            }
            UserProfileChangeRequest profileUpdates = builder.build();

            user.updateProfile(profileUpdates)
                    .addOnCompleteListener(task -> {
                        progressBar.setVisibility(View.GONE);
                        if (task.isSuccessful()) {
                            Toast.makeText(getContext(), "Profile updated!", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(getContext(), "Update failed", Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }
}