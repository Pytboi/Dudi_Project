package com.example.dudi_project.data;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface RunDao {

    @Insert
    void insert(Run run);

    @Query("SELECT * FROM runs_table ORDER BY timestamp DESC")
    LiveData<List<Run>> getAllRuns();

    @Query("SELECT * FROM runs_table WHERE id = :runId")
    Run getRunById(int runId);
}