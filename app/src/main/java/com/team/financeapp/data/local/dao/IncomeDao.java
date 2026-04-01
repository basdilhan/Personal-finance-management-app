package com.team.financeapp.data.local.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.team.financeapp.data.local.entity.IncomeEntity;

import java.util.List;

@Dao
public interface IncomeDao {

    @Query("SELECT * FROM incomes WHERE userId = :userId AND deleted = 0 ORDER BY date DESC")
    List<IncomeEntity> getByUser(String userId);

    @Query("SELECT * FROM incomes WHERE userId = :userId AND syncState != 'SYNCED'")
    List<IncomeEntity> getPendingSync(String userId);

    @Query("SELECT * FROM incomes WHERE localId = :localId LIMIT 1")
    IncomeEntity getByLocalId(long localId);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(IncomeEntity entity);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<IncomeEntity> entities);

    @Query("DELETE FROM incomes WHERE userId = :userId")
    void deleteAllForUser(String userId);

    @Update
    void update(IncomeEntity entity);

    @Query("DELETE FROM incomes WHERE localId = :localId")
    void deleteByLocalId(long localId);
}