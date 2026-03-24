package com.example.dudi_project;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
            
            if (currentUser != null) {
                // בדיקה אם המשתמש כבר סיים את הגדרת הפרופיל שלו ב-Firestore
                FirebaseFirestore.getInstance().collection("users").document(currentUser.getUid()).get()
                        .addOnCompleteListener(task -> {
                            Intent intent;
                            if (task.isSuccessful() && task.getResult().exists()) {
                                intent = new Intent(SplashActivity.this, MainActivity.class);
                            } else {
                                intent = new Intent(SplashActivity.this, UserSetupActivity.class);
                            }
                            startActivity(intent);
                            finish();
                        });
            } else {
                Intent intent = new Intent(SplashActivity.this, LoginActivity.class);
                startActivity(intent);
                finish();
            }
        }, 2500);
    }
}