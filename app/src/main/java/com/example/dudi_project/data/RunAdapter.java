package com.example.dudi_project.data;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.dudi_project.R;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class RunAdapter extends RecyclerView.Adapter<RunAdapter.RunViewHolder> {

    public interface OnItemClickListener {
        void onItemClick(Run run);
    }

    private List<Run> runs = new ArrayList<>();
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault());
    private OnItemClickListener listener;

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public RunViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_run, parent, false);
        return new RunViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull RunViewHolder holder, int position) {
        Run currentRun = runs.get(position);

        holder.tvDate.setText(dateFormat.format(new Date(currentRun.getTimestamp())).toUpperCase());
        holder.tvDistance.setText(String.format(Locale.getDefault(), "%.2f km", currentRun.getDistance() / 1000f));
        
        long millis = currentRun.getDurationMillis();
        int seconds = (int) (millis / 1000) % 60;
        int minutes = (int) (millis / (1000 * 60));
        holder.tvTime.setText(String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds));

        int paceMin = (int) (currentRun.getAveragePace() / 60);
        int paceSec = (int) (currentRun.getAveragePace() % 60);
        holder.tvPace.setText(String.format(Locale.getDefault(), "%d:%02d", paceMin, paceSec));

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(currentRun);
            }
        });
    }

    @Override
    public int getItemCount() {
        return runs.size();
    }

    public void setRuns(List<Run> runs) {
        this.runs = runs;
        notifyDataSetChanged();
    }

    class RunViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvDate, tvDistance, tvTime, tvPace;

        public RunViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDate = itemView.findViewById(R.id.tv_item_date);
            tvDistance = itemView.findViewById(R.id.tv_item_distance);
            tvTime = itemView.findViewById(R.id.tv_item_time);
            tvPace = itemView.findViewById(R.id.tv_item_pace);
        }
    }
}