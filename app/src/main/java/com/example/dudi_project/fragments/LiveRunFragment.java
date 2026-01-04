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

public class LiveRunFragment extends Fragment {

    public interface LiveRunListener {
        void onRunFinished(); // שים לב: ב-Main קראנו לזה onRunFinished
    }

    private LiveRunListener listener;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof LiveRunListener) {
            listener = (LiveRunListener) context;
        } else {
            throw new RuntimeException("Activity must implement LiveRunListener");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_live_run, container, false);

        Button btnStop = view.findViewById(R.id.btn_stop_run);
        btnStop.setOnClickListener(v -> listener.onRunFinished());

        return view;
    }
}