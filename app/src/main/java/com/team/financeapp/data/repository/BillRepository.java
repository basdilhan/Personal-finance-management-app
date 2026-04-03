package com.team.financeapp.data.repository;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.team.financeapp.Bill;
import com.team.financeapp.R;
import com.team.financeapp.data.local.AppDatabase;
import com.team.financeapp.data.local.SyncState;
import com.team.financeapp.data.local.dao.BillDao;
import com.team.financeapp.data.local.entity.BillEntity;
import com.team.financeapp.notifications.FinancialReminderScheduler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
    private final FirebaseFirestore firestore;
    private final Handler mainHandler;

    public BillRepository(@NonNull Context context) {
        this.appContext = context.getApplicationContext();
        this.billDao = AppDatabase.getInstance(appContext).billDao();
        this.firestore = FirebaseFirestore.getInstance();
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
        entity.remoteId = UUID.randomUUID().toString();
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
            pushBillToRemote(existing, callback);
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
            pushBillToRemote(existing, callback);
        });
    }

    private void refreshFromRemote(@NonNull String userId, @NonNull LoadBillsCallback callback) {
        firestore.collection("bills")
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(querySnapshot -> IO.execute(() -> {
                    List<BillEntity> remoteEntities = new ArrayList<>();
                    for (DocumentSnapshot document : querySnapshot.getDocuments()) {
                        remoteEntities.add(fromDocument(document));
                    }

                    for (BillEntity bill : billDao.getAllByUser(userId)) {
                        FinancialReminderScheduler.cancelBillDueReminders(appContext, bill.remoteId);
                    }

                    billDao.deleteAllForUser(userId);
                    billDao.insertAll(remoteEntities);

                    for (BillEntity bill : billDao.getByUser(userId)) {
                        FinancialReminderScheduler.scheduleBillReminder(appContext, bill);
                    }

                    List<Bill> latest = toBills(billDao.getByUser(userId));
                    mainHandler.post(() -> callback.onBillsLoaded(latest));
                }))
                .addOnFailureListener(e -> mainHandler.post(() -> callback.onError(
                        e.getMessage() == null ? "Failed to refresh bills" : e.getMessage()
                )));
    }

    private void pushBillToRemote(@NonNull BillEntity entity, @NonNull SaveBillCallback callback) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("userId", entity.userId);
        payload.put("name", entity.name);
        payload.put("description", entity.description);
        payload.put("amount", entity.amount);
        payload.put("dueDate", entity.dueDate);
        payload.put("category", entity.category);
        payload.put("categoryIcon", entity.categoryIcon);
        payload.put("status", entity.status);
        payload.put("indicatorColor", entity.indicatorColor);
        payload.put("deleted", entity.deleted);
        payload.put("syncState", SyncState.SYNCED);
        payload.put("createdAt", entity.createdAt);
        payload.put("updatedAt", System.currentTimeMillis());

        firestore.collection("bills")
                .document(entity.remoteId)
                .set(payload)
                .addOnSuccessListener(unused -> IO.execute(() -> {
                    entity.syncState = SyncState.SYNCED;
                    entity.updatedAt = System.currentTimeMillis();
                    billDao.update(entity);
                }))
                .addOnFailureListener(e -> IO.execute(() -> {
                    entity.syncState = SyncState.FAILED;
                    entity.updatedAt = System.currentTimeMillis();
                    billDao.update(entity);
                    mainHandler.post(() -> callback.onError(
                            e.getMessage() == null ? "Saved locally but cloud sync failed" : e.getMessage()
                    ));
                }));
    }

    private void pushBillToRemote(@NonNull BillEntity entity, @NonNull ModifyBillCallback callback) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("userId", entity.userId);
        payload.put("name", entity.name);
        payload.put("description", entity.description);
        payload.put("amount", entity.amount);
        payload.put("dueDate", entity.dueDate);
        payload.put("category", entity.category);
        payload.put("categoryIcon", entity.categoryIcon);
        payload.put("status", entity.status);
        payload.put("indicatorColor", entity.indicatorColor);
        payload.put("deleted", entity.deleted);
        payload.put("syncState", SyncState.SYNCED);
        payload.put("createdAt", entity.createdAt);
        payload.put("updatedAt", System.currentTimeMillis());

        firestore.collection("bills")
                .document(entity.remoteId)
                .set(payload)
                .addOnSuccessListener(unused -> IO.execute(() -> {
                    entity.syncState = SyncState.SYNCED;
                    entity.updatedAt = System.currentTimeMillis();
                    billDao.update(entity);
                }))
                .addOnFailureListener(e -> IO.execute(() -> {
                    entity.syncState = SyncState.FAILED;
                    entity.updatedAt = System.currentTimeMillis();
                    billDao.update(entity);
                    mainHandler.post(() -> callback.onError(
                            e.getMessage() == null ? "Saved locally but cloud sync failed" : e.getMessage()
                    ));
                }));
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
        return bills;
    }

    private BillEntity fromDocument(DocumentSnapshot document) {
        BillEntity entity = new BillEntity();
        entity.remoteId = document.getId();
        entity.userId = getString(document, "userId", "");
        entity.name = getString(document, "name", "");
        entity.description = getString(document, "description", "");
        entity.amount = getDouble(document, "amount", 0.0d);
        entity.dueDate = getEpochMillis(document, "dueDate", 0L);
        entity.category = getString(document, "category", "Other");
        entity.categoryIcon = (int) getLong(document, "categoryIcon", R.drawable.ic_receipt);
        entity.status = getString(document, "status", "pending");
        entity.indicatorColor = (int) getLong(document, "indicatorColor", R.drawable.circle_blue_light);
        entity.deleted = document.getBoolean("deleted") != null && Boolean.TRUE.equals(document.getBoolean("deleted"));
        entity.syncState = SyncState.SYNCED;
        entity.createdAt = getEpochMillis(document, "createdAt", System.currentTimeMillis());
        entity.updatedAt = getEpochMillis(document, "updatedAt", System.currentTimeMillis());
        return entity;
    }

    private static String getString(DocumentSnapshot doc, String key, String fallback) {
        String value = doc.getString(key);
        return value == null ? fallback : value;
    }

    private static long getLong(DocumentSnapshot doc, String key, long fallback) {
        Long value = doc.getLong(key);
        return value == null ? fallback : value;
    }

    private static long getEpochMillis(DocumentSnapshot doc, String key, long fallback) {
        Object value = doc.get(key);
        if (value == null) {
            return fallback;
        }
        if (value instanceof Timestamp) {
            return ((Timestamp) value).toDate().getTime();
        }
        if (value instanceof Number) {
            return normalizeEpochMillis(((Number) value).longValue());
        }
        if (value instanceof String) {
            try {
                return normalizeEpochMillis(Long.parseLong((String) value));
            } catch (NumberFormatException ignored) {
                return fallback;
            }
        }
        return fallback;
    }

    private static long normalizeEpochMillis(long raw) {
        if (raw <= 0L) {
            return raw;
        }
        return raw < 1_000_000_000_000L ? raw * 1000L : raw;
    }

    private static double getDouble(DocumentSnapshot doc, String key, double fallback) {
        Object value = doc.get(key);
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        if (value instanceof String) {
            try {
                return Double.parseDouble((String) value);
            } catch (NumberFormatException ignored) {
                return fallback;
            }
        }
        return fallback;
    }
}
