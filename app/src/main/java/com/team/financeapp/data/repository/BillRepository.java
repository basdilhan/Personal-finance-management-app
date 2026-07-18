package com.team.financeapp.data.repository;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

import com.team.financeapp.Bill;
import com.team.financeapp.data.local.AppDatabase;
import com.team.financeapp.data.local.SyncState;
import com.team.financeapp.data.local.dao.BillDao;
import com.team.financeapp.data.local.entity.BillEntity;
import com.team.financeapp.data.remote.ApiClient;
import com.team.financeapp.data.remote.BillApiService;
import com.team.financeapp.notifications.FinancialReminderScheduler;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class BillRepository {

    public interface LoadBillsCallback {
        void onBillsLoaded(List<Bill> bills);
        void onError(String message);
    }

    public interface SaveBillCallback {
        void onSuccess();
        void onError(String message);
    }

    public interface ModifyBillCallback {
        void onSuccess();
        void onError(String message);
    }

    private static final ExecutorService IO = Executors.newSingleThreadExecutor();

    private final Context appContext;
    private final BillDao billDao;
    private final BillApiService apiService;
    private final Handler mainHandler;

    public BillRepository(@NonNull Context context) {
        this.appContext = context.getApplicationContext();
        this.billDao = AppDatabase.getInstance(appContext).billDao();
        this.apiService = ApiClient.getClient().create(BillApiService.class);
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    public void loadBills(@NonNull String userId, @NonNull LoadBillsCallback callback) {
        IO.execute(() -> {
            List<BillEntity> localEntities = billDao.getByUser(userId);
            List<Bill> localBills = toBills(localEntities);
            mainHandler.post(() -> callback.onBillsLoaded(localBills));
        });

        refreshFromRemote(userId, callback);
    }

    public void saveBill(@NonNull String userId, @NonNull Bill bill, @NonNull SaveBillCallback callback) {
        BillEntity entity = fromBill(userId, bill);
        entity.remoteId = UUID.randomUUID().toString(); // Temporary UUID
        entity.syncState = SyncState.PENDING;
        entity.createdAt = System.currentTimeMillis();
        entity.updatedAt = entity.createdAt;

        IO.execute(() -> {
            long localId = billDao.insert(entity);
            entity.localId = localId;
            FinancialReminderScheduler.scheduleBillReminder(appContext, entity);
            FinancialReminderScheduler.scheduleBillAddedReminder(appContext, entity);
            mainHandler.post(callback::onSuccess);
            pushBillToRemote(entity, callback);
        });
    }

    public void updateBill(@NonNull String userId, @NonNull Bill bill, @NonNull ModifyBillCallback callback) {
        IO.execute(() -> {
            BillEntity existing = billDao.getByLocalId(bill.getId());
            if (existing == null || !userId.equals(existing.userId)) {
                mainHandler.post(() -> callback.onError("Bill not found"));
                return;
            }

            existing.name = bill.getName();
            existing.description = bill.getDescription();
            existing.amount = bill.getAmount();
            existing.dueDate = bill.getDueDate();
            existing.category = bill.getCategory();
            existing.categoryIcon = bill.getCategoryIcon();
            existing.status = bill.getStatus();
            existing.indicatorColor = bill.getIndicatorColor();
            existing.deleted = false;
            existing.syncState = SyncState.PENDING;
            existing.updatedAt = System.currentTimeMillis();

            billDao.update(existing);
            FinancialReminderScheduler.scheduleBillReminder(appContext, existing);
            mainHandler.post(callback::onSuccess);
            updateBillInRemote(existing, callback);
        });
    }

    public void deleteBill(@NonNull String userId, int billId, @NonNull ModifyBillCallback callback) {
        IO.execute(() -> {
            BillEntity existing = billDao.getByLocalId(billId);
            if (existing == null || !userId.equals(existing.userId)) {
                mainHandler.post(() -> callback.onError("Bill not found"));
                return;
            }

            existing.deleted = true;
            existing.syncState = SyncState.PENDING;
            existing.updatedAt = System.currentTimeMillis();
            
            billDao.update(existing);
            FinancialReminderScheduler.cancelBillReminder(appContext, existing.remoteId);
            mainHandler.post(callback::onSuccess);
            deleteBillInRemote(existing, callback);
        });
    }

    private void refreshFromRemote(@NonNull String userId, @NonNull LoadBillsCallback callback) {
        apiService.getBills().enqueue(new Callback<List<BillEntity>>() {
            @Override
            public void onResponse(Call<List<BillEntity>> call, Response<List<BillEntity>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<BillEntity> remoteEntities = response.body();
                    IO.execute(() -> {
                        for (BillEntity bill : billDao.getAllByUser(userId)) {
                            FinancialReminderScheduler.cancelBillDueReminders(appContext, bill.remoteId);
                        }

                        for (BillEntity entity : remoteEntities) {
                            entity.syncState = SyncState.SYNCED;
                            entity.remoteId = String.valueOf(entity.localId);
                        }

                        billDao.deleteAllForUser(userId);
                        billDao.insertAll(remoteEntities);

                        for (BillEntity bill : billDao.getByUser(userId)) {
                            FinancialReminderScheduler.scheduleBillReminder(appContext, bill);
                        }

                        List<Bill> latest = toBills(billDao.getByUser(userId));
                        mainHandler.post(() -> callback.onBillsLoaded(latest));
                    });
                } else {
                    mainHandler.post(() -> callback.onError("Failed to refresh bills: HTTP " + response.code()));
                }
            }

            @Override
            public void onFailure(Call<List<BillEntity>> call, Throwable t) {
                mainHandler.post(() -> callback.onError(t.getMessage() == null ? "Network error" : t.getMessage()));
            }
        });
    }

    private void pushBillToRemote(@NonNull BillEntity entity, @NonNull SaveBillCallback callback) {
        apiService.createBill(entity).enqueue(new Callback<BillEntity>() {
            @Override
            public void onResponse(Call<BillEntity> call, Response<BillEntity> response) {
                if (response.isSuccessful() && response.body() != null) {
                    IO.execute(() -> {
                        // ✅ CRITICAL: Save the real backend ID so edit/delete work correctly
                        entity.remoteId = String.valueOf(response.body().localId);
                        entity.syncState = SyncState.SYNCED;
                        entity.updatedAt = System.currentTimeMillis();
                        billDao.update(entity);
                        // Reschedule reminder with correct remoteId
                        FinancialReminderScheduler.scheduleBillReminder(appContext, entity);
                    });
                } else {
                    handleSyncFailure(entity, callback, "HTTP " + response.code());
                }
            }

            @Override
            public void onFailure(Call<BillEntity> call, Throwable t) {
                handleSyncFailure(entity, callback, t.getMessage());
            }
        });
    }

    private void updateBillInRemote(@NonNull BillEntity entity, @NonNull ModifyBillCallback callback) {
        int backendId = 0;
        try {
            backendId = Integer.parseInt(entity.remoteId);
        } catch (NumberFormatException ignored) {}

        apiService.updateBill(backendId, entity).enqueue(new Callback<BillEntity>() {
            @Override
            public void onResponse(Call<BillEntity> call, Response<BillEntity> response) {
                if (response.isSuccessful()) {
                    IO.execute(() -> {
                        entity.syncState = SyncState.SYNCED;
                        entity.updatedAt = System.currentTimeMillis();
                        billDao.update(entity);
                    });
                } else {
                    handleSyncFailure(entity, callback, "HTTP " + response.code());
                }
            }

            @Override
            public void onFailure(Call<BillEntity> call, Throwable t) {
                handleSyncFailure(entity, callback, t.getMessage());
            }
        });
    }

    private void deleteBillInRemote(@NonNull BillEntity entity, @NonNull ModifyBillCallback callback) {
        int backendId = 0;
        try {
            backendId = Integer.parseInt(entity.remoteId);
        } catch (NumberFormatException ignored) {}

        apiService.deleteBill(backendId).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    IO.execute(() -> {
                        entity.syncState = SyncState.SYNCED;
                        entity.updatedAt = System.currentTimeMillis();
                        billDao.update(entity);
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

    private void handleSyncFailure(BillEntity entity, Object callback, String errorMsg) {
        IO.execute(() -> {
            entity.syncState = SyncState.FAILED;
            entity.updatedAt = System.currentTimeMillis();
            billDao.update(entity);
            
            String msg = errorMsg == null ? "Cloud sync failed" : errorMsg;
            if (callback instanceof SaveBillCallback) {
                mainHandler.post(() -> ((SaveBillCallback) callback).onError(msg));
            } else if (callback instanceof ModifyBillCallback) {
                mainHandler.post(() -> ((ModifyBillCallback) callback).onError(msg));
            }
        });
    }

    private BillEntity fromBill(String userId, Bill bill) {
        BillEntity entity = new BillEntity();
        entity.userId = userId;
        entity.name = bill.getName();
        entity.description = bill.getDescription();
        entity.amount = bill.getAmount();
        entity.dueDate = bill.getDueDate();
        entity.category = bill.getCategory();
        entity.categoryIcon = bill.getCategoryIcon();
        entity.status = bill.getStatus();
        entity.indicatorColor = bill.getIndicatorColor();
        entity.deleted = false;
        return entity;
    }

    private List<Bill> toBills(List<BillEntity> entities) {
        List<Bill> bills = new ArrayList<>();
        for (BillEntity entity : entities) {
            if (!entity.deleted) {
                bills.add(new Bill(
                        (int) entity.localId,
                        entity.name,
                        entity.description,
                        entity.amount,
                        entity.dueDate,
                        entity.category,
                        entity.categoryIcon,
                        entity.status,
                        entity.indicatorColor
                ));
            }
        }
        return bills;
    }
}
