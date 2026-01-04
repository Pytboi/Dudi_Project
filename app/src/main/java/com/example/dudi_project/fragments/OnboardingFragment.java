package com.example.dudi_project.fragments;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.example.dudi_project.R;

public class OnboardingFragment extends Fragment {

    public interface OnboardingListener {
        void onOnboardingComplete();
    }

    private OnboardingListener listener;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof OnboardingListener) {
            listener = (OnboardingListener) context;
        } else {
            throw new RuntimeException("Activity must implement OnboardingListener");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_onboarding, container, false);

        Button btnFinish = view.findViewById(R.id.btn_finish_onboarding);
        btnFinish.setOnClickListener(v -> listener.onOnboardingComplete());

        return view;
    }
}