package com.team.financeapp.data.repository;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

import com.team.financeapp.BudgetProgress;
import com.team.financeapp.data.local.AppDatabase;
import com.team.financeapp.data.local.SyncState;
import com.team.financeapp.data.local.dao.BudgetDao;
import com.team.financeapp.data.local.dao.ExpenseDao;
import com.team.financeapp.data.local.entity.BudgetLimitEntity;
import com.team.financeapp.data.remote.ApiClient;
import com.team.financeapp.data.remote.BudgetApiService;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class BudgetRepository {

    public interface LoadBudgetsCallback {
        void onBudgetsLoaded(List<BudgetProgress> budgets);
        void onError(String message);
    }

    public interface SaveBudgetCallback {
        void onSuccess();
        void onError(String message);
    }

    private static final ExecutorService IO = Executors.newSingleThreadExecutor();

    private final BudgetDao budgetDao;
    private final ExpenseDao expenseDao;
    private final BudgetApiService apiService;
    private final Handler mainHandler;

    public BudgetRepository(@NonNull Context context) {
        AppDatabase db = AppDatabase.getInstance(context);
        this.budgetDao = db.budgetDao();
        this.expenseDao = db.expenseDao();
        this.apiService = ApiClient.getClient().create(BudgetApiService.class);
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    public void loadBudgetsForMonth(@NonNull String userId, @NonNull String monthYear, @NonNull LoadBudgetsCallback callback) {
        IO.execute(() -> {
            List<BudgetLimitEntity> localLimits = budgetDao.getByUserAndMonth(userId, monthYear);
            List<BudgetProgress> progressList = calculateProgress(userId, localLimits, monthYear);
            mainHandler.post(() -> callback.onBudgetsLoaded(progressList));
        });

        refreshFromRemote(userId, monthYear, callback);
    }

    public void saveBudgetLimit(@NonNull String userId, @NonNull String category, double limitAmount, @NonNull String monthYear, @NonNull SaveBudgetCallback callback) {
        IO.execute(() -> {
            BudgetLimitEntity entity = budgetDao.getByCategoryAndMonth(userId, category, monthYear);
            if (entity == null) {
                entity = new BudgetLimitEntity();
                entity.userId = userId;
                entity.category = category;
                entity.monthYear = monthYear;
                entity.remoteId = UUID.randomUUID().toString();
            }
            entity.limitAmount = limitAmount;
            entity.syncState = SyncState.PENDING;
            entity.updatedAt = System.currentTimeMillis();

            long localId = budgetDao.insert(entity);
            if (entity.localId == 0) entity.localId = localId; // Assign if it was a new insert
            
            mainHandler.post(callback::onSuccess);
            pushBudgetToRemote(entity, callback);
        });
    }

    public void deleteBudgetLimit(@NonNull String userId, @NonNull BudgetLimitEntity entity, @NonNull SaveBudgetCallback callback) {
        IO.execute(() -> {
            budgetDao.deleteByCategoryAndMonth(userId, entity.category, entity.monthYear);
            mainHandler.post(callback::onSuccess);

            int backendId = 0;
            try {
                backendId = Integer.parseInt(entity.remoteId);
            } catch (NumberFormatException ignored) {}

            if (backendId > 0) {
                apiService.deleteBudget(backendId).enqueue(new Callback<Void>() {
                    @Override
                    public void onResponse(Call<Void> call, Response<Void> response) {}

                    @Override
                    public void onFailure(Call<Void> call, Throwable t) {}
                });
            }
        });
    }

    private void refreshFromRemote(@NonNull String userId, @NonNull String monthYear, @NonNull LoadBudgetsCallback callback) {
        apiService.getBudgets(monthYear).enqueue(new Callback<List<BudgetLimitEntity>>() {
            @Override
            public void onResponse(Call<List<BudgetLimitEntity>> call, Response<List<BudgetLimitEntity>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<BudgetLimitEntity> remote = response.body();
                    IO.execute(() -> {
                        for (BudgetLimitEntity entity : remote) {
                            entity.syncState = SyncState.SYNCED;
                            entity.remoteId = String.valueOf(entity.localId);
                        }
                        
                        // We only want to replace for this specific monthYear and user
                        // But since the API returns limits for a specific month, we can clear local limits for this month and re-insert
                        List<BudgetLimitEntity> localLimits = budgetDao.getByUserAndMonth(userId, monthYear);
                        for (BudgetLimitEntity local : localLimits) {
                             budgetDao.deleteByCategoryAndMonth(userId, local.category, monthYear);
                        }
                        
                        budgetDao.insertAll(remote);
                        
                        List<BudgetLimitEntity> latest = budgetDao.getByUserAndMonth(userId, monthYear);
                        List<BudgetProgress> progressList = calculateProgress(userId, latest, monthYear);
                        mainHandler.post(() -> callback.onBudgetsLoaded(progressList));
                    });
                }
            }

            @Override
            public void onFailure(Call<List<BudgetLimitEntity>> call, Throwable t) {
                mainHandler.post(() -> callback.onError(t.getMessage() == null ? "Network error" : t.getMessage()));
            }
        });
    }

    private void pushBudgetToRemote(@NonNull BudgetLimitEntity entity, @NonNull SaveBudgetCallback callback) {
        apiService.createOrUpdateBudget(entity).enqueue(new Callback<BudgetLimitEntity>() {
            @Override
            public void onResponse(Call<BudgetLimitEntity> call, Response<BudgetLimitEntity> response) {
                if (response.isSuccessful() && response.body() != null) {
                    IO.execute(() -> {
                        entity.remoteId = String.valueOf(response.body().localId);
                        entity.syncState = SyncState.SYNCED;
                        entity.updatedAt = System.currentTimeMillis();
                        budgetDao.update(entity);
                    });
                }
            }

            @Override
            public void onFailure(Call<BudgetLimitEntity> call, Throwable t) {
                // Sync failed
            }
        });
    }

    private List<BudgetProgress> calculateProgress(String userId, List<BudgetLimitEntity> limits, String monthYear) {
        List<BudgetProgress> progressList = new ArrayList<>();
        
        // Parse monthYear "yyyy-MM" to get start and end timestamps
        String[] parts = monthYear.split("-");
        int year = Integer.parseInt(parts[0]);
        int month = Integer.parseInt(parts[1]) - 1; // Calendar months are 0-based

        Calendar cal = Calendar.getInstance();
        cal.set(year, month, 1, 0, 0, 0);
        cal.set(Calendar.MILLISECOND, 0);
        long startOfMonth = cal.getTimeInMillis();

        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH));
        cal.set(Calendar.HOUR_OF_DAY, 23);
        cal.set(Calendar.MINUTE, 59);
        cal.set(Calendar.SECOND, 59);
        long endOfMonth = cal.getTimeInMillis();

        for (BudgetLimitEntity limit : limits) {
            Double total = expenseDao.getCategoryTotalForMonth(userId, limit.category, startOfMonth, endOfMonth);
            double spent = total == null ? 0.0 : total;
            progressList.add(new BudgetProgress(limit, spent));
        }

        return progressList;
    }
}
