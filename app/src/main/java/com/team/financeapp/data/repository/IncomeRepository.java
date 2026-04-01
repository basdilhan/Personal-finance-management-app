package com.team.financeapp.data.repository;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.team.financeapp.IncomeEntry;
import com.team.financeapp.R;
import com.team.financeapp.data.local.AppDatabase;
import com.team.financeapp.data.local.SyncState;
import com.team.financeapp.data.local.dao.IncomeDao;
import com.team.financeapp.data.local.entity.IncomeEntity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class IncomeRepository {

    public interface LoadIncomeCallback {
        void onIncomeLoaded(List<IncomeEntry> incomes);

        void onError(String message);
    }

    public interface SaveIncomeCallback {
        void onSuccess();

        void onError(String message);
    }

    private static final ExecutorService IO = Executors.newSingleThreadExecutor();

    private final IncomeDao incomeDao;
    private final FirebaseFirestore firestore;
    private final Handler mainHandler;

    public IncomeRepository(@NonNull Context context) {
        this.incomeDao = AppDatabase.getInstance(context).incomeDao();
        this.firestore = FirebaseFirestore.getInstance();
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
        entity.remoteId = UUID.randomUUID().toString();
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

    private void refreshFromRemote(@NonNull String userId, @NonNull LoadIncomeCallback callback) {
        firestore.collection("incomes")
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(querySnapshot -> IO.execute(() -> {
                    List<IncomeEntity> remote = new ArrayList<>();
                    for (DocumentSnapshot document : querySnapshot.getDocuments()) {
                        remote.add(fromDocument(document));
                    }
                    incomeDao.deleteAllForUser(userId);
                    incomeDao.insertAll(remote);
                    List<IncomeEntry> latest = toIncomeEntries(incomeDao.getByUser(userId));
                    mainHandler.post(() -> callback.onIncomeLoaded(latest));
                }))
                .addOnFailureListener(e -> mainHandler.post(() -> callback.onError(
                        e.getMessage() == null ? "Failed to refresh incomes" : e.getMessage()
                )));
    }

    private void pushIncomeToRemote(@NonNull IncomeEntity entity, @NonNull SaveIncomeCallback callback) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("userId", entity.userId);
        payload.put("source", entity.source);
        payload.put("amount", entity.amount);
        payload.put("note", entity.note);
        payload.put("date", entity.date);
        payload.put("time", entity.time);
        payload.put("sourceIcon", entity.sourceIcon);
        payload.put("deleted", entity.deleted);
        payload.put("syncState", SyncState.SYNCED);
        payload.put("createdAt", entity.createdAt);
        payload.put("updatedAt", System.currentTimeMillis());

        firestore.collection("incomes")
                .document(entity.remoteId)
                .set(payload)
                .addOnSuccessListener(unused -> IO.execute(() -> {
                    entity.syncState = SyncState.SYNCED;
                    entity.updatedAt = System.currentTimeMillis();
                    incomeDao.update(entity);
                }))
                .addOnFailureListener(e -> IO.execute(() -> {
                    entity.syncState = SyncState.FAILED;
                    entity.updatedAt = System.currentTimeMillis();
                    incomeDao.update(entity);
                    mainHandler.post(() -> callback.onError(
                            e.getMessage() == null ? "Saved locally but cloud sync failed" : e.getMessage()
                    ));
                }));
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
            entries.add(new IncomeEntry(
                    entity.source,
                    entity.amount,
                    entity.note,
                    entity.date,
                    entity.time,
                    entity.sourceIcon
            ));
        }
        return entries;
    }

    private IncomeEntity fromDocument(DocumentSnapshot document) {
        IncomeEntity entity = new IncomeEntity();
        entity.remoteId = document.getId();
        entity.userId = getString(document, "userId", "");
        entity.source = getString(document, "source", "Other");
        entity.amount = getDouble(document, "amount", 0.0d);
        entity.note = getString(document, "note", "");
        entity.date = getEpochMillis(document, "date", 0L);
        entity.time = getString(document, "time", "00:00");
        entity.sourceIcon = (int) getLong(document, "sourceIcon", R.drawable.ic_wallet);
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