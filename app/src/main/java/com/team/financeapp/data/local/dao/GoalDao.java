package com.team.financeapp.data.local.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.team.financeapp.data.local.entity.GoalEntity;

import java.util.List;

@Dao
public interface GoalDao {

    @Query("SELECT * FROM goals WHERE userId = :userId AND deleted = 0 ORDER BY targetDate ASC")
    List<GoalEntity> getByUser(String userId);

    @Query("SELECT * FROM goals WHERE userId = :userId AND syncState != 'SYNCED'")
    List<GoalEntity> getPendingSync(String userId);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(GoalEntity entity);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<GoalEntity> entities);

    @Update
    void update(GoalEntity entity);

    @Query("DELETE FROM goals WHERE localId = :localId")
    void deleteByLocalId(long localId);
}
