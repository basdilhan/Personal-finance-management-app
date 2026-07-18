package com.team.financeapp.data.repository;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

import com.team.financeapp.Expense;
import com.team.financeapp.data.local.AppDatabase;
import com.team.financeapp.data.local.SyncState;
import com.team.financeapp.data.local.dao.ExpenseDao;
import com.team.financeapp.data.local.entity.ExpenseEntity;
import com.team.financeapp.data.remote.ApiClient;
import com.team.financeapp.data.remote.ExpenseApiService;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ExpenseRepository {

    public interface LoadExpensesCallback {
        void onExpensesLoaded(List<Expense> expenses);
        void onError(String message);
    }

    public interface SaveExpenseCallback {
        void onSuccess();
        void onError(String message);
    }

    public interface ModifyExpenseCallback {
        void onSuccess();
        void onError(String message);
    }

    private static final ExecutorService IO = Executors.newSingleThreadExecutor();

    private final ExpenseDao expenseDao;
    private final ExpenseApiService apiService;
    private final Handler mainHandler;

    public ExpenseRepository(@NonNull Context context) {
        this.expenseDao = AppDatabase.getInstance(context).expenseDao();
        this.apiService = ApiClient.getClient().create(ExpenseApiService.class);
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    public void loadExpenses(@NonNull String userId, @NonNull LoadExpensesCallback callback) {
        // 1. Load from local Room database first (offline-first)
        IO.execute(() -> {
            List<ExpenseEntity> localEntities = expenseDao.getByUser(userId);
            List<Expense> localExpenses = toExpenses(localEntities);
            mainHandler.post(() -> callback.onExpensesLoaded(localExpenses));
        });

        // 2. Fetch fresh data from Spring Boot REST API
        refreshFromRemote(userId, callback);
    }

    public void saveExpense(@NonNull String userId, @NonNull Expense expense, @NonNull SaveExpenseCallback callback) {
        ExpenseEntity entity = fromExpense(userId, expense);
        entity.remoteId = UUID.randomUUID().toString(); // Use as temp remote ID until real one arrives
        entity.syncState = SyncState.PENDING;
        entity.createdAt = System.currentTimeMillis();
        entity.updatedAt = entity.createdAt;

        IO.execute(() -> {
            // Save locally immediately
            long localId = expenseDao.insert(entity);
            entity.localId = localId;
            mainHandler.post(callback::onSuccess);
            
            // Push to Spring Boot backend
            pushExpenseToRemote(entity, callback);
        });
    }

    public void updateExpense(@NonNull String userId, @NonNull Expense expense, @NonNull ModifyExpenseCallback callback) {
        IO.execute(() -> {
            ExpenseEntity existing = expenseDao.getByLocalId(expense.getId());
            if (existing == null || !userId.equals(existing.userId)) {
                mainHandler.post(() -> callback.onError("Expense not found"));
                return;
            }

            existing.category = expense.getCategory();
            existing.amount = expense.getAmount();
            existing.description = expense.getDescription();
            existing.date = expense.getDate();
            existing.time = expense.getTime();
            existing.categoryIcon = expense.getCategoryIcon();
            existing.deleted = false;
            existing.syncState = SyncState.PENDING;
            existing.updatedAt = System.currentTimeMillis();

            // Update locally immediately
            expenseDao.update(existing);
            mainHandler.post(callback::onSuccess);
            
            // Update in Spring Boot backend
            updateExpenseInRemote(existing, callback);
        });
    }

    public void deleteExpense(@NonNull String userId, int expenseId, @NonNull ModifyExpenseCallback callback) {
        IO.execute(() -> {
            ExpenseEntity existing = expenseDao.getByLocalId(expenseId);
            if (existing == null || !userId.equals(existing.userId)) {
                mainHandler.post(() -> callback.onError("Expense not found"));
                return;
            }

            existing.deleted = true;
            existing.syncState = SyncState.PENDING;
            existing.updatedAt = System.currentTimeMillis();
            
            // Soft delete locally
            expenseDao.update(existing);
            mainHandler.post(callback::onSuccess);
            
            // Delete in Spring Boot backend
            deleteExpenseInRemote(existing, callback);
        });
    }

    private void refreshFromRemote(@NonNull String userId, @NonNull LoadExpensesCallback callback) {
        apiService.getExpenses().enqueue(new Callback<List<ExpenseEntity>>() {
            @Override
            public void onResponse(Call<List<ExpenseEntity>> call, Response<List<ExpenseEntity>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<ExpenseEntity> remoteEntities = response.body();
                    IO.execute(() -> {
                        // Mark all as SYNCED since they came from the server
                        for (ExpenseEntity entity : remoteEntities) {
                            entity.syncState = SyncState.SYNCED;
                            // Spring boot sends integer IDs, but our local expects remoteId to be a string
                            // If your backend ID is Integer, map it to remoteId
                            entity.remoteId = String.valueOf(entity.localId); // Mapping backend ID if needed
                        }
                        
                        expenseDao.deleteAllForUser(userId);
                        expenseDao.insertAll(remoteEntities);
                        List<Expense> latest = toExpenses(expenseDao.getByUser(userId));
                        mainHandler.post(() -> callback.onExpensesLoaded(latest));
                    });
                } else {
                    mainHandler.post(() -> callback.onError("Failed to refresh: HTTP " + response.code()));
                }
            }

            @Override
            public void onFailure(Call<List<ExpenseEntity>> call, Throwable t) {
                mainHandler.post(() -> callback.onError(t.getMessage() == null ? "Network error" : t.getMessage()));
            }
        });
    }

    private void pushExpenseToRemote(@NonNull ExpenseEntity entity, @NonNull SaveExpenseCallback callback) {
        apiService.createExpense(entity).enqueue(new Callback<ExpenseEntity>() {
            @Override
            public void onResponse(Call<ExpenseEntity> call, Response<ExpenseEntity> response) {
                if (response.isSuccessful() && response.body() != null) {
                    IO.execute(() -> {
                        // ✅ CRITICAL: Save the real backend ID so edit/delete work correctly
                        entity.remoteId = String.valueOf(response.body().localId);
                        entity.syncState = SyncState.SYNCED;
                        entity.updatedAt = System.currentTimeMillis();
                        expenseDao.update(entity);
                    });
                } else {
                    handleSyncFailure(entity, callback, "HTTP " + response.code());
                }
            }

            @Override
            public void onFailure(Call<ExpenseEntity> call, Throwable t) {
                handleSyncFailure(entity, callback, t.getMessage());
            }
        });
    }

    private void updateExpenseInRemote(@NonNull ExpenseEntity entity, @NonNull ModifyExpenseCallback callback) {
        // Extract the integer ID from remoteId if available, or just send the entity
        int backendId = 0;
        try {
            backendId = Integer.parseInt(entity.remoteId);
        } catch (NumberFormatException ignored) {}

        apiService.updateExpense(backendId, entity).enqueue(new Callback<ExpenseEntity>() {
            @Override
            public void onResponse(Call<ExpenseEntity> call, Response<ExpenseEntity> response) {
                if (response.isSuccessful()) {
                    IO.execute(() -> {
                        entity.syncState = SyncState.SYNCED;
                        entity.updatedAt = System.currentTimeMillis();
                        expenseDao.update(entity);
                    });
                } else {
                    handleSyncFailure(entity, callback, "HTTP " + response.code());
                }
            }

            @Override
            public void onFailure(Call<ExpenseEntity> call, Throwable t) {
                handleSyncFailure(entity, callback, t.getMessage());
            }
        });
    }

    private void deleteExpenseInRemote(@NonNull ExpenseEntity entity, @NonNull ModifyExpenseCallback callback) {
        int backendId = 0;
        try {
            backendId = Integer.parseInt(entity.remoteId);
        } catch (NumberFormatException ignored) {}

        apiService.deleteExpense(backendId).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    IO.execute(() -> {
                        entity.syncState = SyncState.SYNCED;
                        entity.updatedAt = System.currentTimeMillis();
                        expenseDao.update(entity);
                    });
                } else {
                    handleSyncFailure(entity, callback, "HTTP " + response.code());
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                handleSyncFailure(entity, callback, t.getMessage());
            }
        });
    }

    private void handleSyncFailure(ExpenseEntity entity, Object callback, String errorMsg) {
        IO.execute(() -> {
            entity.syncState = SyncState.FAILED;
            entity.updatedAt = System.currentTimeMillis();
            expenseDao.update(entity);
            
            String msg = errorMsg == null ? "Cloud sync failed" : errorMsg;
            if (callback instanceof SaveExpenseCallback) {
                mainHandler.post(() -> ((SaveExpenseCallback) callback).onError(msg));
            } else if (callback instanceof ModifyExpenseCallback) {
                mainHandler.post(() -> ((ModifyExpenseCallback) callback).onError(msg));
            }
        });
    }

    private ExpenseEntity fromExpense(String userId, Expense expense) {
        ExpenseEntity entity = new ExpenseEntity();
        entity.userId = userId;
        entity.category = expense.getCategory();
        entity.amount = expense.getAmount();
        entity.description = expense.getDescription();
        entity.date = expense.getDate();
        entity.time = expense.getTime();
        entity.categoryIcon = expense.getCategoryIcon();
        entity.deleted = false;
        return entity;
    }

    private List<Expense> toExpenses(List<ExpenseEntity> entities) {
        List<Expense> expenses = new ArrayList<>();
        for (ExpenseEntity entity : entities) {
            // Only add non-deleted items to UI
            if (!entity.deleted) {
                expenses.add(new Expense(
                        (int) entity.localId,
                        entity.category,
                        entity.amount,
                        entity.description,
                        entity.date,
                        entity.time,
                        entity.categoryIcon
                ));
            }
        }
        return expenses;
    }
}