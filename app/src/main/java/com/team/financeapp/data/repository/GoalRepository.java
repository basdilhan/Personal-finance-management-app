package com.team.financeapp.data.repository;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

import com.team.financeapp.Goal;
import com.team.financeapp.data.local.AppDatabase;
import com.team.financeapp.data.local.SyncState;
import com.team.financeapp.data.local.dao.GoalDao;
import com.team.financeapp.data.local.entity.GoalEntity;
import com.team.financeapp.data.remote.ApiClient;
import com.team.financeapp.data.remote.GoalApiService;
import com.team.financeapp.notifications.FinancialReminderScheduler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class GoalRepository {

    public interface LoadGoalsCallback {
        void onGoalsLoaded(List<Goal> goals);
        void onError(String message);
    }

    public interface SaveGoalCallback {
        void onSuccess(Goal goal);
        void onError(String message);
    }

    public interface UpdateGoalCallback {
        void onSuccess();
        void onError(String message);
    }

    public interface DeleteGoalCallback {
        void onSuccess();
        void onError(String message);
    }

    private static final ExecutorService IO = Executors.newSingleThreadExecutor();

    private final Context appContext;
    private final GoalDao goalDao;
    private final GoalApiService apiService;
    private final Handler mainHandler;

    public GoalRepository(@NonNull Context context) {
        this.appContext = context.getApplicationContext();
        this.goalDao = AppDatabase.getInstance(appContext).goalDao();
        this.apiService = ApiClient.getClient().create(GoalApiService.class);
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    public void loadGoals(@NonNull String userId, @NonNull LoadGoalsCallback callback) {
        IO.execute(() -> {
            List<GoalEntity> localEntities = goalDao.getByUser(userId);
            List<Goal> localGoals = toGoals(localEntities);
            mainHandler.post(() -> callback.onGoalsLoaded(localGoals));
        });

        refreshFromRemote(userId, callback);
    }

    public void saveGoal(@NonNull String userId, @NonNull Goal goal, @NonNull SaveGoalCallback callback) {
        GoalEntity entity = fromGoal(userId, goal);
        entity.remoteId = UUID.randomUUID().toString();
        entity.syncState = SyncState.PENDING;
        entity.createdAt = System.currentTimeMillis();
        entity.updatedAt = entity.createdAt;
        entity.deleted = false;

        IO.execute(() -> {
            long localId = goalDao.insert(entity);
            entity.localId = localId;
            FinancialReminderScheduler.scheduleGoalReminder(appContext, entity);
            FinancialReminderScheduler.scheduleGoalAddedReminder(appContext, entity);

            Goal savedGoal = new Goal(
                    (int) localId,
                    entity.name,
                    entity.description,
                    entity.targetAmount,
                    entity.currentAmount,
                    entity.addedSavingsAmount,
                    entity.targetDate,
                    entity.category,
                    entity.categoryIcon,
                    entity.progressCircleBackground
            );

            mainHandler.post(() -> callback.onSuccess(savedGoal));
            pushGoalToRemote(entity, new SaveGoalCallback() {
                @Override
                public void onSuccess(Goal goal) {}
                @Override
                public void onError(String message) {}
            });
        });
    }

    public void updateGoal(@NonNull String userId, @NonNull Goal goal, @NonNull UpdateGoalCallback callback) {
        IO.execute(() -> {
            GoalEntity existingEntity = goalDao.getById(goal.getId());

            if (existingEntity != null) {
                existingEntity.userId = userId;
                existingEntity.name = goal.getName();
                existingEntity.description = goal.getDescription();
                existingEntity.targetAmount = goal.getTargetAmount();
                existingEntity.currentAmount = goal.getCurrentAmount();
                existingEntity.targetDate = goal.getTargetDate();
                existingEntity.category = goal.getCategory();
                existingEntity.categoryIcon = goal.getCategoryIcon();
                existingEntity.progressCircleBackground = goal.getProgressCircleBackground();
                existingEntity.syncState = SyncState.PENDING;
                existingEntity.updatedAt = System.currentTimeMillis();
                existingEntity.deleted = false;

                goalDao.update(existingEntity);
                FinancialReminderScheduler.scheduleGoalReminder(appContext, existingEntity);
                mainHandler.post(callback::onSuccess);

                updateGoalInRemote(existingEntity, callback);
            } else {
                mainHandler.post(() -> callback.onError("Goal not found"));
            }
        });
    }

    public void addGoalSavings(@NonNull String userId, int goalLocalId, double amountToAdd, @NonNull UpdateGoalCallback callback) {
        IO.execute(() -> {
            GoalEntity existingEntity = goalDao.getById(goalLocalId);
            if (existingEntity == null || !userId.equals(existingEntity.userId)) {
                mainHandler.post(() -> callback.onError("Goal not found"));
                return;
            }

            existingEntity.currentAmount += amountToAdd;
            existingEntity.addedSavingsAmount += amountToAdd;
            existingEntity.syncState = SyncState.PENDING;
            existingEntity.updatedAt = System.currentTimeMillis();

            goalDao.update(existingEntity);
            FinancialReminderScheduler.scheduleGoalReminder(appContext, existingEntity);
            mainHandler.post(callback::onSuccess);

            // Use the PATCH endpoint for addSavings or just full update
            int backendId = 0;
            try {
                backendId = Integer.parseInt(existingEntity.remoteId);
            } catch (NumberFormatException ignored) {}

            Map<String, Object> body = new HashMap<>();
            body.put("amount", amountToAdd);
            
            apiService.addSavings(backendId, body).enqueue(new Callback<GoalEntity>() {
                @Override
                public void onResponse(Call<GoalEntity> call, Response<GoalEntity> response) {
                    if (response.isSuccessful()) {
                        IO.execute(() -> {
                            existingEntity.syncState = SyncState.SYNCED;
                            existingEntity.updatedAt = System.currentTimeMillis();
                            goalDao.update(existingEntity);
                        });
                    }
                }
                @Override
                public void onFailure(Call<GoalEntity> call, Throwable t) {}
            });
        });
    }

    public void deleteGoal(@NonNull String userId, int goalLocalId, @NonNull DeleteGoalCallback callback) {
        IO.execute(() -> {
            GoalEntity entity = goalDao.getById(goalLocalId);
            if (entity != null) {
                entity.deleted = true;
                entity.syncState = SyncState.PENDING;
                entity.updatedAt = System.currentTimeMillis();
                goalDao.update(entity);
                FinancialReminderScheduler.cancelGoalReminder(appContext, entity.remoteId);
                mainHandler.post(callback::onSuccess);

                deleteGoalInRemote(entity, callback);
            } else {
                mainHandler.post(() -> callback.onError("Goal not found"));
            }
        });
    }

    private void refreshFromRemote(@NonNull String userId, @NonNull LoadGoalsCallback callback) {
        apiService.getGoals().enqueue(new Callback<List<GoalEntity>>() {
            @Override
            public void onResponse(Call<List<GoalEntity>> call, Response<List<GoalEntity>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<GoalEntity> remoteEntities = response.body();
                    IO.execute(() -> {
                        for (GoalEntity remoteEntity : remoteEntities) {
                            remoteEntity.syncState = SyncState.SYNCED;
                            remoteEntity.remoteId = String.valueOf(remoteEntity.localId);
                            
                            GoalEntity localEntity = goalDao.getByRemoteId(remoteEntity.remoteId);
                            if (localEntity == null) {
                                goalDao.insert(remoteEntity);
                            } else {
                                remoteEntity.localId = localEntity.localId;
                                goalDao.update(remoteEntity);
                            }

                            FinancialReminderScheduler.scheduleGoalReminder(appContext, remoteEntity);
                        }

                        List<Goal> latest = toGoals(goalDao.getByUser(userId));
                        mainHandler.post(() -> callback.onGoalsLoaded(latest));
                    });
                } else {
                    mainHandler.post(() -> callback.onError("Failed to refresh goals: HTTP " + response.code()));
                }
            }

            @Override
            public void onFailure(Call<List<GoalEntity>> call, Throwable t) {
                mainHandler.post(() -> callback.onError(t.getMessage() == null ? "Network error" : t.getMessage()));
            }
        });
    }

    private void pushGoalToRemote(@NonNull GoalEntity entity, @NonNull SaveGoalCallback callback) {
        apiService.createGoal(entity).enqueue(new Callback<GoalEntity>() {
            @Override
            public void onResponse(Call<GoalEntity> call, Response<GoalEntity> response) {
                if (response.isSuccessful() && response.body() != null) {
                    IO.execute(() -> {
                        // ✅ CRITICAL: Save the real backend ID so edit/delete/savings work correctly
                        entity.remoteId = String.valueOf(response.body().localId);
                        entity.syncState = SyncState.SYNCED;
                        entity.updatedAt = System.currentTimeMillis();
                        goalDao.update(entity);
                        // Reschedule reminder with correct remoteId
                        FinancialReminderScheduler.scheduleGoalReminder(appContext, entity);
                    });
                } else {
                    handleSyncFailure(entity, callback, "HTTP " + response.code());
                }
            }

            @Override
            public void onFailure(Call<GoalEntity> call, Throwable t) {
                handleSyncFailure(entity, callback, t.getMessage());
            }
        });
    }

    private void updateGoalInRemote(@NonNull GoalEntity entity, @NonNull UpdateGoalCallback callback) {
        int backendId = 0;
        try {
            backendId = Integer.parseInt(entity.remoteId);
        } catch (NumberFormatException ignored) {}

        apiService.updateGoal(backendId, entity).enqueue(new Callback<GoalEntity>() {
            @Override
            public void onResponse(Call<GoalEntity> call, Response<GoalEntity> response) {
                if (response.isSuccessful()) {
                    IO.execute(() -> {
                        entity.syncState = SyncState.SYNCED;
                        entity.updatedAt = System.currentTimeMillis();
                        goalDao.update(entity);
                    });
                } else {
                    handleSyncFailure(entity, callback, "HTTP " + response.code());
                }
            }

            @Override
            public void onFailure(Call<GoalEntity> call, Throwable t) {
                handleSyncFailure(entity, callback, t.getMessage());
            }
        });
    }

    private void deleteGoalInRemote(@NonNull GoalEntity entity, @NonNull DeleteGoalCallback callback) {
        int backendId = 0;
        try {
            backendId = Integer.parseInt(entity.remoteId);
        } catch (NumberFormatException ignored) {}

        apiService.deleteGoal(backendId).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    IO.execute(() -> {
                        entity.syncState = SyncState.SYNCED;
                        entity.updatedAt = System.currentTimeMillis();
                        goalDao.update(entity);
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

    private void handleSyncFailure(GoalEntity entity, Object callback, String errorMsg) {
        IO.execute(() -> {
            entity.syncState = SyncState.FAILED;
            entity.updatedAt = System.currentTimeMillis();
            goalDao.update(entity);
            
            String msg = errorMsg == null ? "Cloud sync failed" : errorMsg;
            if (callback instanceof SaveGoalCallback) {
                mainHandler.post(() -> ((SaveGoalCallback) callback).onError(msg));
            } else if (callback instanceof UpdateGoalCallback) {
                mainHandler.post(() -> ((UpdateGoalCallback) callback).onError(msg));
            } else if (callback instanceof DeleteGoalCallback) {
                mainHandler.post(() -> ((DeleteGoalCallback) callback).onError(msg));
            }
        });
    }

    private List<Goal> toGoals(List<GoalEntity> entities) {
        List<Goal> goals = new ArrayList<>();
        for (GoalEntity entity : entities) {
            if (!entity.deleted) {
                goals.add(new Goal(
                        (int) entity.localId,
                        entity.name,
                        entity.description,
                        entity.targetAmount,
                        entity.currentAmount,
                        entity.addedSavingsAmount,
                        entity.targetDate,
                        entity.category,
                        entity.categoryIcon,
                        entity.progressCircleBackground
                ));
            }
        }
        return goals;
    }

    private GoalEntity fromGoal(String userId, Goal goal) {
        GoalEntity entity = new GoalEntity();
        entity.userId = userId;
        entity.name = goal.getName();
        entity.description = goal.getDescription();
        entity.targetAmount = goal.getTargetAmount();
        entity.currentAmount = goal.getCurrentAmount();
        entity.addedSavingsAmount = goal.getAddedSavingsAmount();
        entity.targetDate = goal.getTargetDate();
        entity.category = goal.getCategory();
        entity.categoryIcon = goal.getCategoryIcon();
        entity.progressCircleBackground = goal.getProgressCircleBackground();
        return entity;
    }
}
