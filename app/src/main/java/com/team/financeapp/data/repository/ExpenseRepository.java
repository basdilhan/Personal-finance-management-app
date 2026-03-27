package com.team.financeapp.data.repository;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.team.financeapp.Expense;
import com.team.financeapp.R;
import com.team.financeapp.data.local.AppDatabase;
import com.team.financeapp.data.local.SyncState;
import com.team.financeapp.data.local.dao.ExpenseDao;
import com.team.financeapp.data.local.entity.ExpenseEntity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ExpenseRepository {

    public interface LoadExpensesCallback {
        void onExpensesLoaded(List<Expense> expenses);

        void onError(String message);
    }

    public interface SaveExpenseCallback {
        void onSuccess();

        void onError(String message);
    }

    private static final ExecutorService IO = Executors.newSingleThreadExecutor();

    private final ExpenseDao expenseDao;
    private final FirebaseFirestore firestore;
    private final Handler mainHandler;

    public ExpenseRepository(@NonNull Context context) {
        this.expenseDao = AppDatabase.getInstance(context).expenseDao();
        this.firestore = FirebaseFirestore.getInstance();
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    public void loadExpenses(@NonNull String userId, @NonNull LoadExpensesCallback callback) {
        IO.execute(() -> {
            List<ExpenseEntity> localEntities = expenseDao.getByUser(userId);
            List<Expense> localExpenses = toExpenses(localEntities);
            mainHandler.post(() -> callback.onExpensesLoaded(localExpenses));
        });

        refreshFromRemote(userId, callback);
    }

    public void saveExpense(@NonNull String userId, @NonNull Expense expense, @NonNull SaveExpenseCallback callback) {
        ExpenseEntity entity = fromExpense(userId, expense);
        entity.remoteId = UUID.randomUUID().toString();
        entity.syncState = SyncState.PENDING;
        entity.createdAt = System.currentTimeMillis();
        entity.updatedAt = entity.createdAt;

        IO.execute(() -> {
            long localId = expenseDao.insert(entity);
            entity.localId = localId;
            mainHandler.post(callback::onSuccess);
            pushExpenseToRemote(entity, callback);
        });
    }

    private void refreshFromRemote(@NonNull String userId, @NonNull LoadExpensesCallback callback) {
        firestore.collection("expenses")
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(querySnapshot -> IO.execute(() -> {
                    List<ExpenseEntity> remoteEntities = new ArrayList<>();
                    for (DocumentSnapshot document : querySnapshot.getDocuments()) {
                        remoteEntities.add(fromDocument(document));
                    }

                    expenseDao.deleteAllForUser(userId);
                    expenseDao.insertAll(remoteEntities);
                    List<Expense> latest = toExpenses(expenseDao.getByUser(userId));
                    mainHandler.post(() -> callback.onExpensesLoaded(latest));
                }))
                .addOnFailureListener(e -> mainHandler.post(() -> callback.onError(
                        e.getMessage() == null ? "Failed to refresh expenses" : e.getMessage()
                )));
    }

    private void pushExpenseToRemote(@NonNull ExpenseEntity entity, @NonNull SaveExpenseCallback callback) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("userId", entity.userId);
        payload.put("category", entity.category);
        payload.put("amount", entity.amount);
        payload.put("description", entity.description);
        payload.put("date", entity.date);
        payload.put("time", entity.time);
        payload.put("categoryIcon", entity.categoryIcon);
        payload.put("deleted", entity.deleted);
        payload.put("syncState", SyncState.SYNCED);
        payload.put("createdAt", entity.createdAt);
        payload.put("updatedAt", System.currentTimeMillis());

        firestore.collection("expenses")
                .document(entity.remoteId)
                .set(payload)
                .addOnSuccessListener(unused -> IO.execute(() -> {
                    entity.syncState = SyncState.SYNCED;
                    entity.updatedAt = System.currentTimeMillis();
                    expenseDao.update(entity);
                }))
                .addOnFailureListener(e -> IO.execute(() -> {
                    entity.syncState = SyncState.FAILED;
                    entity.updatedAt = System.currentTimeMillis();
                    expenseDao.update(entity);
                    mainHandler.post(() -> callback.onError(
                            e.getMessage() == null ? "Saved locally but cloud sync failed" : e.getMessage()
                    ));
                }));
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
        return expenses;
    }

    private ExpenseEntity fromDocument(DocumentSnapshot document) {
        ExpenseEntity entity = new ExpenseEntity();
        entity.remoteId = document.getId();
        entity.userId = getString(document, "userId", "");
        entity.category = getString(document, "category", "Other");
        entity.amount = getDouble(document, "amount", 0.0d);
        entity.description = getString(document, "description", "");
        entity.date = getLong(document, "date", 0L);
        entity.time = getString(document, "time", "00:00");
        entity.categoryIcon = (int) getLong(document, "categoryIcon", R.drawable.ic_receipt);
        entity.deleted = document.getBoolean("deleted") != null && Boolean.TRUE.equals(document.getBoolean("deleted"));
        entity.syncState = SyncState.SYNCED;
        entity.createdAt = getLong(document, "createdAt", System.currentTimeMillis());
        entity.updatedAt = getLong(document, "updatedAt", System.currentTimeMillis());
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

    private static double getDouble(DocumentSnapshot doc, String key, double fallback) {
        Double value = doc.getDouble(key);
        return value == null ? fallback : value;
    }
}