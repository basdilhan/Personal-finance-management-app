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

    @Query("SELECT * FROM expenses WHERE localId = :localId LIMIT 1")
    ExpenseEntity getByLocalId(long localId);

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

    @Query("SELECT SUM(amount) FROM expenses WHERE userId = :userId AND category = :category AND date >= :startOfMonth AND date <= :endOfMonth AND deleted = 0")
    Double getCategoryTotalForMonth(String userId, String category, long startOfMonth, long endOfMonth);

    @Query("SELECT SUM(amount) FROM expenses WHERE userId = :userId AND date >= :start AND date <= :end AND deleted = 0")
    Double getTotalForRange(String userId, long start, long end);

    @Query("SELECT category, SUM(amount) as total FROM expenses WHERE userId = :userId AND date >= :start AND date <= :end AND deleted = 0 GROUP BY category")
    List<CategoryTotal> getCategoryTotalsForRange(String userId, long start, long end);
}
