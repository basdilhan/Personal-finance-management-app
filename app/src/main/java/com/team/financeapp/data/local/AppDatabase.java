package com.team.financeapp.data.local;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.team.financeapp.data.local.dao.BillDao;
import com.team.financeapp.data.local.dao.ExpenseDao;
import com.team.financeapp.data.local.dao.GoalDao;
import com.team.financeapp.data.local.dao.IncomeDao;
import com.team.financeapp.data.local.entity.BillEntity;
import com.team.financeapp.data.local.entity.ExpenseEntity;
import com.team.financeapp.data.local.entity.GoalEntity;
import com.team.financeapp.data.local.entity.IncomeEntity;

@Database(
        entities = {BillEntity.class, ExpenseEntity.class, GoalEntity.class, IncomeEntity.class},
        version = 2,
        exportSchema = false
)
public abstract class AppDatabase extends RoomDatabase {

    private static final String DB_NAME = "finance_local.db";
    private static volatile AppDatabase INSTANCE;

    public abstract BillDao billDao();

    public abstract ExpenseDao expenseDao();

    public abstract GoalDao goalDao();

    public abstract IncomeDao incomeDao();

    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    AppDatabase.class,
                                    DB_NAME
                            )
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}
