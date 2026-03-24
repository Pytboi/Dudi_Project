package com.example.dudi_project;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.DatePicker;
import android.widget.NumberPicker;
import android.widget.ProgressBar;
import android.widget.RadioGroup;
import android.widget.Toast;
import android.widget.ViewFlipper;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class UserSetupActivity extends AppCompatActivity {

    private ViewFlipper viewFlipper;
    private ProgressBar progressBar;
    private MaterialButton btnNext;
    
    private TextInputEditText etName;
    private RadioGroup rgGender, rgFitness;
    private DatePicker dpBirthdate;
    private NumberPicker npHeight, npWeight;

    private int currentStep = 0;
    private final int TOTAL_STEPS = 6;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_setup);

        viewFlipper = findViewById(R.id.vf_setup);
        progressBar = findViewById(R.id.setup_progress);
        btnNext = findViewById(R.id.btn_next);

        etName = findViewById(R.id.et_setup_name);
        rgGender = findViewById(R.id.rg_gender);
        dpBirthdate = findViewById(R.id.dp_birthdate);
        npHeight = findViewById(R.id.np_height);
        npWeight = findViewById(R.id.np_weight);
        rgFitness = findViewById(R.id.rg_fitness);

        setupPickers();

        btnNext.setOnClickListener(v -> handleNextStep());
    }

    private void setupPickers() {
        npHeight.setMinValue(100);
        npHeight.setMaxValue(250);
        npHeight.setValue(175);

        npWeight.setMinValue(30);
        npWeight.setMaxValue(200);
        npWeight.setValue(70);
    }

    private void handleNextStep() {
        if (currentStep < TOTAL_STEPS - 1) {
            currentStep++;
            viewFlipper.showNext();
            progressBar.setProgress(currentStep + 1);
            if (currentStep == TOTAL_STEPS - 1) {
                btnNext.setText("סיים והתחל");
            }
        } else {
            saveUserDataToFirestore();
        }
    }

    private void saveUserDataToFirestore() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "User not authenticated!", Toast.LENGTH_SHORT).show();
            return;
        }

        btnNext.setEnabled(false); // מניעת לחיצות כפולות
        Toast.makeText(this, "Saving your profile...", Toast.LENGTH_SHORT).show();

        Map<String, Object> userData = new HashMap<>();
        userData.put("nickname", etName.getText().toString());
        
        int selectedGenderId = rgGender.getCheckedRadioButtonId();
        String gender = "Other";
        if (selectedGenderId == R.id.rb_male) gender = "Male";
        else if (selectedGenderId == R.id.rb_female) gender = "Female";
        userData.put("gender", gender);

        String birthDate = dpBirthdate.getDayOfMonth() + "/" + (dpBirthdate.getMonth() + 1) + "/" + dpBirthdate.getYear();
        userData.put("birthDate", birthDate);
        userData.put("height", npHeight.getValue());
        userData.put("weight", npWeight.getValue());

        int selectedFitnessId = rgFitness.getCheckedRadioButtonId();
        String fitness = "Beginner";
        if (selectedFitnessId == R.id.level_intermediate) fitness = "Intermediate";
        else if (selectedFitnessId == R.id.level_advanced) fitness = "Advanced";
        else if (selectedFitnessId == R.id.level_pro) fitness = "Pro";
        userData.put("fitnessLevel", fitness);

        FirebaseFirestore.getInstance().collection("users").document(user.getUid())
                .set(userData)
                .addOnSuccessListener(aVoid -> {
                    startActivity(new Intent(UserSetupActivity.this, MainActivity.class));
                    finish();
                })
                .addOnFailureListener(e -> {
                    btnNext.setEnabled(true);
                    Log.e("UserSetup", "Firestore Error: ", e);
                    Toast.makeText(UserSetupActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }
}