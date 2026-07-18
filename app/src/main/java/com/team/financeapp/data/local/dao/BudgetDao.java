package com.team.financeapp.data.local.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.team.financeapp.data.local.entity.BudgetLimitEntity;

import java.util.List;

@Dao
public interface BudgetDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(BudgetLimitEntity budgetLimit);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<BudgetLimitEntity> budgetLimits);

    @Update
    void update(BudgetLimitEntity budgetLimit);

    @Query("SELECT * FROM budget_limits WHERE user_id = :userId AND month_year = :monthYear")
    List<BudgetLimitEntity> getByUserAndMonth(String userId, String monthYear);

    @Query("SELECT * FROM budget_limits WHERE user_id = :userId AND category = :category AND month_year = :monthYear LIMIT 1")
    BudgetLimitEntity getByCategoryAndMonth(String userId, String category, String monthYear);

    @Query("DELETE FROM budget_limits WHERE user_id = :userId")
    void deleteAllForUser(String userId);

    @Query("DELETE FROM budget_limits WHERE user_id = :userId AND category = :category AND month_year = :monthYear")
    void deleteByCategoryAndMonth(String userId, String category, String monthYear);
}
