package com.team.financeapp.data.repository;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

import com.team.financeapp.IncomeEntry;
import com.team.financeapp.data.local.AppDatabase;
import com.team.financeapp.data.local.SyncState;
import com.team.financeapp.data.local.dao.IncomeDao;
import com.team.financeapp.data.local.entity.IncomeEntity;
import com.team.financeapp.data.remote.ApiClient;
import com.team.financeapp.data.remote.IncomeApiService;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class IncomeRepository {

    public interface LoadIncomeCallback {
        void onIncomeLoaded(List<IncomeEntry> incomes);
        void onError(String message);
    }

    public interface SaveIncomeCallback {
        void onSuccess();
        void onError(String message);
    }

    public interface ModifyIncomeCallback {
        void onSuccess();
        void onError(String message);
    }

    private static final ExecutorService IO = Executors.newSingleThreadExecutor();

    private final IncomeDao incomeDao;
    private final IncomeApiService apiService;
    private final Handler mainHandler;

    public IncomeRepository(@NonNull Context context) {
        this.incomeDao = AppDatabase.getInstance(context).incomeDao();
        this.apiService = ApiClient.getClient().create(IncomeApiService.class);
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    public void loadIncome(@NonNull String userId, @NonNull LoadIncomeCallback callback) {
        IO.execute(() -> {
            List<IncomeEntity> local = incomeDao.getByUser(userId);
            mainHandler.post(() -> callback.onIncomeLoaded(toIncomeEntries(local)));
        });

        refreshFromRemote(userId, callback);
    }

    public void saveIncome(@NonNull String userId, @NonNull IncomeEntry income, @NonNull SaveIncomeCallback callback) {
        IncomeEntity entity = fromIncome(userId, income);
        entity.remoteId = UUID.randomUUID().toString(); // Temporary UUID
        entity.syncState = SyncState.PENDING;
        entity.createdAt = System.currentTimeMillis();
        entity.updatedAt = entity.createdAt;

        IO.execute(() -> {
            long localId = incomeDao.insert(entity);
            entity.localId = localId;
            mainHandler.post(callback::onSuccess);
            pushIncomeToRemote(entity, callback);
        });
    }

    public void updateIncome(@NonNull String userId, @NonNull IncomeEntry income, @NonNull ModifyIncomeCallback callback) {
        IO.execute(() -> {
            IncomeEntity existing = incomeDao.getByLocalId(income.getId());
            if (existing == null || !userId.equals(existing.userId)) {
                mainHandler.post(() -> callback.onError("Income entry not found"));
                return;
            }

            existing.source = income.getSource();
            existing.amount = income.getAmount();
            existing.note = income.getNote();
            existing.date = income.getDate();
            existing.time = income.getTime();
            existing.sourceIcon = income.getSourceIcon();
            existing.deleted = false;
            existing.syncState = SyncState.PENDING;
            existing.updatedAt = System.currentTimeMillis();

            incomeDao.update(existing);
            mainHandler.post(callback::onSuccess);
            updateIncomeInRemote(existing, callback);
        });
    }

    public void deleteIncome(@NonNull String userId, int incomeId, @NonNull ModifyIncomeCallback callback) {
        IO.execute(() -> {
            IncomeEntity existing = incomeDao.getByLocalId(incomeId);
            if (existing == null || !userId.equals(existing.userId)) {
                mainHandler.post(() -> callback.onError("Income entry not found"));
                return;
            }

            existing.deleted = true;
            existing.syncState = SyncState.PENDING;
            existing.updatedAt = System.currentTimeMillis();
            
            incomeDao.update(existing);
            mainHandler.post(callback::onSuccess);
            deleteIncomeInRemote(existing, callback);
        });
    }

    private void refreshFromRemote(@NonNull String userId, @NonNull LoadIncomeCallback callback) {
        apiService.getIncomes().enqueue(new Callback<List<IncomeEntity>>() {
            @Override
            public void onResponse(Call<List<IncomeEntity>> call, Response<List<IncomeEntity>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<IncomeEntity> remote = response.body();
                    IO.execute(() -> {
                        for (IncomeEntity entity : remote) {
                            entity.syncState = SyncState.SYNCED;
                            entity.remoteId = String.valueOf(entity.localId);
                        }
                        incomeDao.deleteAllForUser(userId);
                        incomeDao.insertAll(remote);
                        List<IncomeEntry> latest = toIncomeEntries(incomeDao.getByUser(userId));
                        mainHandler.post(() -> callback.onIncomeLoaded(latest));
                    });
                } else {
                    mainHandler.post(() -> callback.onError("Failed to refresh incomes: HTTP " + response.code()));
                }
            }

            @Override
            public void onFailure(Call<List<IncomeEntity>> call, Throwable t) {
                mainHandler.post(() -> callback.onError(t.getMessage() == null ? "Network error" : t.getMessage()));
            }
        });
    }

    private void pushIncomeToRemote(@NonNull IncomeEntity entity, @NonNull SaveIncomeCallback callback) {
        apiService.createIncome(entity).enqueue(new Callback<IncomeEntity>() {
            @Override
            public void onResponse(Call<IncomeEntity> call, Response<IncomeEntity> response) {
                if (response.isSuccessful() && response.body() != null) {
                    IO.execute(() -> {
                        // ✅ CRITICAL: Save the real backend ID so edit/delete work correctly
                        entity.remoteId = String.valueOf(response.body().localId);
                        entity.syncState = SyncState.SYNCED;
                        entity.updatedAt = System.currentTimeMillis();
                        incomeDao.update(entity);
                    });
                } else {
                    handleSyncFailure(entity, callback, "HTTP " + response.code());
                }
            }

            @Override
            public void onFailure(Call<IncomeEntity> call, Throwable t) {
                handleSyncFailure(entity, callback, t.getMessage());
            }
        });
    }

    private void updateIncomeInRemote(@NonNull IncomeEntity entity, @NonNull ModifyIncomeCallback callback) {
        int backendId = 0;
        try {
            backendId = Integer.parseInt(entity.remoteId);
        } catch (NumberFormatException ignored) {}

        apiService.updateIncome(backendId, entity).enqueue(new Callback<IncomeEntity>() {
            @Override
            public void onResponse(Call<IncomeEntity> call, Response<IncomeEntity> response) {
                if (response.isSuccessful()) {
                    IO.execute(() -> {
                        entity.syncState = SyncState.SYNCED;
                        entity.updatedAt = System.currentTimeMillis();
                        incomeDao.update(entity);
                    });
                } else {
                    handleSyncFailure(entity, callback, "HTTP " + response.code());
                }
            }

            @Override
            public void onFailure(Call<IncomeEntity> call, Throwable t) {
                handleSyncFailure(entity, callback, t.getMessage());
            }
        });
    }

    private void deleteIncomeInRemote(@NonNull IncomeEntity entity, @NonNull ModifyIncomeCallback callback) {
        int backendId = 0;
        try {
            backendId = Integer.parseInt(entity.remoteId);
        } catch (NumberFormatException ignored) {}

        apiService.deleteIncome(backendId).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    IO.execute(() -> {
                        entity.syncState = SyncState.SYNCED;
                        entity.updatedAt = System.currentTimeMillis();
                        incomeDao.update(entity);
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

    private void handleSyncFailure(IncomeEntity entity, Object callback, String errorMsg) {
        IO.execute(() -> {
            entity.syncState = SyncState.FAILED;
            entity.updatedAt = System.currentTimeMillis();
            incomeDao.update(entity);
            
            String msg = errorMsg == null ? "Cloud sync failed" : errorMsg;
            if (callback instanceof SaveIncomeCallback) {
                mainHandler.post(() -> ((SaveIncomeCallback) callback).onError(msg));
            } else if (callback instanceof ModifyIncomeCallback) {
                mainHandler.post(() -> ((ModifyIncomeCallback) callback).onError(msg));
            }
        });
    }

    private IncomeEntity fromIncome(String userId, IncomeEntry income) {
        IncomeEntity entity = new IncomeEntity();
        entity.userId = userId;
        entity.source = income.getSource();
        entity.amount = income.getAmount();
        entity.note = income.getNote();
        entity.date = income.getDate();
        entity.time = income.getTime();
        entity.sourceIcon = income.getSourceIcon();
        entity.deleted = false;
        return entity;
    }

    private List<IncomeEntry> toIncomeEntries(List<IncomeEntity> entities) {
        List<IncomeEntry> entries = new ArrayList<>();
        for (IncomeEntity entity : entities) {
            if (!entity.deleted) {
                entries.add(new IncomeEntry(
                        (int) entity.localId,
                        entity.source,
                        entity.amount,
                        entity.note,
                        entity.date,
                        entity.time,
                        entity.sourceIcon
                ));
            }
        }
        return entries;
    }
}