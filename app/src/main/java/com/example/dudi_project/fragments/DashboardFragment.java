package com.example.dudi_project.fragments;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.example.dudi_project.R;

public class DashboardFragment extends Fragment {

    public interface DashboardListener {
        void onStartRun();
    }

    private DashboardListener listener;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof DashboardListener) {
            listener = (DashboardListener) context;
        } else {
            throw new RuntimeException(context.toString() + " must implement DashboardListener");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_dashboard, container, false);

        TextView tvWelcome = view.findViewById(R.id.tv_welcome);
        // Reset to default text since we removed Firebase
        if (tvWelcome != null) {
            tvWelcome.setText("LACE UP.");
        }

        Button btnStartRun = view.findViewById(R.id.btn_start_run);
        if (btnStartRun != null) {
            btnStartRun.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onStartRun();
                }
            });
        }

        return view;
    }
}