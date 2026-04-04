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

    @Query("SELECT * FROM goals WHERE userId = :userId ORDER BY targetDate ASC")
    List<GoalEntity> getAllByUser(String userId);

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

    @Query("SELECT * FROM goals WHERE localId = :localId")
    GoalEntity getById(long localId);

    @Query("SELECT * FROM goals WHERE remoteId = :remoteId LIMIT 1")
    GoalEntity getByRemoteId(String remoteId);

    @Query("DELETE FROM goals WHERE userId = :userId")
    void deleteAllForUser(String userId);
}
