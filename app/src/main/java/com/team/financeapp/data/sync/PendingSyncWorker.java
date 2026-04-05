package com.team.financeapp.data.sync;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.BackoffPolicy;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.team.financeapp.data.local.AppDatabase;
import com.team.financeapp.data.local.SyncState;
import com.team.financeapp.data.local.dao.BillDao;
import com.team.financeapp.data.local.dao.ExpenseDao;
import com.team.financeapp.data.local.dao.IncomeDao;
import com.team.financeapp.data.local.entity.BillEntity;
import com.team.financeapp.data.local.entity.ExpenseEntity;
import com.team.financeapp.data.local.entity.IncomeEntity;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class PendingSyncWorker extends Worker {

    public static final String UNIQUE_WORK_NAME = "pending_finance_sync";

    private static final String TAG = "PendingSyncWorker";
    private static final long FIRESTORE_TIMEOUT_SECONDS = 20L;

    public PendingSyncWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        String userId = getCurrentUserId();
        if (userId == null || userId.trim().isEmpty()) {
            Log.d(TAG, "Skipping sync because there is no signed-in user");
            return Result.success();
        }

        AppDatabase db = AppDatabase.getInstance(getApplicationContext());
        BillDao billDao = db.billDao();
        ExpenseDao expenseDao = db.expenseDao();
        IncomeDao incomeDao = db.incomeDao();
        FirebaseFirestore firestore = FirebaseFirestore.getInstance();

        boolean hasFailures = false;
        int syncedCount = 0;

        List<BillEntity> pendingBills = billDao.getPendingSync(userId);
        for (BillEntity bill : pendingBills) {
            if (syncBill(firestore, bill)) {
                syncedCount++;
                continue;
            }
            hasFailures = true;
            bill.syncState = SyncState.FAILED;
            bill.updatedAt = System.currentTimeMillis();
            billDao.update(bill);
        }

        List<ExpenseEntity> pendingExpenses = expenseDao.getPendingSync(userId);
        for (ExpenseEntity expense : pendingExpenses) {
            if (syncExpense(firestore, expense)) {
                syncedCount++;
                continue;
            }
            hasFailures = true;
            expense.syncState = SyncState.FAILED;
            expense.updatedAt = System.currentTimeMillis();
            expenseDao.update(expense);
        }

        List<IncomeEntity> pendingIncomes = incomeDao.getPendingSync(userId);
        for (IncomeEntity income : pendingIncomes) {
            if (syncIncome(firestore, income)) {
                syncedCount++;
                continue;
            }
            hasFailures = true;
            income.syncState = SyncState.FAILED;
            income.updatedAt = System.currentTimeMillis();
            incomeDao.update(income);
        }

        Log.d(TAG, "Sync run finished. Synced records=" + syncedCount + ", hadFailures=" + hasFailures);
        return hasFailures ? Result.retry() : Result.success();
    }

    private static String getCurrentUserId() {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            return null;
        }
        return FirebaseAuth.getInstance().getCurrentUser().getUid();
    }

    private boolean syncBill(FirebaseFirestore firestore, BillEntity bill) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("userId", bill.userId);
        payload.put("name", bill.name);
        payload.put("description", bill.description);
        payload.put("amount", bill.amount);
        payload.put("dueDate", bill.dueDate);
        payload.put("category", bill.category);
        payload.put("categoryIcon", bill.categoryIcon);
        payload.put("status", bill.status);
        payload.put("indicatorColor", bill.indicatorColor);
        payload.put("deleted", bill.deleted);
        payload.put("isRecurring", bill.isRecurring);
        payload.put("syncState", SyncState.SYNCED);
        payload.put("createdAt", bill.createdAt);
        payload.put("updatedAt", System.currentTimeMillis());

        try {
            Tasks.await(
                    firestore.collection("bills").document(bill.remoteId).set(payload),
                    FIRESTORE_TIMEOUT_SECONDS,
                    TimeUnit.SECONDS
            );
            bill.syncState = SyncState.SYNCED;
            bill.updatedAt = System.currentTimeMillis();
            AppDatabase.getInstance(getApplicationContext()).billDao().update(bill);
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Bill sync failed for remoteId=" + bill.remoteId, e);
            return false;
        }
    }

    private boolean syncExpense(FirebaseFirestore firestore, ExpenseEntity expense) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("userId", expense.userId);
        payload.put("category", expense.category);
        payload.put("amount", expense.amount);
        payload.put("description", expense.description);
        payload.put("date", expense.date);
        payload.put("time", expense.time);
        payload.put("categoryIcon", expense.categoryIcon);
        payload.put("deleted", expense.deleted);
        payload.put("syncState", SyncState.SYNCED);
        payload.put("createdAt", expense.createdAt);
        payload.put("updatedAt", System.currentTimeMillis());

        try {
            Tasks.await(
                    firestore.collection("expenses").document(expense.remoteId).set(payload),
                    FIRESTORE_TIMEOUT_SECONDS,
                    TimeUnit.SECONDS
            );
            expense.syncState = SyncState.SYNCED;
            expense.updatedAt = System.currentTimeMillis();
            AppDatabase.getInstance(getApplicationContext()).expenseDao().update(expense);
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Expense sync failed for remoteId=" + expense.remoteId, e);
            return false;
        }
    }

    private boolean syncIncome(FirebaseFirestore firestore, IncomeEntity income) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("userId", income.userId);
        payload.put("source", income.source);
        payload.put("amount", income.amount);
        payload.put("note", income.note);
        payload.put("date", income.date);
        payload.put("time", income.time);
        payload.put("sourceIcon", income.sourceIcon);
        payload.put("deleted", income.deleted);
        payload.put("syncState", SyncState.SYNCED);
        payload.put("createdAt", income.createdAt);
        payload.put("updatedAt", System.currentTimeMillis());

        try {
            Tasks.await(
                    firestore.collection("incomes").document(income.remoteId).set(payload),
                    FIRESTORE_TIMEOUT_SECONDS,
                    TimeUnit.SECONDS
            );
            income.syncState = SyncState.SYNCED;
            income.updatedAt = System.currentTimeMillis();
            AppDatabase.getInstance(getApplicationContext()).incomeDao().update(income);
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Income sync failed for remoteId=" + income.remoteId, e);
            return false;
        }
    }

    public static void schedule(Context context) {
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        PeriodicWorkRequest request = new PeriodicWorkRequest.Builder(
                PendingSyncWorker.class,
                15,
                TimeUnit.MINUTES
        )
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .build();

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                UNIQUE_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
        );
    }
}
