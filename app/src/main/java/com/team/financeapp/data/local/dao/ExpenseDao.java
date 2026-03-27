package com.team.financeapp.data.local.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.team.financeapp.data.local.entity.ExpenseEntity;

import java.util.List;

@Dao
public interface ExpenseDao {

    @Query("SELECT * FROM expenses WHERE userId = :userId AND deleted = 0 ORDER BY date DESC")
    List<ExpenseEntity> getByUser(String userId);

    @Query("SELECT * FROM expenses WHERE userId = :userId AND syncState != 'SYNCED'")
    List<ExpenseEntity> getPendingSync(String userId);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(ExpenseEntity entity);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<ExpenseEntity> entities);

    @Query("DELETE FROM expenses WHERE userId = :userId")
    void deleteAllForUser(String userId);

    @Update
    void update(ExpenseEntity entity);

    @Query("DELETE FROM expenses WHERE localId = :localId")
    void deleteByLocalId(long localId);
}
