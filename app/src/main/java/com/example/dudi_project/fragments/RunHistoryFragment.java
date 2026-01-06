package com.example.dudi_project.fragments;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.dudi_project.R;
import com.example.dudi_project.data.AppDatabase;
import com.example.dudi_project.data.RunAdapter;

public class RunHistoryFragment extends Fragment {

    public interface HistoryListener {
        void onViewRunAnalysis(int runId);
    }

    private HistoryListener listener;
    private RecyclerView recyclerView;
    private RunAdapter adapter;
    private TextView tvNoData;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof HistoryListener) {
            listener = (HistoryListener) context;
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_run_history, container, false);

        recyclerView = view.findViewById(R.id.rv_history);
        tvNoData = view.findViewById(R.id.tv_no_data);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new RunAdapter();
        recyclerView.setAdapter(adapter);

        // חיבור הלחיצה על פריט למעבר למסך הניתוח
        adapter.setOnItemClickListener(run -> {
            if (listener != null) {
                listener.onViewRunAnalysis(run.getId());
            }
        });

        loadRuns();

        return view;
    }

    private void loadRuns() {
        AppDatabase.getInstance(requireContext()).runDao().getAllRuns().observe(getViewLifecycleOwner(), runs -> {
            if (runs == null || runs.isEmpty()) {
                tvNoData.setVisibility(View.VISIBLE);
                recyclerView.setVisibility(View.GONE);
            } else {
                tvNoData.setVisibility(View.GONE);
                recyclerView.setVisibility(View.VISIBLE);
                adapter.setRuns(runs);
            }
        });
    }
}